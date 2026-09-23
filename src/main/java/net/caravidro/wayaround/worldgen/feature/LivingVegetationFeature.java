package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Adds a second, biome-aware vegetation layer on top of vanilla worldgen.
 *
 * The point is not "more trees everywhere". Dense biomes become genuinely
 * dense, open biomes remain readable, and river edges get overhanging canopy
 * without planting trunks in the water.
 */
public final class LivingVegetationFeature
        extends Feature<NoneFeatureConfiguration> {

    public LivingVegetationFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        String biome =
                level.getBiome(origin)
                        .unwrapKey()
                        .map(key -> key.location().getPath())
                        .orElse("");

        int attempts = densityFor(biome);

        if (attempts <= 0) {
            return false;
        }

        int placed = 0;

        for (int i = 0; i < attempts; i++) {
            int x =
                    origin.getX()
                    + random.nextInt(16);

            int z =
                    origin.getZ()
                    + random.nextInt(16);

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos trunkBase =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (tryPlaceTree(
                    level,
                    trunkBase,
                    biome,
                    random
            )) {
                placed++;
            }
        }

        return placed > 0;
    }

    private static int densityFor(String biome) {
        if (biome.contains("jungle")) {
            return 18;
        }

        if (biome.contains("dark_forest")) {
            return 15;
        }

        if (biome.contains("old_growth")) {
            return 13;
        }

        if (biome.contains("forest")) {
            return 11;
        }

        if (biome.contains("taiga")
                || biome.contains("grove")) {
            return 10;
        }

        if (biome.contains("river")) {
            return 7;
        }

        if (biome.contains("savanna")) {
            return 5;
        }

        if (biome.contains("meadow")) {
            return 3;
        }

        if (biome.contains("plains")) {
            return 2;
        }

        return 0;
    }

    private static boolean tryPlaceTree(
            WorldGenLevel level,
            BlockPos base,
            String biome,
            RandomSource random
    ) {
        BlockPos groundPos = base.below();
        BlockState ground = level.getBlockState(groundPos);

        if (!validGround(ground)) {
            return false;
        }

        if (!level.getFluidState(base).isEmpty()) {
            return false;
        }

        TreePalette palette =
                paletteFor(biome);

        int height =
                palette.minHeight
                + random.nextInt(
                        palette.maxHeight
                        - palette.minHeight
                        + 1
                );

        /*
         * A trunk must stay on land. Leaves are allowed to extend above water,
         * which creates the "forest reaching over the river" silhouette.
         */
        for (int dy = 0; dy <= height + 2; dy++) {
            BlockPos pos = base.above(dy);
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()
                    && state.getBlock() != palette.leaves
                    && !state.canBeReplaced()) {
                return false;
            }

            if (!state.getFluidState().isEmpty()) {
                return false;
            }
        }

        for (int dy = 0; dy < height; dy++) {
            level.setBlock(
                    base.above(dy),
                    palette.log.defaultBlockState(),
                    2
            );
        }

        BlockPos crown =
                base.above(height);

        placeCanopy(
                level,
                crown,
                palette.leaves,
                random,
                palette.wideCanopy
        );

        Direction riverDirection =
                findNearbyWater(
                        level,
                        base,
                        6
                );

        if (riverDirection != null
                && height >= 5
                && random.nextFloat() < 0.78F) {

            placeRiverBranch(
                    level,
                    base.above(height - 2),
                    riverDirection,
                    palette,
                    random
            );
        }

        return true;
    }

    private static void placeCanopy(
            WorldGenLevel level,
            BlockPos crown,
            Block leaves,
            RandomSource random,
            boolean wide
    ) {
        int baseRadius = wide ? 3 : 2;

        for (int dy = -2; dy <= 1; dy++) {
            int radius =
                    dy == 1
                            ? Math.max(1, baseRadius - 1)
                            : baseRadius;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double normalized =
                            (dx * dx + dz * dz)
                            /
                            (double) (radius * radius + 1);

                    if (normalized > 1.35) {
                        continue;
                    }

                    if (Math.abs(dx) == radius
                            && Math.abs(dz) == radius
                            && random.nextBoolean()) {
                        continue;
                    }

                    setLeafIfFree(
                            level,
                            crown.offset(dx, dy, dz),
                            leaves
                    );
                }
            }
        }

        setLeafIfFree(
                level,
                crown.above(2),
                leaves
        );
    }

    private static void placeRiverBranch(
            WorldGenLevel level,
            BlockPos branchStart,
            Direction direction,
            TreePalette palette,
            RandomSource random
    ) {
        int length =
                2
                + random.nextInt(2);

        BlockPos end = branchStart;

        for (int i = 1; i <= length; i++) {
            BlockPos pos =
                    branchStart.relative(
                            direction,
                            i
                    );

            /*
             * Never replace water. Branches live above it.
             */
            if (!level.getFluidState(pos).isEmpty()) {
                break;
            }

            BlockState state =
                    level.getBlockState(pos);

            if (!state.isAir()
                    && state.getBlock() != palette.leaves
                    && !state.canBeReplaced()) {
                break;
            }

            level.setBlock(
                    pos,
                    palette.log.defaultBlockState(),
                    2
            );

            end = pos;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5) {
                        continue;
                    }

                    setLeafIfFree(
                            level,
                            end.offset(dx, dy, dz),
                            palette.leaves
                    );
                }
            }
        }
    }

    private static Direction findNearbyWater(
            WorldGenLevel level,
            BlockPos base,
            int radius
    ) {
        Direction best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            for (int distance = 2;
                    distance <= radius;
                    distance++) {

                BlockPos probe =
                        base.relative(
                                direction,
                                distance
                        );

                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos sample =
                            probe.offset(
                                    0,
                                    dy,
                                    0
                            );

                    if (level.getFluidState(sample)
                            .is(FluidTags.WATER)) {

                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = direction;
                        }

                        break;
                    }
                }
            }
        }

        return best;
    }

    private static void setLeafIfFree(
            WorldGenLevel level,
            BlockPos pos,
            Block leaves
    ) {
        /*
         * Leaves may occupy AIR above a river, but the feature never replaces
         * the water itself.
         */
        if (!level.getFluidState(pos).isEmpty()) {
            return;
        }

        BlockState state =
                level.getBlockState(pos);

        if (state.isAir()
                || state.canBeReplaced()
                || state.getBlock() == leaves) {

            BlockState leafState =
                    leaves.defaultBlockState();

            if (leafState.hasProperty(LeavesBlock.DISTANCE)) {
                leafState =
                        leafState.setValue(
                                LeavesBlock.DISTANCE,
                                3
                        );
            }

            level.setBlock(
                    pos,
                    leafState,
                    2
            );
        }
    }

    private static boolean validGround(
            BlockState state
    ) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MYCELIUM);
    }

    private static TreePalette paletteFor(
            String biome
    ) {
        if (biome.contains("birch")) {
            return new TreePalette(
                    Blocks.BIRCH_LOG,
                    Blocks.BIRCH_LEAVES,
                    5,
                    7,
                    false
            );
        }

        if (biome.contains("taiga")
                || biome.contains("grove")) {
            return new TreePalette(
                    Blocks.SPRUCE_LOG,
                    Blocks.SPRUCE_LEAVES,
                    6,
                    9,
                    false
            );
        }

        if (biome.contains("jungle")) {
            return new TreePalette(
                    Blocks.JUNGLE_LOG,
                    Blocks.JUNGLE_LEAVES,
                    7,
                    11,
                    true
            );
        }

        if (biome.contains("savanna")) {
            return new TreePalette(
                    Blocks.ACACIA_LOG,
                    Blocks.ACACIA_LEAVES,
                    5,
                    7,
                    true
            );
        }

        return new TreePalette(
                Blocks.OAK_LOG,
                Blocks.OAK_LEAVES,
                5,
                8,
                true
        );
    }

    private record TreePalette(
            Block log,
            Block leaves,
            int minHeight,
            int maxHeight,
            boolean wideCanopy
    ) {
    }
}
