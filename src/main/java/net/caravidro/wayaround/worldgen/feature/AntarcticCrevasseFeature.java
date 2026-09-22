package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.terrain.AntarcticTerrain;

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


public final class AntarcticCrevasseFeature
        extends Feature<NoneFeatureConfiguration> {

    public AntarcticCrevasseFeature(
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
         * =====================================================
         * CHANCE
         * =====================================================
         *
         * ~38% dos chunks antárticos tentam
         * gerar uma crevasse.
         */

        if (
                random.nextFloat()
                        >
                0.38F
        ) {

            return false;
        }


        /*
         * =====================================================
         * CHUNK CENTRAL
         * =====================================================
         *
         * O Feature recebe uma região segura de
         * 3x3 chunks.
         *
         * Nunca devemos escrever fora de:
         *
         * centerChunk - 1
         *
         * até
         *
         * centerChunk + 1
         */

        int centerChunkX =
                origin.getX() >> 4;

        int centerChunkZ =
                origin.getZ() >> 4;


        int chunkMinX =
                centerChunkX << 4;

        int chunkMinZ =
                centerChunkZ << 4;


        /*
         * Começa quase exatamente no centro.
         *
         * Isso dá espaço para a crevasse crescer
         * nas DUAS direções sem sair loucamente
         * da região de geração.
         */

        int startX =
                chunkMinX + 8;

        int startZ =
                chunkMinZ + 8;


        /*
         * =====================================================
         * ANTÁRTIDA
         * =====================================================
         *
         * Não precisamos consultar biome registry
         * durante worldgen.
         *
         * AntarcticField já é a fonte geográfica.
         */

        if (
                !AntarcticField.isAntarctic(
                        startX,
                        startZ
                )
        ) {

            return false;
        }


        /*
         * =====================================================
         * DIREÇÃO
         * =====================================================
         */

        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        double dirX =
                Math.cos(
                        angle
                );

        double dirZ =
                Math.sin(
                        angle
                );


        /*
         * Vetor perpendicular.
         *
         * Usado para largura e serpenteamento.
         */

        double sideX =
                -dirZ;

        double sideZ =
                dirX;


        /*
         * =====================================================
         * COMPRIMENTO
         * =====================================================
         *
         * Antigo:
         *
         * saía de um ponto e andava até 47 blocos
         * para UM lado.
         *
         * Agora:
         *
         * a fenda cresce para OS DOIS lados.
         *
         * Comprimento total:
         *
         * ~24 até ~36 blocos.
         */

        int length =
                50
                        +
                        random.nextInt(
                                13
                        );


        int halfLength =
                length / 2;


        double phase =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        boolean carved =
                false;


        /*
         * =====================================================
         * CAMINHO
         * =====================================================
         */

        for (
                int step = -halfLength;
                step <= halfLength;
                step++
        ) {

            /*
             * Faz a fissura serpentear.
             */

            double wobble =
                    Math.sin(

                            (
                                    step
                                            +
                                    halfLength
                            )
                                    *
                                    0.31

                                    +

                            phase

                    )
                            *
                            2.4;


            double centerX =
                    startX

                            +

                    dirX
                            *
                            step

                            +

                    sideX
                            *
                            wobble;


            double centerZ =
                    startZ

                            +

                    dirZ
                            *
                            step

                            +

                    sideZ
                            *
                            wobble;


            /*
             * =================================================
             * FORMATO
             * =================================================
             *
             * início:
             *
             *   |
             *
             * centro:
             *
             *  |||||
             *
             * fim:
             *
             *   |
             */

            double progress =
                    (
                            step
                                    +
                            halfLength
                    )

                            /

                    (double) (
                            halfLength
                                    *
                            2
                    );


            progress =
                    Mth.clamp(
                            progress,
                            0.0,
                            1.0
                    );


            double shape =
                    Math.sin(
                            progress
                                    *
                            Math.PI
                    );


            int width =
                    1
                            +
                            (
                                    shape > 0.55

                                            ?

                                    1

                                            :

                                    0
                            );


            /*
             * Algumas partes ficam mais largas.
             */

            if (
                    random.nextFloat()
                            <
                    0.10F
            ) {

                width++;
            }


            width =
                    Mth.clamp(
                            width,
                            1,
                            3
                    );


            /*
             * =================================================
             * LARGURA
             * =================================================
             */

            for (
                    int lateral = -width;
                    lateral <= width;
                    lateral++
            ) {

                int x =
                        Mth.floor(

                                centerX

                                        +

                                sideX
                                        *
                                        lateral

                        );


                int z =
                        Mth.floor(

                                centerZ

                                        +

                                sideZ
                                        *
                                        lateral

                        );


                /*
                 * =================================================
                 * TRAVA MAIS IMPORTANTE DO ARQUIVO
                 * =================================================
                 *
                 * NUNCA sequer fazemos getHeight()
                 * fora dos 3x3 chunks.
                 *
                 * Então não existe:
                 *
                 * setBlock far chunk
                 *
                 * getBlockState far chunk
                 *
                 * decoração far chunk
                 */

                if (
                        !isInsideSafeArea(

                                x,
                                z,

                                centerChunkX,
                                centerChunkZ

                        )
                ) {

                    continue;
                }


                /*
                 * =================================================
                 * SUPERFÍCIE
                 * =================================================
                 */

                int top =
                        level.getHeight(

                                Heightmap.Types
                                        .WORLD_SURFACE_WG,

                                x,
                                z

                        );


                /*
                 * =================================================
                 * ESPESSURA DO GLACIAL
                 * =================================================
                 */

                int glacierThickness =
                        AntarcticTerrain
                                .getGlacierThickness(
                                        x,
                                        z
                                );


                /*
                 * Nunca queremos atravessar
                 * completamente a geleira e abrir
                 * um buraco monstruoso até pedra.
                 */

                int maxDepth =
                        Math.min(

                                glacierThickness - 5,

                                38

                        );


                if (
                        maxDepth < 8
                ) {

                    continue;
                }


                /*
                 * =================================================
                 * PROFUNDIDADE
                 * =================================================
                 */

                int depth =
                        10

                                +

                        random.nextInt(

                                Math.max(

                                        1,

                                        maxDepth - 9

                                )

                        );


                /*
                 * Laterais menos profundas.
                 *
                 * Corte aproximadamente:
                 *
                 *        \   /
                 *         \ /
                 *          |
                 *          |
                 */

                depth -=
                        Math.abs(
                                lateral
                        )
                                *
                                2;


                depth =
                        Math.max(
                                6,
                                depth
                        );


                /*
                 * =================================================
                 * ESCAVA
                 * =================================================
                 */

                for (
                        int d = 0;
                        d < depth;
                        d++
                ) {

                    int y =
                            top
                                    -
                                    1
                                    -
                                    d;


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
                     * Só escava material glacial.
                     */

                    if (
                            !isGlacialBlock(
                                    state
                            )
                    ) {

                        /*
                         * Rocha = acabou.
                         */

                        if (
                                state.is(
                                        Blocks.STONE
                                )

                                        ||

                                state.is(
                                        Blocks.DEEPSLATE
                                )
                        ) {

                            break;
                        }


                        /*
                         * Air/caverna/etc:
                         *
                         * pula mas não necessariamente
                         * encerra a coluna.
                         */

                        continue;
                    }


                    /*
                     * =================================================
                     * ABRE A FENDA
                     * =================================================
                     */

                    level.setBlock(

                            pos,

                            Blocks
                                    .CAVE_AIR
                                    .defaultBlockState(),

                            2

                    );


                    /*
                     * =================================================
                     * BLUE ICE NAS PAREDES
                     * =================================================
                     */

                    decorateWall(

                            level,

                            pos.offset(
                                    1,
                                    0,
                                    0
                            ),

                            random,

                            centerChunkX,
                            centerChunkZ

                    );


                    decorateWall(

                            level,

                            pos.offset(
                                    -1,
                                    0,
                                    0
                            ),

                            random,

                            centerChunkX,
                            centerChunkZ

                    );


                    decorateWall(

                            level,

                            pos.offset(
                                    0,
                                    0,
                                    1
                            ),

                            random,

                            centerChunkX,
                            centerChunkZ

                    );


                    decorateWall(

                            level,

                            pos.offset(
                                    0,
                                    0,
                                    -1
                            ),

                            random,

                            centerChunkX,
                            centerChunkZ

                    );


                    carved =
                            true;
                }
            }
        }


        return carved;
    }


    /*
     * =========================================================
     * SAFE GENERATION AREA
     * =========================================================
     *
     * Feature worldgen:
     *
     *   [ ][ ][ ]
     *   [ ][X][ ]
     *   [ ][ ][ ]
     *
     * X = chunk central.
     *
     * Só mexemos nesses nove chunks.
     */

    private static boolean isInsideSafeArea(

            int blockX,
            int blockZ,

            int centerChunkX,
            int centerChunkZ

    ) {

        int chunkX =
                blockX >> 4;

        int chunkZ =
                blockZ >> 4;


        return

                Math.abs(
                        chunkX
                                -
                        centerChunkX
                )
                        <=
                        1

                        &&

                Math.abs(
                        chunkZ
                                -
                        centerChunkZ
                )
                        <=
                        1;
    }


    /*
     * =========================================================
     * DECORA PAREDE
     * =========================================================
     */

    private static void decorateWall(

            WorldGenLevel level,

            BlockPos pos,

            RandomSource random,

            int centerChunkX,
            int centerChunkZ

    ) {

        /*
         * ESSENCIAL:
         *
         * o bloco vizinho também precisa permanecer
         * dentro da região segura.
         */

        if (
                !isInsideSafeArea(

                        pos.getX(),
                        pos.getZ(),

                        centerChunkX,
                        centerChunkZ

                )
        ) {

            return;
        }


        BlockState state =
                level.getBlockState(
                        pos
                );


        if (
                state.is(
                        Blocks.PACKED_ICE
                )

                        &&

                random.nextFloat()
                        <
                0.16F
        ) {

            level.setBlock(

                    pos,

                    Blocks
                            .BLUE_ICE
                            .defaultBlockState(),

                    2

            );
        }
    }


    /*
     * =========================================================
     * BLOCOS ESCAVÁVEIS
     * =========================================================
     */

    private static boolean isGlacialBlock(
            BlockState state
    ) {

        return

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
}