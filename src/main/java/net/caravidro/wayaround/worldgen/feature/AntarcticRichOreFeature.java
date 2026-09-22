package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;

import net.minecraft.core.BlockPos;

import net.minecraft.util.RandomSource;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class AntarcticRichOreFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final OreSpec[] ORES = {

            /*
             * MUITO ferro.
             */
            new OreSpec(
                    Blocks.IRON_ORE,
                    Blocks.DEEPSLATE_IRON_ORE,

                    34,
                    8,
                    14,

                    -56,
                    120
            ),

            /*
             * Carvão abundante.
             */
            new OreSpec(
                    Blocks.COAL_ORE,
                    Blocks.DEEPSLATE_COAL_ORE,

                    22,
                    8,
                    15,

                    0,
                    160
            ),

            /*
             * Cobre.
             */
            new OreSpec(
                    Blocks.COPPER_ORE,
                    Blocks.DEEPSLATE_COPPER_ORE,

                    20,
                    7,
                    14,

                    -16,
                    112
            ),

            /*
             * Ouro.
             */
            new OreSpec(
                    Blocks.GOLD_ORE,
                    Blocks.DEEPSLATE_GOLD_ORE,

                    14,
                    5,
                    10,

                    -64,
                    40
            ),

            /*
             * Redstone.
             */
            new OreSpec(
                    Blocks.REDSTONE_ORE,
                    Blocks.DEEPSLATE_REDSTONE_ORE,

                    18,
                    5,
                    11,

                    -64,
                    24
            ),

            /*
             * Lápis.
             */
            new OreSpec(
                    Blocks.LAPIS_ORE,
                    Blocks.DEEPSLATE_LAPIS_ORE,

                    11,
                    4,
                    9,

                    -64,
                    72
            ),

            /*
             * Diamante.
             *
             * SIM, MUITO mais que vanilla.
             */
            new OreSpec(
                    Blocks.DIAMOND_ORE,
                    Blocks.DEEPSLATE_DIAMOND_ORE,

                    10,
                    3,
                    7,

                    -64,
                    20
            ),

            /*
             * Esmeralda.
             */
            new OreSpec(
                    Blocks.EMERALD_ORE,
                    Blocks.DEEPSLATE_EMERALD_ORE,

                    7,
                    2,
                    6,

                    -16,
                    120
            )
    };

    public AntarcticRichOreFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {

        WorldGenLevel level =
                context.level();

        RandomSource random =
                context.random();

        BlockPos origin =
                context.origin();

        /*
         * Confirma que estamos na Antártica.
         */
        BlockPos biomeCheck =
                origin.offset(
                        8,
                        64,
                        8
                );

        if (
                !level.getBiome(
                        biomeCheck
                ).is(
                        WayAroundBiomes
                                .ANTARCTIC_ICE_SHEET
                )
        ) {
            return false;
        }

        boolean placed =
                false;

        for (
                OreSpec spec
                :
                ORES
        ) {

            for (
                    int attempt = 0;
                    attempt < spec.attempts;
                    attempt++
            ) {

                int x =
                        origin.getX()
                                +
                                random.nextInt(
                                        16
                                );

                int z =
                        origin.getZ()
                                +
                                random.nextInt(
                                        16
                                );

                int minY =
                        Math.max(
                                level.getMinBuildHeight(),
                                spec.minY
                        );

                int maxY =
                        Math.min(
                                level.getMaxBuildHeight() - 1,
                                spec.maxY
                        );

                if (
                        maxY <= minY
                ) {
                    continue;
                }

                int y =
                        minY
                                +
                                random.nextInt(
                                        maxY
                                                -
                                                minY
                                                +
                                                1
                                );

                int veinSize =
                        spec.minSize

                                +
                                random.nextInt(
                                        spec.maxSize
                                                -
                                                spec.minSize
                                                +
                                                1
                                );

                if (
                        placeVein(
                                level,
                                random,

                                new BlockPos(
                                        x,
                                        y,
                                        z
                                ),

                                spec,

                                veinSize
                        )
                ) {

                    placed = true;
                }
            }
        }

        return placed;
    }

    private static boolean placeVein(
            WorldGenLevel level,
            RandomSource random,

            BlockPos start,

            OreSpec spec,

            int size
    ) {

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(
                        start.getX(),
                        start.getY(),
                        start.getZ()
                );

        boolean placed =
                false;

        for (
                int i = 0;
                i < size;
                i++
        ) {

            BlockState current =
                    level.getBlockState(
                            cursor
                    );

            BlockState ore =
                    getOreState(
                            current,
                            spec
                    );

            if (
                    ore != null
            ) {

                level.setBlock(
                        cursor,
                        ore,
                        2
                );

                placed = true;
            }

            /*
             * Caminhada aleatória.
             *
             * Forma veios menos esféricos.
             */
            cursor.move(
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1
            );
        }

        return placed;
    }

    private static BlockState getOreState(
            BlockState current,
            OreSpec spec
    ) {

        /*
         * Rochas profundas.
         */
        if (
                current.is(
                        Blocks.DEEPSLATE
                )

                        ||
                        current.is(
                                Blocks.TUFF
                        )
        ) {

            return spec
                    .deepOre
                    .defaultBlockState();
        }

        /*
         * Rochas normais.
         */
        if (
                current.is(
                        Blocks.STONE
                )

                        ||
                        current.is(
                                Blocks.GRANITE
                        )

                        ||
                        current.is(
                                Blocks.DIORITE
                        )

                        ||
                        current.is(
                                Blocks.ANDESITE
                        )
        ) {

            return spec
                    .normalOre
                    .defaultBlockState();
        }

        /*
         * Não coloca minério dentro
         * da geleira.
         */
        return null;
    }

    private static final class OreSpec {

        private final Block normalOre;
        private final Block deepOre;

        private final int attempts;

        private final int minSize;
        private final int maxSize;

        private final int minY;
        private final int maxY;

        private OreSpec(
                Block normalOre,
                Block deepOre,

                int attempts,

                int minSize,
                int maxSize,

                int minY,
                int maxY
        ) {

            this.normalOre =
                    normalOre;

            this.deepOre =
                    deepOre;

            this.attempts =
                    attempts;

            this.minSize =
                    minSize;

            this.maxSize =
                    maxSize;

            this.minY =
                    minY;

            this.maxY =
                    maxY;
        }
    }
}