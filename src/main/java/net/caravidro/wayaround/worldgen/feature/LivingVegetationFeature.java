package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.ecology.AquaticFloorLifeBlock;
import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.ecology.VegetationPalette;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
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
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return false;
        }

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        String biome =
                level.getBiome(origin)
                        .unwrapKey()
                        .map(key -> key.location().getPath())
                        .orElse("");

        if (biome.contains("ocean")) {
            return placeOceanFloorLife(
                    level,
                    origin,
                    biome,
                    random
            );
        }

        int attempts =
                densityFor(
                        biome
                );

        int placed =
                placeGroundVegetation(
                        level,
                        origin,
                        biome,
                        random
                );

        if (attempts <= 0) {
            return placed > 0;
        }

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

    private static boolean placeOceanFloorLife(
            WorldGenLevel level,
            BlockPos origin,
            String biome,
            RandomSource random
    ) {
        int attempts =
                biome.contains("warm_ocean")
                        ? 42
                        : biome.contains("deep")
                                ? 27
                                : 34;

        int placed =
                0;

        for (int i = 0;
             i < attempts;
             i++) {
            int x =
                    origin.getX()
                            + random.nextInt(16);

            int z =
                    origin.getZ()
                            + random.nextInt(16);

            int y =
                    level.getHeight(
                            Heightmap.Types.OCEAN_FLOOR_WG,
                            x,
                            z
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            /*
             * Only occupy a genuinely empty water block.
             *
             * Checking FluidState alone was too permissive: coral fans,
             * seagrass, kelp and other waterlogged content also report WATER,
             * so the feature could overwrite a perfectly good vanilla reef.
             */
            if (!isEmptyOceanCell(
                    level,
                    pos
            )) {
                continue;
            }

            if (!validOceanFloor(
                    level.getBlockState(
                            pos.below()
                    )
            )) {
                continue;
            }

            float pick =
                    random.nextFloat();

            BlockState life;

            if (biome.contains("warm_ocean")
                    && pick < 0.24F) {
                life =
                        EcologyContent.SEA_SPONGE.get()
                                .defaultBlockState();

            } else if (pick < 0.44F) {
                life =
                        EcologyContent.SEA_CUCUMBER.get()
                                .defaultBlockState();

            } else if (pick < 0.69F) {
                life =
                        EcologyContent.SEA_LETTUCE.get()
                                .defaultBlockState();

            } else {
                life =
                        EcologyContent.SEAGRASS_TUFT.get()
                                .defaultBlockState();
            }

            life =
                    life.setValue(
                            AquaticFloorLifeBlock.WATERLOGGED,
                            true
                    );

            if (!life.canSurvive(
                    level,
                    pos
            )) {
                continue;
            }

            level.setBlock(
                    pos,
                    life,
                    2
            );

            placed++;

            if (random.nextFloat() < 0.34F) {
                int ox =
                        x
                                + random.nextInt(3)
                                - 1;

                int oz =
                        z
                                + random.nextInt(3)
                                - 1;

                int oy =
                        level.getHeight(
                                Heightmap.Types.OCEAN_FLOOR_WG,
                                ox,
                                oz
                        );

                BlockPos neighbor =
                        new BlockPos(
                                ox,
                                oy,
                                oz
                        );

                if (isEmptyOceanCell(
                        level,
                        neighbor
                )
                        && validOceanFloor(
                        level.getBlockState(
                                neighbor.below()
                        )
                )
                        && life.canSurvive(
                        level,
                        neighbor
                )) {
                    level.setBlock(
                            neighbor,
                            life,
                            2
                    );

                    placed++;
                }
            }
        }

        return placed > 0;
    }

    private static boolean validOceanFloor(
            BlockState state
    ) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.MUD)
                || state.is(Blocks.DIRT);
    }

    private static boolean isEmptyOceanCell(
            WorldGenLevel level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        /*
         * Exact WATER is intentional. A coral fan can contain water, but it is
         * not a water block and therefore survives untouched. The same rule
         * protects seagrass, kelp and other features from this second layer.
         */
        return state.is(
                Blocks.WATER
        )
                && level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        );
    }

    private static int densityFor(String biome) {
        if (biome.contains("cherry")) {
            return 28;
        }

        if (biome.contains("mangrove")
                || biome.contains("swamp")) {
            return 24;
        }

        if (biome.contains("jungle")) {
            return 52;
        }

        if (biome.contains("dark_forest")) {
            return 46;
        }

        if (biome.contains("old_growth")) {
            return 40;
        }

        if (biome.contains("forest")) {
            return 34;
        }

        if (biome.contains("taiga")
                || biome.contains("grove")) {
            return 29;
        }

        if (biome.contains("river")) {
            return 23;
        }

        if (biome.contains("savanna")) {
            return 14;
        }

        if (biome.contains("meadow")) {
            return 12;
        }

        if (biome.contains("plains")) {
            return 11;
        }

        return 0;
    }

    private static boolean tryPlaceTree(
            WorldGenLevel level,
            BlockPos base,
            String biome,
            RandomSource random
    ) {
        OrganicTreeGenerator.Result result =
                OrganicTreeGenerator.place(
                        level,
                        base,
                        biome,
                        random
                );

        if (!result.placed()) {
            return false;
        }

        placeUnderstory(
                level,
                base,
                biome,
                result.nearWater(),
                random
        );

        return true;
    }

    private static void placeUnderstory(
            WorldGenLevel level,
            BlockPos base,
            String biome,
            boolean nearRiver,
            RandomSource random
    ) {
        int attempts =
                12
                        + random.nextInt(15);

        for (int i = 0; i < attempts; i++) {
            BlockPos pos =
                    base.offset(
                            random.nextInt(9) - 4,
                            0,
                            random.nextInt(9) - 4
                    );

            while (pos.getY() > level.getMinBuildHeight()
                    && level.getBlockState(pos).isAir()) {
                pos = pos.below();
            }

            BlockPos above =
                    pos.above();

            if (!level.getBlockState(above).isAir()) {
                continue;
            }

            BlockState ground =
                    level.getBlockState(pos);

            if (!validGround(ground)) {
                continue;
            }

            boolean wet =
                    nearRiver
                            || ground.is(
                            Blocks.MUD
                    )
                            || ground.is(
                            Blocks.MOSS_BLOCK
                    );

            BlockState plant =
                    VegetationPalette.pick(
                            biome,
                            wet,
                            nearRiver,
                            random
                    );

            if (plant.canSurvive(level, above)) {
                level.setBlock(
                        above,
                        plant,
                        2
                );
            }
        }
    }

    private static int placeGroundVegetation(
            WorldGenLevel level,
            BlockPos origin,
            String biome,
            RandomSource random
    ) {
        int attempts =
                VegetationPalette.groundAttempts(
                        biome
                );

        if (attempts <= 0) {
            return 0;
        }

        int placed =
                0;

        for (int i = 0;
             i < attempts;
             i++) {

            int x =
                    origin.getX()
                            + random.nextInt(
                            16
                    );

            int z =
                    origin.getZ()
                            + random.nextInt(
                            16
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos above =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            BlockPos ground =
                    above.below();

            if (!level.ensureCanWrite(
                    above
            )
                    || !level.getBlockState(
                    above
            ).isAir()
                    || !validGround(
                    level.getBlockState(
                            ground
                    )
            )) {
                continue;
            }

            boolean nearWater =
                    nearWater(
                            level,
                            ground,
                            2
                    );

            BlockState groundState =
                    level.getBlockState(
                            ground
                    );

            boolean wet =
                    nearWater
                            || groundState.is(
                            Blocks.MUD
                    )
                            || groundState.is(
                            Blocks.MOSS_BLOCK
                    )
                            || groundState.is(
                            Blocks.ROOTED_DIRT
                    );

            BlockState plant =
                    VegetationPalette.pick(
                            biome,
                            wet,
                            nearWater,
                            random
                    );

            if (!plant.canSurvive(
                    level,
                    above
            )) {
                continue;
            }

            level.setBlock(
                    above,
                    plant,
                    2
            );

            placed++;

            /*
             * Ferns, reeds, clover and mushroom patches look substantially
             * better as irregular colonies than isolated single blocks.
             */
            if (VegetationPalette.clumps(
                    plant
            )
                    && random.nextFloat()
                    < 0.42F) {

                int satellites =
                        1
                                + random.nextInt(
                                3
                        );

                for (int n = 0;
                     n < satellites;
                     n++) {

                    int ox =
                            x
                                    + random.nextInt(
                                    5
                            )
                                    - 2;

                    int oz =
                            z
                                    + random.nextInt(
                                    5
                            )
                                    - 2;

                    int oy =
                            level.getHeight(
                                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    ox,
                                    oz
                            );

                    BlockPos neighbor =
                            new BlockPos(
                                    ox,
                                    oy,
                                    oz
                            );

                    BlockPos neighborGround =
                            neighbor.below();

                    if (!level.ensureCanWrite(
                            neighbor
                    )
                            || !level.getBlockState(
                            neighbor
                    ).isAir()
                            || !validGround(
                            level.getBlockState(
                                    neighborGround
                            )
                    )
                            || !plant.canSurvive(
                            level,
                            neighbor
                    )) {
                        continue;
                    }

                    level.setBlock(
                            neighbor,
                            plant,
                            2
                    );

                    placed++;
                }
            }
        }

        return placed;
    }

    private static boolean nearWater(
            WorldGenLevel level,
            BlockPos ground,
            int radius
    ) {
        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            for (int distance = 1;
                 distance <= radius;
                 distance++) {

                BlockPos probe =
                        ground.relative(
                                direction,
                                distance
                        );

                if (level.getFluidState(
                        probe
                ).is(
                        FluidTags.WATER
                )
                        || level.getFluidState(
                        probe.above()
                ).is(
                        FluidTags.WATER
                )) {
                    return true;
                }
            }
        }

        return false;
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

}
