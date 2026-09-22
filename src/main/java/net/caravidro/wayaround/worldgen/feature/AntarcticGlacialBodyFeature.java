package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.terrain.AntarcticTerrain;

import net.minecraft.core.BlockPos;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class AntarcticGlacialBodyFeature
        extends Feature<NoneFeatureConfiguration> {

    public AntarcticGlacialBodyFeature(
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

        BlockPos origin =
                context.origin();

        boolean changed =
                false;

        /*
         * Uma execução por chunk.
         */
        int startX =
                origin.getX();

        int startZ =
                origin.getZ();

        for (
                int localX = 0;
                localX < 16;
                localX++
        ) {

            for (
                    int localZ = 0;
                    localZ < 16;
                    localZ++
            ) {

                int x =
                        startX
                                +
                                localX;

                int z =
                        startZ
                                +
                                localZ;

                int top =
                        level.getHeight(
                                Heightmap.Types
                                        .WORLD_SURFACE_WG,

                                x,
                                z
                        );

                BlockPos surfacePos =
                        new BlockPos(
                                x,
                                top - 1,
                                z
                        );

                if (
                        !level.getBiome(
                                surfacePos
                        ).is(
                                WayAroundBiomes
                                        .ANTARCTIC_ICE_SHEET
                        )
                ) {
                    continue;
                }

                int thickness =
                        AntarcticTerrain
                                .getGlacierThickness(
                                        x,
                                        z
                                );

                /*
                 * 2–5 blocos de neve compactada
                 * antes do gelo.
                 */
                double snowNoise =
                        AntarcticTerrain
                                .getSurfaceSnowNoise(
                                        x,
                                        z
                                );

                int snowDepth =
                        2;

                if (
                        snowNoise > -0.1
                ) {
                    snowDepth++;
                }

                if (
                        snowNoise > 0.25
                ) {
                    snowDepth++;
                }

                if (
                        snowNoise > 0.55
                ) {
                    snowDepth++;
                }

                int glacierBottom =
                        Math.max(
                                level.getMinBuildHeight(),
                                top - thickness
                        );

                for (
                        int y = top - 1;
                        y >= glacierBottom;
                        y--
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    /*
                     * NÃO preenche cavernas.
                     *
                     * Assim nossas cavernas continuam
                     * existindo, mas suas paredes viram gelo.
                     */
                    if (
                            state.isAir()
                    ) {
                        continue;
                    }

                    /*
                     * Não converte líquidos em gelo aqui.
                     *
                     * Nada de lago virar uma bolha
                     * bizarra automaticamente.
                     */
                    if (
                            !state
                                    .getFluidState()
                                    .isEmpty()
                    ) {
                        continue;
                    }

                    if (
                            !isNaturalTerrain(
                                    state
                            )
                    ) {
                        continue;
                    }

                    int depth =
                            top - y;

                    /*
                     * =========================================
                     * SNOW BLOCK
                     * =========================================
                     */
                    if (
                            depth <= snowDepth
                    ) {

                        level.setBlock(
                                pos,

                                Blocks
                                        .SNOW_BLOCK
                                        .defaultBlockState(),

                                2
                        );

                        changed = true;

                        continue;
                    }

                    /*
                     * =========================================
                     * TRANSIÇÃO ROCHA <-> GELO
                     * =========================================
                     */

                    int distanceToBottom =
                            y
                                    -
                                    glacierBottom;

                    if (
                            distanceToBottom <= 4
                    ) {

                        long hash =
                                hash(
                                        x,
                                        y,
                                        z
                                );

                        /*
                         * Mistura irregular.
                         */
                        if (
                                Math.floorMod(
                                        hash,
                                        100
                                )
                                        <
                                        52
                        ) {

                            level.setBlock(
                                    pos,

                                    Blocks
                                            .PACKED_ICE
                                            .defaultBlockState(),

                                    2
                            );
                        }

                        /*
                         * Senão mantém stone/deepslate.
                         */

                        changed = true;

                        continue;
                    }

                    /*
                     * =========================================
                     * CORPO DA GELEIRA
                     * =========================================
                     */

                    long hash =
                            hash(
                                    x,
                                    y,
                                    z
                            );

                    /*
                     * Blue Ice bem raro,
                     * como faixas comprimidas.
                     */
                    boolean blueIce =
                            Math.floorMod(
                                    hash,
                                    173
                            )
                                    ==
                                    0;

                    level.setBlock(
                            pos,

                            blueIce
                                    ?
                                    Blocks
                                            .BLUE_ICE
                                            .defaultBlockState()
                                    :
                                    Blocks
                                            .PACKED_ICE
                                            .defaultBlockState(),

                            2
                    );

                    changed = true;
                }
            }
        }

        return changed;
    }
private static boolean isNaturalTerrain(
        BlockState state
) {

    return state.is(
            Blocks.STONE
    )

            ||
            state.is(
                    Blocks.DEEPSLATE
            )

            ||
            state.is(
                    Blocks.GRANITE
            )

            ||
            state.is(
                    Blocks.DIORITE
            )

            ||
            state.is(
                    Blocks.ANDESITE
            )

            ||
            state.is(
                    Blocks.TUFF
            )

            /*
             * =================================================
             * SURFACE VANILLA
             * =================================================
             *
             * Se o Minecraft pintar a superfície antes
             * da nossa geleira, nós também substituímos.
             */

            ||
            state.is(
                    Blocks.GRASS_BLOCK
            )

            ||
            state.is(
                    Blocks.DIRT
            )

            ||
            state.is(
                    Blocks.COARSE_DIRT
            )

            ||
            state.is(
                    Blocks.ROOTED_DIRT
            )

            ||
            state.is(
                    Blocks.GRAVEL
            )

            ||
            state.is(
                    Blocks.SAND
            )

            /*
             * =================================================
             * GELO
             * =================================================
             */

            ||
            state.is(
                    Blocks.PACKED_ICE
            )

            ||
            state.is(
                    Blocks.BLUE_ICE
            )

            ||
            state.is(
                    Blocks.SNOW_BLOCK
            );
}
    private static long hash(
            int x,
            int y,
            int z
    ) {

        long value =
                71238123L;

        value ^=
                (long) x
                        *
                        341873128712L;

        value ^=
                (long) y
                        *
                        42317861L;

        value ^=
                (long) z
                        *
                        132897987541L;

        value ^=
                value >>> 33;

        value *=
                0xff51afd7ed558ccdL;

        value ^=
                value >>> 33;

        return value;
    }
}