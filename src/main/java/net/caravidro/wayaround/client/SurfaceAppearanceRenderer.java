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
import net.caravidro.wayaround.appearance.PuddleField;
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
    private static final int MAX_PUDDLES = 88;
    private static final int MAX_RENDER_PUDDLES = 28;
    private static final int GRID = PuddleField.RESOLUTION;
    private static final float[] SAMPLE = new float[(GRID + 1) * (GRID + 1)];
    // Reuse scratch space instead of creating float arrays per mesh cell.
    private static final float[] POLY_X = new float[8], POLY_Z = new float[8];
    private static final float[] CORNER_X = new float[4], CORNER_Z = new float[4],
                                 CORNER_DENSITY = new float[4];
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
        // Deterministic, sparse anchors. Each puddle may span several blocks:
        // creating one per ground block would bury the world in overlapping decals.
        if (!DEBUG_UNTIL.containsKey(key)
                && Math.floorMod(PuddleField.mix(key ^ 0xC231A96EEEDL), 7) != 0) return;
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
            int rendered = 0;
            // Explicit visual tests have priority even in a dense rainfall scene.
            for (var entry : PUDDLES.entrySet()) {
                if (!DEBUG_UNTIL.containsKey(entry.getKey())) continue;
                BlockPos pos = BlockPos.of(entry.getKey());
                if (!near(pos, cam) || !owner.hasChunkAt(pos)) continue;
                count += drawPuddle(buffer, pose, cam, pos, entry.getValue(), owner.getGameTime());
                if (++rendered >= MAX_RENDER_PUDDLES) break;
            }
            if (rendered < MAX_RENDER_PUDDLES) {
                for (var entry : PUDDLES.entrySet()) {
                    if (DEBUG_UNTIL.containsKey(entry.getKey())) continue;
                    BlockPos pos = BlockPos.of(entry.getKey());
                    if (!near(pos, cam) || !owner.hasChunkAt(pos)) continue;
                    count += drawPuddle(buffer, pose, cam, pos, entry.getValue(), owner.getGameTime());
                    if (++rendered >= MAX_RENDER_PUDDLES) break;
                }
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
     * Project one implicit world-space decal across the anchor's solid top and
     * up to eight adjacent exposed, coplanar full blocks. No floating bridges
     * across missing blocks, no puddles up walls or into unloaded chunks.
     *
     * Each cell is clipped to an interpolated 0-isocontour (marching-squares
     * style) before triangulation. The world field is sampled by ABSOLUTE
     * coordinate, so the boundary is mathematically identical at block edges.
     */
    private static int drawPuddle(BufferBuilder b, Matrix4f pose, Vec3 cam,
            BlockPos anchor, float wetness, long time) {
        if (wetness < .012F || !validPuddleFloor(anchor)) return 0;
        PuddleField.Shape shape = PuddleField.describe(
                anchor.asLong(), anchor.getX(), anchor.getZ(), wetness);
        int count = 0;
        for (int ox = -PuddleField.BLOCK_REACH; ox <= PuddleField.BLOCK_REACH; ox++) {
            for (int oz = -PuddleField.BLOCK_REACH; oz <= PuddleField.BLOCK_REACH; oz++) {
                BlockPos tile = anchor.offset(ox, 0, oz);
                if (!validPuddleFloor(tile)) continue;
                if (!canProjectFromAnchor(anchor, tile)) continue;
                count += drawTile(b, pose, cam, tile, shape, wetness, time);
            }
        }
        return count;
    }

    /** Require an unbroken cardinal route on the same y-level to a neighbor. */
    private static boolean canProjectFromAnchor(BlockPos origin, BlockPos neighbor) {
        int dx = neighbor.getX() - origin.getX(), dz = neighbor.getZ() - origin.getZ();
        if (dx != 0 && dz != 0) {
            // A diagonal only connects if both orthogonal support surfaces exist.
            return validPuddleFloor(origin.offset(dx, 0, 0))
                    && validPuddleFloor(origin.offset(0, 0, dz));
        }
        return true;
    }

    private static boolean validPuddleFloor(BlockPos pos) {
        if (!owner.hasChunkAt(pos)) return false;
        return SurfaceAppearance.supportsPuddles(owner.getBlockState(pos))
                && owner.getBlockState(pos).isSolidRender(owner, pos)
                && owner.getBlockState(pos.above()).isAir();
    }

    private static int drawTile(BufferBuilder b, Matrix4f pose, Vec3 cam,
            BlockPos tile, PuddleField.Shape shape, float wetness, long time) {
        int stride = GRID + 1, count = 0;
        for (int z = 0; z <= GRID; z++) {
            for (int x = 0; x <= GRID; x++) {
                double worldX = tile.getX() + x / (double) GRID;
                double worldZ = tile.getZ() + z / (double) GRID;
                SAMPLE[z * stride + x] = (float) PuddleField.density(shape, worldX, worldZ);
            }
        }
        float alpha = Math.min(1F, wetness);
        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                float s = 1F / GRID;
                float x0 = tile.getX() + x * s, x1 = x0 + s;
                float z0 = tile.getZ() + z * s, z1 = z0 + s;
                // Counterclockwise x/z rectangle; renderer disables backface culling.
                CORNER_X[0] = x0; CORNER_Z[0] = z0;
                CORNER_X[1] = x0; CORNER_Z[1] = z1;
                CORNER_X[2] = x1; CORNER_Z[2] = z1;
                CORNER_X[3] = x1; CORNER_Z[3] = z0;
                CORNER_DENSITY[0] = SAMPLE[z * stride + x];
                CORNER_DENSITY[1] = SAMPLE[(z + 1) * stride + x];
                CORNER_DENSITY[2] = SAMPLE[(z + 1) * stride + x + 1];
                CORNER_DENSITY[3] = SAMPLE[z * stride + x + 1];

                float max = Math.max(Math.max(CORNER_DENSITY[0], CORNER_DENSITY[1]),
                        Math.max(CORNER_DENSITY[2], CORNER_DENSITY[3]));
                if (max <= -.09F) continue;
                float centerX = (x0 + x1) * .5F, centerZ = (z0 + z1) * .5F;
                float tint = (float) PuddleField.valueNoise(centerX * 2.4D,
                        centerZ * 2.4D, shape.seed() ^ 0xACA49937L);
                int shade = Math.round(tint * 13.0F);
                // Lower dark edge is wider than the inner blue-gray water.
                count += emitContour(b, pose, cam, tile, -.075F,
                        16 + shade, 33 + shade, 47 + shade,
                        Math.round(alpha * 150), .0045F);
                count += emitContour(b, pose, cam, tile, .015F,
                        44 + shade, 78 + shade, 103 + shade,
                        Math.round(alpha * 194), .007F);

                // Patchy pearly highlights are also contoured, never box-shaped.
                // A small time phase makes the reflected shine subtly shift.
                if (((PuddleField.mix(shape.seed() + (long) Math.floor(centerX * 17D) * 53L + (long) Math.floor(centerZ * 17D)) >>> 7) & 3L) == 0) {
                    count += emitContour(b, pose, cam, tile,
                            .46F + .045F * (float) Math.sin(time * .022D + centerX * 3.2D),
                            147, 189, 206, Math.round(alpha * 83), .009F);
                }
            }
        }
        return count;
    }

    /**
     * Clip one quad's corners against density >= threshold; interpolate edges,
     * then fan-triangulate. Quad mode uses duplicated last corner for triangles
     * (a degenerate quad). This preserves the existing batched rust pipeline.
     */
    private static int emitContour(BufferBuilder b, Matrix4f pose, Vec3 cam,
            BlockPos tile, float threshold, int red, int green, int blue, int alpha, float lift) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            int prev = (i + 3) % 4;
            float a = CORNER_DENSITY[prev] - threshold;
            float c = CORNER_DENSITY[i] - threshold;
            boolean wasInside = a >= 0F, isInside = c >= 0F;
            if (wasInside != isInside) {
                float t = a / (a - c);
                POLY_X[n] = CORNER_X[prev] + (CORNER_X[i] - CORNER_X[prev]) * t;
                POLY_Z[n] = CORNER_Z[prev] + (CORNER_Z[i] - CORNER_Z[prev]) * t;
                n++;
            }
            if (isInside) {
                POLY_X[n] = CORNER_X[i];
                POLY_Z[n] = CORNER_Z[i];
                n++;
            }
        }
        if (n < 3) return 0;
        float y = (float) (tile.getY() + 1D + lift - cam.y);
        if (n == 4) {
            for (int k = 0; k < 4; k++) {
                puddleVertex(b, pose, cam, POLY_X[k], y, POLY_Z[k], red, green, blue, alpha);
            }
            return 1;
        }
        int emitted = 0;
        for (int k = 1; k < n - 1; k++) {
            puddleVertex(b, pose, cam, POLY_X[0], y, POLY_Z[0], red, green, blue, alpha);
            puddleVertex(b, pose, cam, POLY_X[k], y, POLY_Z[k], red, green, blue, alpha);
            puddleVertex(b, pose, cam, POLY_X[k + 1], y, POLY_Z[k + 1], red, green, blue, alpha);
            puddleVertex(b, pose, cam, POLY_X[k + 1], y, POLY_Z[k + 1], red, green, blue, alpha);
            emitted++;
        }
        return emitted;
    }

    private static void puddleVertex(BufferBuilder b, Matrix4f pose, Vec3 cam,
            float worldX, float localY, float worldZ, int red, int green, int blue, int alpha) {
        b.addVertex(pose, (float) (worldX - cam.x), localY, (float) (worldZ - cam.z))
                .setColor(red, green, blue, alpha);
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
