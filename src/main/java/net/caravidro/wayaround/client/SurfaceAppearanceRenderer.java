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
import net.caravidro.wayaround.appearance.PuddleDebugClientCache;
import net.caravidro.wayaround.appearance.PuddleSilhouette;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
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
    private static final int MAX_PUDDLES = 144;
    private static final int RADIUS = 12;
    private static final LinkedHashMap<Long, Float> PUDDLES = new LinkedHashMap<>();
    private static final LinkedHashMap<Long, Long> DEBUG_UNTIL = new LinkedHashMap<>();
    private static ClientLevel owner;
    private static int ticks;

    private SurfaceAppearanceRenderer() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (owner != mc.level) {
            owner = mc.level;
            PUDDLES.clear();
            DEBUG_UNTIL.clear();
            // Keep the network mailbox through the first client-level tick.
            // Packets can arrive before owner is assigned on world join.
            SurfaceAppearanceClientCache.clear();
            ticks = 0;
        }
        if (owner == null || mc.player == null || mc.isPaused()) return;
        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)) {
            PUDDLES.clear();
            DEBUG_UNTIL.clear();
            PuddleDebugClientCache.clear();
            return; // Disabling the mechanic restores vanilla surfaces.
        }
        long time = owner.getGameTime();
        for (var update : PuddleDebugClientCache.drain().entrySet()) {
            if (update.getValue() <= 0) {
                DEBUG_UNTIL.remove(update.getKey());
                PUDDLES.remove(update.getKey());
            } else {
                if (DEBUG_UNTIL.size() >= 48 && !DEBUG_UNTIL.containsKey(update.getKey())) {
                    DEBUG_UNTIL.remove(DEBUG_UNTIL.keySet().iterator().next());
                }
                DEBUG_UNTIL.put(update.getKey(), time + update.getValue());
                PUDDLES.put(update.getKey(), 1.0F);
            }
        }
        if (++ticks % 8 != 0) return;
        DEBUG_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= time);
        Iterator<Map.Entry<Long, Float>> it = PUDDLES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            if (!owner.hasChunkAt(pos)
                    || !SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))) {
                it.remove();
                continue;
            }
            float goal = DEBUG_UNTIL.containsKey(entry.getKey()) || rainingOn(pos) ? 1.0F : 0.0F;
            float next = goal > 0
                    ? Math.min(1.0F, entry.getValue() + 0.10F)
                    : Math.max(0.0F, entry.getValue() - 0.008F);
            if (next <= 0.001F) it.remove();
            else entry.setValue(next);
        }

        if (!owner.isRaining()
                && (!WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                || LocalWeatherField.sample(owner, mc.player.getX(),
                        mc.player.getZ(), owner.getGameTime()).rain() < 0.08F)) return;

        int cx = mc.player.getBlockX(), cz = mc.player.getBlockZ();
        int cy = mc.player.getBlockY();

        // Dense coverage close to the player, then a few distant samples.
        // The old 24 random probes in a 45x45 square missed small walkways.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                sampleColumn(cx + dx, cz + dz, cy);
            }
        }
        // When testing a small structure, guarantee its looked-at floor is checked.
        if (mc.hitResult instanceof BlockHitResult hit) {
            var pos = hit.getBlockPos();
            if (Math.abs(pos.getX() - cx) <= 12
                    && Math.abs(pos.getZ() - cz) <= 12) {
                sampleColumn(pos.getX(), pos.getZ(), cy);
            }
        }
        for (int i = 0; i < 16; i++) {
            int seed = SurfaceAppearance.hash(BlockPos.asLong(cx, cy, cz), ticks / 8, i);
            int dx = Math.floorMod(seed, RADIUS * 2 + 1) - RADIUS;
            int dz = Math.floorMod(seed >>> 8, RADIUS * 2 + 1) - RADIUS;
            sampleColumn(cx + dx, cz + dz, cy);
        }
    }

    private static void sampleColumn(int x, int z, int playerY) {
        BlockPos column = new BlockPos(x, playerY, z);
        if (!owner.hasChunkAt(column)) return;
        int y = owner.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (Math.abs(y - playerY) > 8) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (!SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))
                || !owner.getBlockState(pos).isSolidRender(owner, pos)
                || !owner.getBlockState(pos.above()).isAir()
                || !rainingOn(pos)) return;
        long key = pos.asLong();
        PUDDLES.put(key, Math.min(1.0F, PUDDLES.getOrDefault(key, 0.0F) + 0.22F));
        while (PUDDLES.size() > MAX_PUDDLES) {
            long oldest = PUDDLES.keySet().iterator().next();
            if (DEBUG_UNTIL.containsKey(oldest)) {
                // Debug puddles must remain visible throughout their short lifetime.
                PUDDLES.remove(oldest);
                PUDDLES.put(oldest, 1.0F);
                continue;
            }
            PUDDLES.remove(oldest);
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
                    int flecks = 5 + (int) (strength * 28.0F);
                    for (int i = 0; i < flecks; i++) {
                        int h = SurfaceAppearance.hash(pos.asLong(), face.ordinal(), i);
                        float u = 0.06F + SurfaceAppearance.unit(h) * 0.76F;
                        float v = 0.06F + SurfaceAppearance.unit(h * 31 + 17) * 0.76F;
                        float size = (0.060F + SurfaceAppearance.unit(h ^ 0x6AD4) * 0.19F)
                                * (0.45F + strength * 0.55F);
                        int alpha = Math.min(245, (int) (strength * 270.0F));
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
                if (!near(pos, cam) || !owner.hasChunkAt(pos)
                        || !SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))) continue;
                count += drawPuddle(buffer, pose, cam, pos, entry.getValue(), owner.getGameTime());
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

    /**
     * Painted 32-column silhouette: irregular full-body scanlines, a darker
     * edge halo and translucent blue-gray interior. No tiled square patches,
     * runtime image files, dynamic texture atlas or new per-block entities.
     */
    private static int drawPuddle(BufferBuilder buffer, Matrix4f pose, Vec3 camera,
            BlockPos pos, float wetness, long worldTime) {
        if (wetness <= 0.001F) return 0;
        int shapes = 0;
        long seed = pos.asLong();
        float growth = 0.40F + 0.60F * Math.min(1F, wetness * 1.6F);
        float originZ = .10F;
        float rowHeight = .80F / PuddleSilhouette.ROWS;
        float baseA = Math.min(1F, wetness);
        for (int row = 0; row < PuddleSilhouette.ROWS; row++) {
            int packed = PuddleSilhouette.span(seed, row);
            if (packed == 0) continue;
            float min = ((packed >>> 8) & 255) / (float) PuddleSilhouette.PIXELS;
            float max = (packed & 255) / (float) PuddleSilhouette.PIXELS;
            float x = .5F + (min - .5F) * growth;
            float z = .5F + (originZ + row * rowHeight - .5F) * growth;
            float width = (max - min) * growth;
            float depth = rowHeight * growth;
            int noise = PuddleSilhouette.hash(seed + row * 0x9E3779B97F4A7C15L);
            int shade = (noise >>> 17) & 15;
            // Silhouette border first, darker and a little wider.
            topQuad(buffer, pose, camera, pos,
                    x - .015F, z - .012F, width + .030F, depth + .024F,
                    12 + shade / 3, 24 + shade / 2, 39 + shade,
                    (int) (baseA * 165F), .0045F);
            // A continuous water body with modest tonal differences per row.
            topQuad(buffer, pose, camera, pos, x, z, width, depth,
                    31 + shade, 65 + shade, 90 + shade,
                    (int) (baseA * 190F), .007F);
            shapes += 2;
            // Pixel-art reflections: short bright streaks inside the silhouette.
            if (row > 3 && row < PuddleSilhouette.ROWS - 4
                    && (noise & 7) <= 2 && width > .18F) {
                float u = ((noise >>> 8) & 15) / 32.0F;
                float start = x + width * (.18F + u);
                float glintWidth = Math.min(width * .32F, .08F + ((noise >>> 22) & 3) * .016F);
                if (start + glintWidth < x + width - .025F) {
                    topQuad(buffer, pose, camera, pos, start, z + depth * .23F,
                            glintWidth, depth * .38F, 138, 183, 194,
                            (int) (baseA * 130F), .009F);
                    shapes++;
                }
            }
        }
        // Low-intensity moving rain rings, three tiny glints instead of a
        // particle entity on every square of wet terrain.
        if (wetness > .42F) {
            int phase = (int) ((worldTime / 9L + (PuddleSilhouette.hash(seed) & 63)) % 60L);
            float ripple = phase / 60F;
            int opacity = (int) (baseA * (1F - ripple) * 100F);
            if (opacity > 3) {
                float half = .035F + ripple * .16F;
                topQuad(buffer, pose, camera, pos,
                        .5F - half, .45F - half * .65F,
                        half * .75F, .010F, 155, 192, 204, opacity, .010F);
                topQuad(buffer, pose, camera, pos,
                        .5F + half * .35F, .45F + half * .65F,
                        half * .65F, .010F, 155, 192, 204, opacity, .010F);
                shapes += 2;
            }
        }
        return shapes;
    }

    private static void topQuad(BufferBuilder b, Matrix4f pose, Vec3 cam,
            BlockPos pos, float u, float v, float width, float depth,
            int r, int g, int blue, int a, float offset) {
        float x = (float) (pos.getX() - cam.x) + u;
        float y = (float) (pos.getY() - cam.y) + 1F + offset;
        float z = (float) (pos.getZ() - cam.z) + v;
        b.addVertex(pose, x, y, z).setColor(r, g, blue, a);
        b.addVertex(pose, x, y, z + depth).setColor(r, g, blue, a);
        b.addVertex(pose, x + width, y, z + depth).setColor(r, g, blue, a);
        b.addVertex(pose, x + width, y, z).setColor(r, g, blue, a);
    }

    private static boolean rainingOn(BlockPos pos) {
        BlockPos sky = pos.above();
        if (!owner.hasChunkAt(pos) || !owner.canSeeSky(sky)) return false;
        if (owner.isRainingAt(sky)) return true;
        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                || !WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)) return false;
        return LocalWeatherField.sample(owner, pos.getX() + .5, pos.getZ() + .5,
                owner.getGameTime()).rain() >= 0.16F;
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
