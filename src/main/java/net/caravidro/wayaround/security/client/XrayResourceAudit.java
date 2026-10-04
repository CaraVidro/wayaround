package net.caravidro.wayaround.security.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.security.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Runs once per resource reload on its preparation executor; no disk or image work in ticks/packets. */
public final class XrayResourceAudit extends SimplePreparableReloadListener<ResourceEvidence> {
    // Intentionally excludes leaves, glass, water, flowers and all naturally translucent mod plants.
    static final String[] BLOCKS = {"stone","deepslate","dirt","grass_block","sand","gravel","andesite","diorite",
            "granite","tuff","netherrack","blackstone","basalt","end_stone","red_sand","clay",
            "diamond_ore","iron_ore","gold_ore","redstone_ore"};
    private static volatile ResourceEvidence latest;
    private int bytesLeft;
    private MessageDigest digest;
    private ResourceManager resources;
    private final Map<String, JsonObject> models = new HashMap<>();
    private final Map<String, Double> alphas = new HashMap<>();

    public static void register(RegisterClientReloadListenersEvent event) { event.registerReloadListener(new XrayResourceAudit()); }
    public static void challenge(long nonce) {
        var result = latest;
        if (result != null && net.minecraft.client.Minecraft.getInstance().getConnection() != null)
            PacketDistributor.sendToServer(new XrayReportPayload(nonce,result));
    }

    @Override protected ResourceEvidence prepare(ResourceManager manager, ProfilerFiller profiler) {
        resources = manager; models.clear(); alphas.clear(); bytesLeft = 32 * 1024 * 1024;
        int transparent = 0, hidden = 0, ore = 0, inspected = 0;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            for (int i=0;i<BLOCKS.length;i++) {
                Observation observation = inspect("minecraft:block/" + BLOCKS[i]);
                if (!observation.known) continue;
                inspected |= 1 << i;
                if (i < ResourceEvidence.HOSTS) {
                    if (observation.transparent) transparent |= 1 << i;
                    if (observation.hidden) hidden |= 1 << i;
                } else if (!observation.transparent && !observation.hidden) ore |= 1 << (i-ResourceEvidence.HOSTS);
            }
            return new ResourceEvidence(transparent,hidden,ore,inspected,HexFormat.of().formatHex(digest.digest()));
        } catch (Exception exception) {
            // Unsupported resources or failed decoding mean unknown, never guilt or a clean bill of health.
            return new ResourceEvidence(0,0,0,0,"0".repeat(64));
        } finally { resources = null; models.clear(); alphas.clear(); }
    }

    @Override protected void apply(ResourceEvidence value, ResourceManager manager, ProfilerFiller profiler) {
        latest = value;
        WayAround.LOGGER.info("[AntiXray] Resource audit completed: {}",value.summary());
        if (Boolean.getBoolean("wayaround.validateAntixray")) XrayResourceAuditValidation.run(manager,profiler);
    }

    private record Observation(boolean known, boolean transparent, boolean hidden) {
        static final Observation UNKNOWN = new Observation(false,false,false);
    }

    private Observation inspect(String name) {
        try {
            Map<String,String> textures = new HashMap<>();
            JsonArray elements = null;
            Set<String> visited = new HashSet<>();
            String current = name;
            for (int depth=0;depth<8;depth++) {
                if (!visited.add(current)) return Observation.UNKNOWN;
                JsonObject model = model(current);
                if (model == null || model.has("loader")) return Observation.UNKNOWN;
                if (model.has("textures")) for (var entry:model.getAsJsonObject("textures").entrySet())
                    textures.putIfAbsent(entry.getKey(),entry.getValue().getAsString());
                if (elements == null && model.has("elements")) elements = model.getAsJsonArray("elements");
                if (!model.has("parent")) break;
                current = model.get("parent").getAsString();
                if (!current.contains(":")) current = "minecraft:"+current;
                if (current.startsWith("minecraft:builtin/")) return Observation.UNKNOWN;
            }
            if (elements == null || elements.size()>64) return Observation.UNKNOWN;
            if (elements.isEmpty()) return new Observation(true,false,true);
            double volume=0, area=0, transparentArea=0;
            for (JsonElement element:elements) {
                JsonObject object = element.getAsJsonObject();
                JsonArray from=object.getAsJsonArray("from"), to=object.getAsJsonArray("to");
                double x=Math.abs(to.get(0).getAsDouble()-from.get(0).getAsDouble());
                double y=Math.abs(to.get(1).getAsDouble()-from.get(1).getAsDouble());
                double z=Math.abs(to.get(2).getAsDouble()-from.get(2).getAsDouble());
                if (!Double.isFinite(x+y+z) || x+y+z>192) return Observation.UNKNOWN;
                JsonObject faces=object.getAsJsonObject("faces");
                if (faces == null) continue;
                volume+=x*y*z;
                for (var face:faces.entrySet()) {
                    double weight=switch (face.getKey()) { case "up","down" -> x*z; case "east","west" -> y*z; default -> x*y; };
                    if (weight<=0) continue;
                    String texture=face.getValue().getAsJsonObject().get("texture").getAsString();
                    Set<String> aliases = new HashSet<>();
                    for (int n=0;texture.startsWith("#") && n<16;n++) {
                        if (!aliases.add(texture)) return Observation.UNKNOWN;
                        texture=textures.getOrDefault(texture.substring(1),"#missing");
                    }
                    if (texture.startsWith("#")) return Observation.UNKNOWN;
                    double alpha=alpha(texture);
                    if (alpha<0) return Observation.UNKNOWN;
                    area+=weight; transparentArea+=weight*alpha;
                }
            }
            // Empty/border models can reveal ores even with entirely opaque textures.
            return new Observation(true,area>0 && transparentArea/area>=.65,volume<819.2 || area<384);
        } catch (Exception exception) { return Observation.UNKNOWN; }
    }

    private JsonObject model(String name) throws IOException {
        if (models.containsKey(name)) return models.get(name);
        ResourceLocation id=ResourceLocation.parse(name);
        byte[] data=read(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"models/"+id.getPath()+".json"),65536);
        JsonObject result=data==null?null:JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();
        models.put(name,result); return result;
    }

    private double alpha(String name) throws IOException {
        String key=name.contains(":")?name:"minecraft:"+name;
        if (alphas.containsKey(key)) return alphas.get(key);
        ResourceLocation id=ResourceLocation.parse(key);
        byte[] data=read(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"textures/"+id.getPath()+".png"),2*1024*1024);
        double result=-1;
        // Validate header/dimensions BEFORE native decoding, including tall animated strips.
        if (data!=null && data.length>=33 && ByteBuffer.wrap(data).getLong()==0x89504e470d0a1a0aL
                && ByteBuffer.wrap(data,12,4).getInt()==0x49484452) {
            int width=ByteBuffer.wrap(data,16,4).getInt(),height=ByteBuffer.wrap(data,20,4).getInt();
            if (width>0 && height>0 && width<=2048 && height<=8192 && (long)width*height<=4194304) {
                try (NativeImage image=NativeImage.read(new ByteArrayInputStream(data))) {
                    int samples=0, transparent=0;
                    // Fixed 256 pixel samples cover the entire PNG, not only animation frame zero.
                    for (int x=0;x<16;x++) for (int y=0;y<16;y++) {
                        int pixel=image.getPixelRGBA(Math.min(width-1,(x*width+width/2)/16),Math.min(height-1,(y*height+height/2)/16));
                        if ((pixel>>>24)<96) transparent++;
                        samples++;
                    }
                    result=ResourceEvidence.transparentSample(transparent,samples)?1:0;
                }
            }
        }
        alphas.put(key,result); return result;
    }

    private byte[] read(ResourceLocation id,int limit) throws IOException {
        if (bytesLeft<=0) return null;
        var resource=resources.getResource(id);
        if (resource.isEmpty()) return null;
        int allowed=Math.min(limit,bytesLeft);
        byte[] bytes;
        try (InputStream input=resource.get().open()) { bytes=input.readNBytes(allowed+1); }
        bytesLeft-=bytes.length;
        digest.update(id.toString().getBytes(StandardCharsets.UTF_8)); digest.update((byte)0); digest.update(bytes);
        return bytes.length>allowed?null:bytes;
    }
}
