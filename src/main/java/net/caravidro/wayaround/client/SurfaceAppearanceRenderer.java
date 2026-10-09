package net.caravidro.wayaround.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.appearance.SurfaceAppearance;
import net.caravidro.wayaround.appearance.SurfaceAppearanceClientCache;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Non-destructive, runtime pixel decals over existing block textures.
 * Persistent iron corrosion comes from TemporalAgingData via a sparse S2C
 * snapshot. Short-lived rain puddles are purely visual, never new blocks.
 *
 * One draw submission, bounded sampled surfaces, no per-frame textures,
 * entities, chunk rebuilds, or interaction with dedicated-server classes.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class SurfaceAppearanceRenderer {
    private static final Direction[] FACES = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final int MAX_PUDDLES = 240;
    private static final int RADIUS = 22;
    private static final LinkedHashMap<Long, Float> PUDDLES = new LinkedHashMap<>();
    private static ClientLevel owner;
    private static int ticks;

    private SurfaceAppearanceRenderer() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (owner != mc.level) {
            owner = mc.level;
            PUDDLES.clear();
            SurfaceAppearanceClientCache.clear();
            ticks = 0;
        }
        if (owner == null || mc.player == null || mc.isPaused()) return;
        if (++ticks % 8 != 0) return;

        Iterator<Map.Entry<Long, Float>> it = PUDDLES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            if (!owner.hasChunkAt(pos)
                    || !SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))) {
                it.remove();
                continue;
            }
            float goal = WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)
                    && owner.isRainingAt(pos.above()) ? 1.0F : 0.0F;
            float next = goal > 0
                    ? Math.min(1.0F, entry.getValue() + 0.10F)
                    : Math.max(0.0F, entry.getValue() - 0.008F);
            if (next <= 0.001F) it.remove();
            else entry.setValue(next);
        }

        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)
                || !owner.isRaining()) return;

        int cx = mc.player.getBlockX(), cz = mc.player.getBlockZ();
        int cy = mc.player.getBlockY();
        for (int i = 0; i < 24; i++) {
            int seed = SurfaceAppearance.hash(BlockPos.asLong(cx, cy, cz),
                    ticks / 8, i);
            int x = cx + Math.floorMod(seed, RADIUS * 2 + 1) - RADIUS;
            int z = cz + Math.floorMod(seed >>> 8, RADIUS * 2 + 1) - RADIUS;
            if (!owner.hasChunkAt(new BlockPos(x, cy, z))) continue;
            int y = owner.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            if (Math.abs(y - cy) > 8) continue;
            BlockPos pos = new BlockPos(x, y, z);
            if (!owner.hasChunkAt(pos)
                    || !owner.isRainingAt(pos.above())
                    || !SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))
                    || !owner.getBlockState(pos.above()).isAir()
                    || !owner.getBlockState(pos).isSolidRender(owner, pos)) continue;
            long key = pos.asLong();
            PUDDLES.put(key, Math.min(1.0F, PUDDLES.getOrDefault(key, 0.0F) + 0.13F));
            while (PUDDLES.size() > MAX_PUDDLES) {
                PUDDLES.remove(PUDDLES.keySet().iterator().next());
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || owner == null || Minecraft.getInstance().level != owner) return;

        boolean rust = WorldFeatureRuntime.clientEnabled(WorldFeature.TIME_AGING);
        boolean puddles = WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER);
        if (!rust && !puddles) return;

        Vec3 cam = event.getCamera().getPosition();
        Matrix4f pose = event.getPoseStack().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int count = 0;

        if (rust) {
            for (var entry : SurfaceAppearanceClientCache.snapshot().entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                float strength = entry.getValue();
                if (strength < 0.02F || !near(pos, cam)
                        || !owner.hasChunkAt(pos)
                        || !SurfaceAppearance.isFerrous(owner.getBlockState(pos))) continue;
                for (Direction face : FACES) {
                    if (!owner.getBlockState(pos.relative(face)).isAir()) continue;
                    int flecks = 2 + (int) (strength * 6.0F);
                    for (int i = 0; i < flecks; i++) {
                        int h = SurfaceAppearance.hash(pos.asLong(), face.ordinal(), i);
                        float u = 0.06F + SurfaceAppearance.unit(h) * 0.76F;
                        float v = 0.06F + SurfaceAppearance.unit(h * 31 + 17) * 0.76F;
                        float size = (0.045F + SurfaceAppearance.unit(h ^ 0x6AD4) * 0.13F)
                                * (0.4F + strength * 0.6F);
                        int alpha = Math.min(210, (int) (strength * 230.0F));
                        int red = 90 + (h & 31);
                        int green = 35 + ((h >>> 5) & 19);
                        surfaceQuad(buffer, pose, cam, pos, face,
                                u, v, size, size * (0.65F + SurfaceAppearance.unit(~h) * 0.6F),
                                red, green, 19, alpha);
                        count++;
                    }
                }
            }
        }
        if (puddles) {
            for (var entry : PUDDLES.entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                if (!near(pos, cam) || !owner.hasChunkAt(pos)) continue;
                float wetness = entry.getValue();
                int hash = SurfaceAppearance.hash(pos.asLong(), 9, 0);
                float x = 0.10F + SurfaceAppearance.unit(hash) * 0.30F;
                float z = 0.10F + SurfaceAppearance.unit(hash ^ 0x5F27) * 0.30F;
                float size = 0.20F + SurfaceAppearance.unit(hash >>> 3) * 0.26F;
                surfaceQuad(buffer, pose, cam, pos, Direction.UP,
                        x, z, size, size * 0.65F, 23, 50, 70,
                        (int) (wetness * 98));
                surfaceQuad(buffer, pose, cam, pos, Direction.UP,
                        x + size * 0.15F, z + size * 0.18F,
                        size * 0.56F, size * 0.22F, 89, 132, 149,
                        (int) (wetness * 78));
                count += 2;
            }
        }
        if (count == 0) {
            buffer.build();
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static boolean near(BlockPos pos, Vec3 camera) {
        double dx = pos.getX() + 0.5 - camera.x;
        double dy = pos.getY() + 0.5 - camera.y;
        double dz = pos.getZ() + 0.5 - camera.z;
        return dx * dx + dy * dy + dz * dz < 30.0 * 30.0;
    }

    /** Draws pixels on any cube face with a slight offset to avoid z-fighting. */
    private static void surfaceQuad(BufferBuilder b, Matrix4f pose, Vec3 camera,
            BlockPos pos, Direction face, float u, float v, float width, float height,
            int r, int g, int blue, int a) {
        float x = (float) (pos.getX() - camera.x);
        float y = (float) (pos.getY() - camera.y);
        float z = (float) (pos.getZ() - camera.z);
        float epsilon = 0.006F;
        // Axis-aligned basis vectors avoid thousands of temporary float[4][3]
        // arrays every frame when hundreds of pixels are visible.
        float bx, by, bz, ux, uy, uz, vx, vy, vz;
        switch (face) {
            case UP -> {
                bx = x + u; by = y + 1 + epsilon; bz = z + v;
                ux = width; uy = 0; uz = 0;
                vx = 0; vy = 0; vz = height;
            }
            case NORTH -> {
                bx = x + u; by = y + v; bz = z - epsilon;
                ux = width; uy = 0; uz = 0;
                vx = 0; vy = height; vz = 0;
            }
            case SOUTH -> {
                bx = x + u; by = y + v; bz = z + 1 + epsilon;
                ux = width; uy = 0; uz = 0;
                vx = 0; vy = height; vz = 0;
            }
            case EAST -> {
                bx = x + 1 + epsilon; by = y + v; bz = z + u;
                ux = 0; uy = 0; uz = width;
                vx = 0; vy = height; vz = 0;
            }
            case WEST -> {
                bx = x - epsilon; by = y + v; bz = z + u;
                ux = 0; uy = 0; uz = width;
                vx = 0; vy = height; vz = 0;
            }
            default -> { return; }
        }
        b.addVertex(pose, bx, by, bz).setColor(r, g, blue, a);
        b.addVertex(pose, bx + vx, by + vy, bz + vz).setColor(r, g, blue, a);
        b.addVertex(pose, bx + ux + vx, by + uy + vy, bz + uz + vz)
                .setColor(r, g, blue, a);
        b.addVertex(pose, bx + ux, by + uy, bz + uz).setColor(r, g, blue, a);
    }
}
