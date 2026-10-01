package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Rebuilds vanilla deep-ocean terrain as a broad abyssal basin.
 *
 * The old implementation dropped each deep-ocean chunk to roughly the same
 * absolute Y, which could leave a biome-border wall. This version derives the
 * abyss blend from the vanilla depth of each local column, so the transition is
 * smooth without ever reading a neighbouring chunk that may not exist yet.
 */
public final class DeepOceanTrenchFeature extends Feature<NoneFeatureConfiguration> {
    public DeepOceanTrenchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int baseX = origin.getX() & ~15;
        int baseZ = origin.getZ() & ~15;
        int seaLevel = level.getSeaLevel();
        int minFloor = level.getMinBuildHeight() + 4;
        int changed = 0;
        double chunkInterior = 0.0;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                int oldFloor = level.getHeight(
                        Heightmap.Types.OCEAN_FLOOR_WG,
                        x,
                        z
                ) - 1;

                if (oldFloor >= seaLevel - 7 || !isDeepOcean(level, x, z, seaLevel - 8)) {
                    continue;
                }

                double interior = interiorFactor(oldFloor, seaLevel, x, z);
                chunkInterior = Math.max(chunkInterior, interior);

                double waveA = Math.sin(x * 0.021 + z * 0.013);
                double waveB = Math.sin(x * 0.008 - z * 0.019);
                double waveC = Math.sin((x + z) * 0.0047);

                int abyssFloor = Mth.clamp(
                        -58
                                + (int) Math.round(
                                waveA * 2.8
                                        + waveB * 2.4
                                        + waveC * 1.8
                        ),
                        minFloor,
                        -50
                );

                // Smoothstep removes the infamous vertical biome-border wall.
                double blend = interior * interior * (3.0 - 2.0 * interior);
                int targetFloor = Mth.clamp(
                        (int) Math.round(Mth.lerp(blend, oldFloor, abyssFloor)),
                        minFloor,
                        oldFloor
                );

                if (oldFloor <= targetFloor + 2) {
                    continue;
                }

                BlockPos.MutableBlockPos cursor =
                        new BlockPos.MutableBlockPos(x, seaLevel, z);

                for (int y = seaLevel; y > targetFloor; y--) {
                    cursor.setY(y);
                    BlockState old = level.getBlockState(cursor);

                    if (old.is(Blocks.BEDROCK) || level.getBlockEntity(cursor) != null) {
                        continue;
                    }

                    if (!old.is(Blocks.WATER)) {
                        level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 2);
                        changed++;
                    }
                }

                cursor.setY(targetFloor);
                BlockState floor =
                        ((x * 31 + z * 17) & 7) == 0
                                ? Blocks.GRAVEL.defaultBlockState()
                                : ((x * 13 + z * 37) & 15) == 0
                                ? Blocks.TUFF.defaultBlockState()
                                : Blocks.DEEPSLATE.defaultBlockState();

                level.setBlock(cursor, floor, 2);

                for (int depth = 1; depth <= 3; depth++) {
                    cursor.setY(targetFloor - depth);
                    if (!level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        level.setBlock(cursor, Blocks.DEEPSLATE.defaultBlockState(), 2);
                    }
                }
            }
        }

        /*
         * Wrecks are intentionally common in the abyss: roughly one proper
         * ruined hull per three strongly-deepened chunks, plus smaller debris.
         * They are procedural remnants rather than full vanilla structures,
         * so they do not force neighbouring chunks to load.
         */
        if (changed > 0 && chunkInterior > 0.52) {
            if (random.nextFloat() < 0.34F) {
                placeWreck(level, baseX, baseZ, seaLevel, random);
            } else if (random.nextFloat() < 0.30F) {
                placeDebris(level, baseX, baseZ, seaLevel, random);
            }
        }

        return changed > 0;
    }

    private static double interiorFactor(
            int oldFloor,
            int seaLevel,
            int x,
            int z
    ) {
        /*
         * Worldgen features are not allowed to assume neighbouring chunks are
         * available. The previous implementation sampled biomes up to 16 blocks
         * away and could crash with "Requested chunk unavailable during world
         * generation".
         *
         * Vanilla ocean depth already contains a very useful border signal:
         * coast/edge columns are shallower while true deep-ocean interiors are
         * much lower. Convert that local depth into a smooth 0..1 abyss blend.
         */
        double vanillaDepth =
                Math.max(
                        0.0,
                        seaLevel - oldFloor
                );

        double depthBlend =
                Mth.clamp(
                        (vanillaDepth - 10.0) / 30.0,
                        0.0,
                        1.0
                );

        /*
         * A tiny continuous low-frequency variation prevents the transition
         * from looking mathematically flat while remaining deterministic across
         * chunk borders. No chunk/biome lookup is involved here.
         */
        double variation =
                Math.sin(
                        x * 0.031
                                + z * 0.017
                ) * 0.055
                        + Math.sin(
                        x * 0.011
                                - z * 0.027
                ) * 0.035;

        return Mth.clamp(
                0.08
                        + depthBlend * 0.92
                        + variation,
                0.06,
                1.0
        );
    }

    private static boolean isDeepOcean(
            WorldGenLevel level,
            int x,
            int z,
            int y
    ) {
        return level.getBiome(new BlockPos(x, y, z))
                .unwrapKey()
                .map(key -> {
                    String path = key.location().getPath();
                    return path.equals("deep_ocean")
                            || path.equals("deep_cold_ocean")
                            || path.equals("deep_frozen_ocean")
                            || path.equals("deep_lukewarm_ocean");
                })
                .orElse(false);
    }

    private static int findFloor(
            WorldGenLevel level,
            int x,
            int z,
            int seaLevel
    ) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(x, seaLevel - 1, z);

        for (int y = seaLevel - 1;
             y > level.getMinBuildHeight() + 4;
             y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);

            if (!state.isAir() && !state.is(Blocks.WATER)) {
                return y;
            }
        }

        return level.getMinBuildHeight() + 5;
    }

    private static void placeWreck(
            WorldGenLevel level,
            int baseX,
            int baseZ,
            int seaLevel,
            RandomSource random
    ) {
        int cx = baseX + 5 + random.nextInt(6);
        int cz = baseZ + 5 + random.nextInt(6);
        int floor = findFloor(level, cx, cz, seaLevel);
        boolean alongX = random.nextBoolean();

        BlockState hull = random.nextBoolean()
                ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
                : Blocks.SPRUCE_PLANKS.defaultBlockState();

        for (int along = -3; along <= 3; along++) {
            for (int across = -2; across <= 2; across++) {
                if (Math.abs(across) == 2 && Math.abs(along) > 2) continue;
                if (random.nextFloat() < 0.23F) continue;

                int x = cx + (alongX ? along : across);
                int z = cz + (alongX ? across : along);
                int y = floor + 1 + (Math.abs(across) == 2 ? 1 : 0);

                setIfWater(level, new BlockPos(x, y, z), hull);
            }
        }

        BlockPos mast = new BlockPos(cx, floor + 2, cz);
        for (int y = 0; y < 5; y++) {
            if (y == 3 && random.nextBoolean()) continue;
            setIfWater(
                    level,
                    mast.above(y),
                    Blocks.SPRUCE_LOG.defaultBlockState()
            );
        }

        for (int side : new int[]{-2, 2}) {
            BlockPos rail = alongX
                    ? new BlockPos(cx, floor + 3, cz + side)
                    : new BlockPos(cx + side, floor + 3, cz);
            setIfWater(level, rail, Blocks.OAK_FENCE.defaultBlockState());
        }

        if (random.nextBoolean()) {
            BlockPos chain = alongX
                    ? new BlockPos(cx + 2, floor + 2, cz)
                    : new BlockPos(cx, floor + 2, cz + 2);
            setIfWater(level, chain, Blocks.CHAIN.defaultBlockState());
            setIfWater(level, chain.below(), Blocks.CHAIN.defaultBlockState());
        }

        setIfWater(
                level,
                new BlockPos(cx + (alongX ? -3 : 0), floor + 1, cz + (alongX ? 0 : -3)),
                Blocks.MOSSY_COBBLESTONE.defaultBlockState()
        );
    }

    private static void placeDebris(
            WorldGenLevel level,
            int baseX,
            int baseZ,
            int seaLevel,
            RandomSource random
    ) {
        int cx = baseX + 4 + random.nextInt(8);
        int cz = baseZ + 4 + random.nextInt(8);
        int floor = findFloor(level, cx, cz, seaLevel);

        for (int i = 0; i < 7 + random.nextInt(8); i++) {
            int x = cx + random.nextInt(9) - 4;
            int z = cz + random.nextInt(9) - 4;
            BlockState state = switch (random.nextInt(4)) {
                case 0 -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                case 1 -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
                case 2 -> Blocks.CHAIN.defaultBlockState();
                default -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            };
            setIfWater(level, new BlockPos(x, floor + 1, z), state);
        }
    }

    private static void setIfWater(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (pos.getY() < level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight()
                || level.getBlockEntity(pos) != null) {
            return;
        }

        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.WATER) || existing.canBeReplaced()) {
            level.setBlock(pos, state, 2);
        }
    }
}
