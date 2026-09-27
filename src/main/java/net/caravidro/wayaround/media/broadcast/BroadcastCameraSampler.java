package net.caravidro.wayaround.media.broadcast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BroadcastCameraSampler {
    public static final int WIDTH = 64;
    public static final int HEIGHT = 36;
    private static final double RANGE = 72.0;

    private BroadcastCameraSampler() {}

    public static byte[] capture(ServerLevel level, BlockPos camera, Direction facing) {
        Vec3 origin = new Vec3(
                /*
                 * Start the ray outside the camera's own collision box.
                 * The old 0.34 offset let many rays immediately hit the
                 * camera block itself, producing the infamous orange TV.
                 */
                camera.getX() + 0.5 + facing.getStepX() * 0.86,
                camera.getY() + 0.68,
                camera.getZ() + 0.5 + facing.getStepZ() * 0.86
        );
        Vec3 forward = new Vec3(facing.getStepX(), 0.0, facing.getStepZ()).normalize();
        return capture(level, origin, forward);
    }

    public static byte[] capture(ServerLevel level, Vec3 origin, Vec3 forward) {
        byte[] rgb = new byte[WIDTH * HEIGHT * 3];

        Vec3 normalized = forward.lengthSqr() < 0.001
                ? new Vec3(0, 0, 1)
                : forward.normalize();

        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = normalized.cross(worldUp);
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
        else right = right.normalize();

        Vec3 up = right.cross(normalized).normalize();

        double tanHalf = Math.tan(Math.toRadians(68.0 * 0.5));
        double aspect = WIDTH / (double) HEIGHT;

        for (int y = 0; y < HEIGHT; y++) {
            double sy = (1.0 - 2.0 * ((y + 0.5) / HEIGHT)) * tanHalf / aspect;

            for (int x = 0; x < WIDTH; x++) {
                double sx = (2.0 * ((x + 0.5) / WIDTH) - 1.0) * tanHalf;

                Vec3 ray = normalized
                        .add(right.scale(sx))
                        .add(up.scale(sy))
                        .normalize();

                Vec3 end = origin.add(ray.scale(RANGE));

                HitResult hit = level.clip(new ClipContext(
                        origin,
                        end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.ANY,
                        CollisionContext.empty()
                ));

                int color;

                if (hit.getType() != HitResult.Type.BLOCK) {
                    double sky = Math.max(0.0, Math.min(1.0, 0.5 + ray.y * 0.5));
                    int r = (int) (76 + 45 * sky);
                    int g = (int) (108 + 60 * sky);
                    int b = (int) (135 + 86 * sky);
                    color = rgb(r, g, b);
                } else {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = level.getBlockState(pos);
                    double distance = hit.getLocation().distanceTo(origin);
                    color = shade(blockColor(level, pos, state), distance);
                }

                int index = (y * WIDTH + x) * 3;
                rgb[index] = (byte) ((color >> 16) & 0xFF);
                rgb[index + 1] = (byte) ((color >> 8) & 0xFF);
                rgb[index + 2] = (byte) (color & 0xFF);
            }
        }

        return rgb;
    }

    public static byte[] intermissionFrame() {
        byte[] rgb = new byte[WIDTH * HEIGHT * 3];
        int[] bars = {
                rgb(224, 224, 190),
                rgb(210, 202, 54),
                rgb(48, 188, 184),
                rgb(54, 184, 70),
                rgb(188, 52, 178),
                rgb(188, 48, 52),
                rgb(52, 68, 188)
        };

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int bar = Math.min(bars.length - 1, x * bars.length / WIDTH);
                int color = bars[bar];

                if (y > HEIGHT - 4) {
                    int gray = ((x / 3) % 2 == 0) ? 36 : 210;
                    color = rgb(gray, gray, gray);
                }

                int i = (y * WIDTH + x) * 3;
                rgb[i] = (byte) ((color >> 16) & 0xFF);
                rgb[i + 1] = (byte) ((color >> 8) & 0xFF);
                rgb[i + 2] = (byte) (color & 0xFF);
            }
        }

        return rgb;
    }

    private static int blockColor(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) return rgb(42, 88, 126);
        if (level.getFluidState(pos).is(FluidTags.LAVA)) return rgb(232, 88, 20);
        if (state.is(BlockTags.LEAVES)) return rgb(54, 104, 47);
        if (state.is(BlockTags.LOGS)) return rgb(102, 72, 43);
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) return rgb(72, 124, 55);
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) return rgb(112, 83, 56);
        if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE)) return rgb(201, 184, 119);
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE)) return rgb(205, 224, 228);
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)) return rgb(116, 118, 118);
        if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)) return rgb(67, 69, 71);
        if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) return rgb(184, 207, 214);
        if (state.is(BlockTags.PLANKS)) return rgb(151, 116, 73);
        if (state.is(Blocks.BRICKS)) return rgb(151, 78, 64);
        if (state.is(Blocks.BLACK_CONCRETE)) return rgb(23, 25, 29);
        if (state.is(Blocks.WHITE_CONCRETE)) return rgb(207, 213, 214);
        if (state.is(Blocks.LIGHT_GRAY_CONCRETE)) return rgb(125, 125, 115);
        if (state.is(Blocks.GRAY_CONCRETE)) return rgb(55, 58, 62);

        /*
         * Unknown blocks still get a stable identity color, but keep the range
         * neutral so a single unsupported block cannot turn the entire feed
         * into an orange/brown slab.
         */
        int hash = BuiltInRegistries.BLOCK.getKey(state.getBlock()).hashCode();
        int base = 92 + Math.floorMod(hash, 54);
        int variationA = Math.floorMod(hash >>> 8, 29) - 14;
        int variationB = Math.floorMod(hash >>> 16, 29) - 14;
        return rgb(
                base,
                base + variationA,
                base + variationB
        );
    }

    private static int shade(int color, double distance) {
        double factor = Math.max(0.30, 1.0 - distance / (RANGE * 1.20));
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return rgb(r, g, b);
    }

    private static int rgb(int r, int g, int b) {
        return (Math.max(0, Math.min(255, r)) << 16)
                | (Math.max(0, Math.min(255, g)) << 8)
                | Math.max(0, Math.min(255, b));
    }
}
