package net.caravidro.wayaround.security.client;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.security.ResourceEvidence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

/** Explicit CI opt-in. Real PNG/model fixtures over the actual Minecraft resource manager. */
final class XrayResourceAuditValidation {
    private static boolean done;
    private static final String[] HOSTS={"stone","dirt","sand","gravel","andesite","diorite","granite","tuff"};
    static void run(ResourceManager base,ProfilerFiller profiler) {
        if(done)return;done=true;
        try {
            ResourceEvidence normal=audit(base,Map.of(),profiler);
            require(normal.clean() && normal.opaqueOres()==15,"Vanilla/mod terrain is clean: "+normal.summary());
            Map<ResourceLocation,byte[]> replacements=new HashMap<>();byte[] clear=png(0),solid=png(255);
            for(String host:HOSTS)replacements.put(id("textures/block/"+host+".png"),clear);
            ResourceEvidence xray=audit(base,replacements,profiler);
            require(xray.strong() && Integer.bitCount(xray.transparent())>=8,"Broad alpha x-ray detected: "+xray.summary());
            for(String host:HOSTS)replacements.put(id("textures/block/"+host+".png"),solid);
            require(audit(base,replacements,profiler).clean(),"Opaque artistic replacements are allowed");
            replacements.clear();replacements.put(id("textures/block/stone.png"),clear);
            require(!audit(base,replacements,profiler).strong(),"One transparent texture cannot convict");
            replacements.clear();for(String host:HOSTS)replacements.put(id("models/block/"+host+".json"),"{\"elements\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ResourceEvidence models=audit(base,replacements,profiler);
            require(models.strong() && Integer.bitCount(models.hiddenModels())==8,"Model-only x-ray detected");
            replacements.clear();for(String host:HOSTS)replacements.put(id("models/block/"+host+".json"),"{\"loader\":\"other:custom\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ResourceEvidence unsupported=audit(base,replacements,profiler);
            require(!unsupported.strong() && !unsupported.clean(),"Unsupported model loader is unknown, never proof");
            replacements.clear();for(String host:HOSTS)replacements.put(id("textures/block/"+host+".png"),new byte[40]);
            ResourceEvidence malformed=audit(base,replacements,profiler);
            require(!malformed.strong() && !malformed.clean(),"Malformed PNG is unknown without native decode");
            require(!normal.fingerprint().equals(xray.fingerprint()) && !xray.fingerprint().equals(models.fingerprint()),"Resource content fingerprint distinguishes changes");
            replacements.clear();
            replacements.put(ResourceLocation.fromNamespaceAndPath("auditfixture","models/block/empty.json"),"{\"elements\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for(String host:HOSTS)replacements.put(id("blockstates/"+host+".json"),"{\"variants\":{\"\":{\"model\":\"auditfixture:block/empty\"}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            require(audit(base,replacements,profiler).strong(),"Blockstate redirection to foreign empty model detected");
            WayAround.LOGGER.info("[AntiXray] CLIENT RESOURCE FIXTURES PASSED: vanilla, alpha, opaque art, single texture, empty models, unsupported loader, invalid PNG, fingerprints, blockstate redirects");
        } catch(Exception exception) { throw new IllegalStateException("AntiXray client resource validation failed",exception); }
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("minecraft",path); }
    private static ResourceEvidence audit(ResourceManager base,Map<ResourceLocation,byte[]> overrides,ProfilerFiller profiler) {
        ResourceManager manager=(ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)-> {
            if(method.getName().equals("getResource") && args!=null && overrides.containsKey(args[0])) {
                byte[] bytes=overrides.get(args[0]);
                return Optional.of(new Resource(null,()->new ByteArrayInputStream(bytes)));
            }
            return method.invoke(base,args);
        });
        return new XrayResourceAudit().prepare(manager,profiler);
    }
    private static byte[] png(int alpha) throws IOException {
        java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int x=0;x<16;x++)for(int y=0;y<16;y++)image.setRGB(x,y,(alpha<<24)|0x808080);
        ByteArrayOutputStream output=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);return output.toByteArray();
    }
    private static void require(boolean pass,String message) { if(!pass)throw new AssertionError(message); }
}
