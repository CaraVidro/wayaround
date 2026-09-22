package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public final class AntarcticIcebergFeature
        extends Feature<NoneFeatureConfiguration> {

    /*
     * =========================================================
     * FREQUÊNCIA
     * =========================================================
     *
     * Longe da Antártida:
     * poucos icebergs.
     *
     * Perto da costa:
     * mais icebergs.
     */

    private static final float FAR_SPAWN_CHANCE =
            0.10F;

    private static final float COAST_SPAWN_CHANCE =
            0.90F;


    public AntarcticIcebergFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {

        super(codec);
    }

private boolean isIcebergWater(
        WorldGenLevel level,

        int x,
        int waterY,
        int z,

        boolean allowCoastalAntarctica
) {

    /*
     * MUITO IMPORTANTE:
     *
     * Durante worldgen NÃO podemos consultar
     * chunks fora do WorldGenRegion.
     */
    if (
            !canWriteColumn(
                    level,
                    x,
                    z
            )
    ) {

        return false;
    }


    BlockPos pos =
            new BlockPos(
                    x,
                    waterY,
                    z
            );


    if (
            !level.getFluidState(
                    pos
            ).is(
                    FluidTags.WATER
            )
    ) {

        return false;
    }


    // Read the stored biome without BiomeManager sampling neighboring chunks.
    var biome = level.getChunk(x >> 4, z >> 4)
            .getNoiseBiome(x >> 2, waterY >> 2, z >> 2);

    if (
            biome.is(
                    WayAroundBiomes.SOUTHERN_OCEAN
            )
    ) {

        return true;
    }


    if (
            allowCoastalAntarctica

                    &&

                    biome.is(
                            WayAroundBiomes.ANTARCTIC_ICE_SHEET
                    )

                    &&

                    AntarcticField.sample(
                            x,
                            z
                    ) < 0.62
    ) {

        return true;
    }


    return false;
}
    /*
     * =========================================================
     * PLACE
     * =========================================================
     */

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


    int seaLevel =
            level.getSeaLevel();

    /*
     * Último bloco de água vanilla.
     */
    int waterSurfaceY =
            seaLevel - 1;


    /*
     * =========================================================
     * POSIÇÃO BASE
     * =========================================================
     */
int anchorChunkX =
        origin.getX() >> 4;

int anchorChunkZ =
        origin.getZ() >> 4;


/*
 * Centro aproximadamente no meio do chunk.
 *
 * Jitter de só +-2 blocos.
 *
 * Isso dá espaço para icebergs grandes crescerem
 * sem chegar facilmente a 2 chunks de distância.
 */

int x =
        (anchorChunkX << 4)
                +
                8
                +
                random.nextInt(5)
                -
                2;

int z =
        (anchorChunkZ << 4)
                +
                8
                +
                random.nextInt(5)
                -
                2;


    /*
     * Precisamos começar no Southern Ocean.
     */
    if (
            !isIcebergWater(
                    level,
                    x,
                    waterSurfaceY,
                    z,
                    false
            )
    ) {

        return false;
    }


    /*
     * =========================================================
     * PROXIMIDADE DA COSTA
     * =========================================================
     *
     * AntarcticField.sample:
     *
     * 0.0 = longe
     * 0.5 = fronteira real da Antártida
     */

    double coastApproach =
            clamp01(
                    AntarcticField.sample(
                            x,
                            z
                    )
                            /
                            0.5
            );


    /*
     * =========================================================
     * PACK ICE
     * =========================================================
     *
     * Esse campo já existe no AntarcticField.
     *
     * Quanto maior:
     *
     * mais gelo
     * mais fragmentos
     * mais próximos
     */

    double pack =
            AntarcticField.packIceStrength(
                    x,
                    z
            );


    /*
     * Mistura os dois campos.
     *
     * pack:
     * começa a encher o oceano antes.
     *
     * coastApproach:
     * dá aquele boost absurdo perto da costa.
     */

    double crowding =
            clamp01(
                    pack * 0.72
                            +
                            coastApproach * 0.45
            );


    /*
     * =========================================================
     * CHANCE
     * =========================================================
     */

    float spawnChance =
            (float) lerp(
                    FAR_SPAWN_CHANCE,
                    COAST_SPAWN_CHANCE,
                    crowding
            );


    if (
            random.nextFloat()
                    >
                    spawnChance
    ) {

        return false;
    }


    /*
     * =========================================================
     * ICEBERG PRINCIPAL
     * =========================================================
     */

    boolean changed =
            generatePrimaryIceberg(
                    level,
                    random,

                    x,
                    waterSurfaceY,
                    z,

                    coastApproach,
                    crowding
            );


    if (
            !changed
    ) {

        return false;
    }


    /*
     * =========================================================
     * FRAGMENTOS AO REDOR
     * =========================================================
     *
     * Esse é o pedaço novo importante.
     *
     * Em vez de:
     *
     *      iceberg
     *
     *
     * teremos:
     *
     *   floe
     *      iceberg
     *  floe   floe
     *
     *
     * perto da costa eles começam inclusive
     * a se sobrepor.
     */

    int extraPieces =
            getExtraPieceCount(
                    random,
                    crowding,
                    coastApproach
            );


    /*
     * Longe:
     * até ~18 blocos de separação.
     *
     * Perto:
     * ~8 blocos.
     *
     * Como os floes têm raios de 3–9,
     * vários vão literalmente encostar.
     */

    int spread =
            Math.max(
                    6,

                    (int) Math.round(
                            18.0
                                    -
                                    crowding * 10.0
                    )
            );


    for (
            int i = 0;
            i < extraPieces;
            i++
    ) {

        int offsetX =
                random.nextInt(
                        spread * 2 + 1
                )
                        -
                        spread;


        int offsetZ =
                random.nextInt(
                        spread * 2 + 1
                )
                        -
                        spread;


        /*
         * =====================================================
         * EMPURRÃO EM DIREÇÃO À ANTÁRTIDA
         * =====================================================
         *
         * No seu sistema geográfico, aumentar Z sempre nos
         * leva na direção da Antártida.
         *
         * Quanto mais perto da costa, mais os pedaços tendem
         * a continuar nessa direção.
         */

        if (
                coastApproach > 0.35
        ) {

            int coastPush =
                    (int) Math.round(
                            coastApproach * 7.0
                    );


            if (
                    coastPush > 0
            ) {

                offsetZ +=
                        random.nextInt(
                                coastPush + 1
                        );
            }
        }


        int pieceX =
                x + offsetX;

        int pieceZ =
                z + offsetZ;


        /*
         * Satélites podem entrar um pouquinho na
         * região costeira antártica SE ainda houver água.
         *
         * Isso permite que eles realmente encontrem a costa.
         */

        if (
                !isIcebergWater(
                        level,
                        pieceX,
                        waterSurfaceY,
                        pieceZ,
                        true
                )
        ) {

            continue;
        }


        double localCoast =
                clamp01(
                        AntarcticField.sample(
                                pieceX,
                                pieceZ
                        )
                                /
                                0.5
                );


        changed |=
                generateSatelliteIce(
                        level,
                        random,

                        pieceX,
                        waterSurfaceY,
                        pieceZ,

                        localCoast,
                        crowding
                );
    }


    /*
     * =========================================================
     * CALVING FIELD
     * =========================================================
     *
     * Muito perto da costa:
     *
     * cria pequenos pedaços em sequência em direção
     * ao continente.
     *
     * Visualmente parece que:
     *
     * uma placa quebrou
     * e vários fragmentos ficaram presos ao redor.
     */

    if (
            coastApproach > 0.70

                    &&

                    random.nextFloat() < 0.70F
    ) {

        changed |=
                generateCalvingChain(
                        level,
                        random,

                        x,
                        waterSurfaceY,
                        z,

                        coastApproach
                );
    }


    return changed;
}
/*
 * =========================================================
 * ICEBERG PRINCIPAL
 * =========================================================
 */

private boolean generatePrimaryIceberg(
        WorldGenLevel level,
        RandomSource random,

        int x,
        int waterY,
        int z,

        double coastApproach,
        double crowding
) {

    /*
     * Longe:
     *
     * muitos floes
     * poucos tabulares
     *
     *
     * Perto:
     *
     * mais tabulares
     * ainda muitos baixos
     */

    double floeChance =
            lerp(
                    0.46,
                    0.16,
                    crowding
            );


    double lowChance =
            lerp(
                    0.31,
                    0.23,
                    crowding
            );


    double tabularChance =
            lerp(
                    0.18,
                    0.46,
                    crowding
            );


    double roll =
            random.nextDouble();


    if (
            roll < floeChance
    ) {

        return generateFlatFloe(
                level,
                random,

                x,
                waterY,
                z
        );
    }


    if (
            roll
                    <
                    floeChance
                            +
                            lowChance
    ) {

        return generateLowIceberg(
                level,
                random,

                x,
                waterY,
                z
        );
    }


    if (
            roll
                    <
                    floeChance
                            +
                            lowChance
                            +
                            tabularChance
    ) {

        return generateTabular(
                level,
                random,

                x,
                waterY,
                z,

                coastApproach
        );
    }


    /*
     * Irregulares continuam existindo,
     * mas são raros.
     */

    return generateIrregular(
            level,
            random,

            x,
            waterY,
            z,

            coastApproach
    );
}


/*
 * =========================================================
 * QUANTIDADE DE FRAGMENTOS
 * =========================================================
 */

private int getExtraPieceCount(
        RandomSource random,
        double crowding,
        double coastApproach
) {

    int amount =
            0;


    /*
     * Chance básica.
     */

    if (
            random.nextDouble()
                    <
                    crowding
    ) {

        amount++;
    }


    /*
     * Pack médio.
     */

    if (
            crowding > 0.45
    ) {

        amount +=
                random.nextInt(2);
    }


    /*
     * Pack pesado.
     */

    if (
            crowding > 0.68
    ) {

        amount +=
                1
                        +
                        random.nextInt(2);
    }


    /*
     * Costa imediata.
     */

    if (
            coastApproach > 0.78

                    &&

                    random.nextFloat() < 0.55F
    ) {

        amount++;
    }


    /*
     * Segurança.
     *
     * Não queremos um chunk gerando
     * 37 geleiras e mandando o Ryzen pedir demissão.
     */

    return Math.min(
            amount,
            5
    );
}


/*
 * =========================================================
 * FRAGMENTOS SECUNDÁRIOS
 * =========================================================
 */

private boolean generateSatelliteIce(
        WorldGenLevel level,
        RandomSource random,

        int x,
        int waterY,
        int z,

        double coastApproach,
        double crowding
) {

    double roll =
            random.nextDouble();


    /*
     * A maioria é floe.
     */

    if (
            roll < 0.62
    ) {

        return generateFlatFloe(
                level,
                random,

                x,
                waterY,
                z
        );
    }


    /*
     * Muitos outros são baixos.
     */

    if (
            roll < 0.93
    ) {

        return generateLowIceberg(
                level,
                random,

                x,
                waterY,
                z
        );
    }


    /*
     * Pack muito forte:
     * ocasionalmente nasce outro tabular
     * colado no primeiro.
     */

    if (
            crowding > 0.72
    ) {

        return generateTabular(
                level,
                random,

                x,
                waterY,
                z,

                coastApproach
        );
    }


    return generateFlatFloe(
            level,
            random,

            x,
            waterY,
            z
    );
}


/*
 * =========================================================
 * CALVING CHAIN
 * =========================================================
 *
 * Cria uma sequência curta de fragmentos
 * indo em direção +Z.
 *
 * Como +Z é em direção ao continente
 * no AntarcticField, isso cria naturalmente:
 *
 *
 * ANTÁRTIDA
 * █████████████
 *
 *       ██
 *     █████
 *    ███
 *   ██
 *
 * oceano
 */

private boolean generateCalvingChain(
        WorldGenLevel level,
        RandomSource random,

        int centerX,
        int waterY,
        int centerZ,

        double coastApproach
) {

    boolean changed =
            false;


    int pieces =
            1
                    +
                    random.nextInt(3);


    int currentX =
            centerX;

    int currentZ =
            centerZ;


    for (
            int i = 0;
            i < pieces;
            i++
    ) {

        /*
         * Pequena deriva lateral.
         */

        currentX +=
                random.nextInt(9)
                        -
                        4;


        /*
         * Sempre progride na direção
         * da Antártida.
         */

        currentZ +=
                3
                        +
                        random.nextInt(6);


        if (
                !isIcebergWater(
                        level,
                        currentX,
                        waterY,
                        currentZ,
                        true
                )
        ) {

            continue;
        }


        /*
         * Fragmentos de calving são normalmente
         * bem baixos.
         */

        if (
                random.nextFloat() < 0.72F
        ) {

            changed |=
                    generateFlatFloe(
                            level,
                            random,

                            currentX,
                            waterY,
                            currentZ
                    );
        }

        else {

            changed |=
                    generateLowIceberg(
                            level,
                            random,

                            currentX,
                            waterY,
                            currentZ
                    );
        }
    }


    return changed;
}


/*
     * =========================================================
     * FLOE
     * =========================================================
     *
     * Pequena placa.
     *
     * Pode ficar:
     *
     * - exatamente nivelada com a água
     * - 1 bloco acima
     */

    private boolean generateFlatFloe(
            WorldGenLevel level,
            RandomSource random,

            int centerX,
            int waterY,
            int centerZ
    ) {

        int radiusX =
                3
                        +
                        random.nextInt(7);

        int radiusZ =
                3
                        +
                        random.nextInt(7);


        /*
         * waterY:
         *
         * topo do bloco fica exatamente na
         * mesma altura visual da água.
         *
         * waterY + 1:
         *
         * um bloco acima.
         */

        int topY =
                waterY
                        +
                        random.nextInt(2);


        int underwaterDepth =
                1
                        +
                        random.nextInt(4);


        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);


        boolean changed =
                false;


        for (
                int dx = -radiusX - 1;
                dx <= radiusX + 1;
                dx++
        ) {

            for (
                    int dz = -radiusZ - 1;
                    dz <= radiusZ + 1;
                    dz++
            ) {
                int worldX = centerX + dx;
                int worldZ = centerZ + dz;

                if (!canWriteColumn(level, worldX, worldZ)) {
                    continue;
                }

                double rotatedX =
                        dx * cos
                                -
                                dz * sin;

                double rotatedZ =
                        dx * sin
                                +
                                dz * cos;


                double nx =
                        rotatedX
                                /
                                radiusX;

                double nz =
                        rotatedZ
                                /
                                radiusZ;


                double distance =
                        nx * nx
                                +
                                nz * nz;


                /*
                 * Só a BORDA é irregular.
                 *
                 * O topo continua plano.
                 */

                double edge =
                        0.82
                                +
                                hash01(
                                        centerX + dx,
                                        centerZ + dz
                                )
                                *
                                0.25;


                if (
                        distance > edge
                ) {

                    continue;
                }


                double centerStrength =
                        1.0
                                -
                                Math.min(
                                        1.0,
                                        distance
                                );


                int localDepth =
                        Math.max(
                                1,

                                (int) Math.round(
                                        underwaterDepth
                                                *
                                                (
                                                        0.35
                                                                +
                                                                centerStrength
                                                                        *
                                                                        0.65
                                                )
                                )
                        );


                for (
                        int y = topY - localDepth;
                        y <= topY;
                        y++
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    centerX + dx,
                                    y,
                                    centerZ + dz
                            );


                    if (
                            !canReplace(
                                    level.getBlockState(
                                            pos
                                    )
                            )
                    ) {

                        continue;
                    }


                    BlockState state;


                    /*
                     * Tampa branca.
                     */

                    if (
                            y == topY
                    ) {

                        state =
                                Blocks.SNOW_BLOCK
                                        .defaultBlockState();
                    }

                    else {

                        state =
                                chooseBodyIce(
                                        random
                                );
                    }


                    changed |= level.setBlock(pos, state, 2);
                }
            }
        }


        return changed;
    }


    /*
     * =========================================================
     * ICEBERG BAIXO
     * =========================================================
     *
     * 1–3 blocos acima do oceano.
     *
     * Ótimo para gameplay:
     * fácil de subir e atravessar.
     */

    private boolean generateLowIceberg(
            WorldGenLevel level,
            RandomSource random,

            int centerX,
            int waterY,
            int centerZ
    ) {

        int radiusX =
                5
                        +
                        random.nextInt(7);

        int radiusZ =
                5
                        +
                        random.nextInt(7);


        int above =
                1
                        +
                        random.nextInt(3);


        int below =
                4
                        +
                        random.nextInt(6);


        int topY =
                waterY
                        +
                        above;


        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);


        boolean changed =
                false;


        for (
                int dx = -radiusX - 1;
                dx <= radiusX + 1;
                dx++
        ) {

            for (
                    int dz = -radiusZ - 1;
                    dz <= radiusZ + 1;
                    dz++
            ) {
                int worldX = centerX + dx;
                int worldZ = centerZ + dz;

                if (!canWriteColumn(level, worldX, worldZ)) {
                    continue;
                }

                double rotatedX =
                        dx * cos
                                -
                                dz * sin;

                double rotatedZ =
                        dx * sin
                                +
                                dz * cos;


                double nx =
                        rotatedX
                                /
                                radiusX;

                double nz =
                        rotatedZ
                                /
                                radiusZ;


                double distance =
                        nx * nx
                                +
                                nz * nz;


                double edge =
                        0.84
                                +
                                hash01(
                                        centerX + dx,
                                        centerZ + dz
                                )
                                *
                                0.22;


                if (
                        distance > edge
                ) {

                    continue;
                }


                double centerStrength =
                        1.0
                                -
                                Math.min(
                                        1.0,
                                        distance
                                );


                int localBelow =
                        Math.max(
                                2,

                                (int) Math.round(
                                        below
                                                *
                                                (
                                                        0.40
                                                                +
                                                                centerStrength
                                                                        *
                                                                        0.60
                                                )
                                )
                        );


                /*
                 * IMPORTANTE:
                 *
                 * topo completamente plano.
                 */

                for (
                        int y = waterY - localBelow;
                        y <= topY;
                        y++
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    centerX + dx,
                                    y,
                                    centerZ + dz
                            );


                    if (
                            !canReplace(
                                    level.getBlockState(
                                            pos
                                    )
                            )
                    ) {

                        continue;
                    }


                    BlockState state =
                            y == topY
                                    ?
                                    Blocks.SNOW_BLOCK
                                            .defaultBlockState()
                                    :
                                    chooseBodyIce(
                                            random
                                    );


                    changed |= level.setBlock(pos, state, 2);
                }
            }
        }


        return changed;
    }


    /*
     * =========================================================
     * TABULAR
     * =========================================================
     *
     * O iceberg clássico da Antártida.
     *
     * Grande.
     * Largo.
     * Quase uma ilha.
     * E principalmente:
     *
     * TOPO PLANO.
     */

    private boolean generateTabular(
            WorldGenLevel level,
            RandomSource random,

            int centerX,
            int waterY,
            int centerZ,

            double coastApproach
    ) {

        /*
         * Quanto mais perto do continente,
         * maiores eles podem ficar.
         */

        int bonus =
                (int) Math.round(
                        coastApproach
                                *
                                5.0
                );


        int radiusX =
                9
                        +
                        bonus
                        +
                        random.nextInt(10);


        int radiusZ =
                7
                        +
                        bonus
                        +
                        random.nextInt(8);


        int above =
                2
                        +
                        random.nextInt(4);


        int below =
                8
                        +
                        random.nextInt(9);


        int topY =
                waterY
                        +
                        above;


        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);


        boolean changed =
                false;


        for (
                int dx = -radiusX - 2;
                dx <= radiusX + 2;
                dx++
        ) {

            for (
                    int dz = -radiusZ - 2;
                    dz <= radiusZ + 2;
                    dz++
            ) {
                int worldX = centerX + dx;
                int worldZ = centerZ + dz;

                if (!canWriteColumn(level, worldX, worldZ)) {
                    continue;
                }

                double rotatedX =
                        dx * cos
                                -
                                dz * sin;

                double rotatedZ =
                        dx * sin
                                +
                                dz * cos;


                double nx =
                        rotatedX
                                /
                                radiusX;

                double nz =
                        rotatedZ
                                /
                                radiusZ;


                double horizontalShape =
                        nx * nx
                                +
                                nz * nz;


                /*
                 * Irregularidade apenas na borda.
                 *
                 * O TOPO não muda.
                 */

                double edge =
                        0.86
                                +
                                hash01(
                                        centerX + dx,
                                        centerZ + dz
                                )
                                *
                                0.20;


                if (
                        horizontalShape > edge
                ) {

                    continue;
                }


                double centerStrength =
                        1.0
                                -
                                Math.min(
                                        1.0,
                                        horizontalShape
                                );


                /*
                 * Embaixo ele afina nas bordas.
                 */

                int localBelow =
                        Math.max(
                                3,

                                (int) Math.round(
                                        below
                                                *
                                                (
                                                        0.45
                                                                +
                                                                centerStrength
                                                                        *
                                                                        0.55
                                                )
                                )
                        );


                for (
                        int y = waterY - localBelow;
                        y <= topY;
                        y++
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    centerX + dx,
                                    y,
                                    centerZ + dz
                            );


                    if (
                            !canReplace(
                                    level.getBlockState(
                                            pos
                                    )
                            )
                    ) {

                        continue;
                    }


                    BlockState state =
                            y == topY
                                    ?
                                    Blocks.SNOW_BLOCK
                                            .defaultBlockState()
                                    :
                                    chooseBodyIce(
                                            random
                                    );


                    changed |= level.setBlock(pos, state, 2);
                }
            }
        }


        return changed;
    }


    /*
     * =========================================================
     * IRREGULAR
     * =========================================================
     *
     * Raros.
     *
     * Esses podem continuar mais "dramáticos".
     */

    private boolean generateIrregular(
            WorldGenLevel level,
            RandomSource random,

            int centerX,
            int waterY,
            int centerZ,

            double coastApproach
    ) {

        int bonus =
                (int) Math.round(
                        coastApproach
                                *
                                3.0
                );


        int radiusX =
                4
                        +
                        bonus
                        +
                        random.nextInt(6);

        int radiusZ =
                4
                        +
                        bonus
                        +
                        random.nextInt(6);


        int above =
                4
                        +
                        random.nextInt(6);


        int below =
                7
                        +
                        random.nextInt(9);


        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0;


        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);


        boolean changed =
                false;


        for (
                int dx = -radiusX - 2;
                dx <= radiusX + 2;
                dx++
        ) {

            for (
                    int dz = -radiusZ - 2;
                    dz <= radiusZ + 2;
                    dz++
            ) {
                int worldX = centerX + dx;
                int worldZ = centerZ + dz;

                if (!canWriteColumn(level, worldX, worldZ)) {
                    continue;
                }

                double rotatedX =
                        dx * cos
                                -
                                dz * sin;

                double rotatedZ =
                        dx * sin
                                +
                                dz * cos;


                double distortion =
                        0.90
                                +
                                hash01(
                                        centerX + dx,
                                        centerZ + dz
                                )
                                *
                                0.16;


                for (
                        int dy = -below;
                        dy <= above;
                        dy++
                ) {

                    double rx =
                            rotatedX
                                    /
                                    radiusX;

                    double rz =
                            rotatedZ
                                    /
                                    radiusZ;


                    double vertical =
                            dy >= 0
                                    ?
                                    dy
                                            /
                                            (double) above
                                    :
                                    dy
                                            /
                                            (double) below;


                    double shape =
                            rx * rx
                                    +
                                    rz * rz
                                    +
                                    vertical * vertical;


                    if (
                            dy < 0
                    ) {

                        shape *=
                                0.88;
                    }


                    if (
                            shape > distortion
                    ) {

                        continue;
                    }


                    BlockPos pos =
                            new BlockPos(
                                    centerX + dx,
                                    waterY + dy,
                                    centerZ + dz
                            );


                    if (
                            !canReplace(
                                    level.getBlockState(
                                            pos
                                    )
                            )
                    ) {

                        continue;
                    }


                    BlockState state;


                    if (
                            dy >= above - 1
                    ) {

                        state =
                                Blocks.SNOW_BLOCK
                                        .defaultBlockState();
                    }

                    else {

                        state =
                                chooseBodyIce(
                                        random
                                );
                    }


                    changed |= level.setBlock(pos, state, 2);
                }
            }
        }


        return changed;
    }


    /*
     * =========================================================
     * MATERIAL DO CORPO
     * =========================================================
     */

    private BlockState chooseBodyIce(
            RandomSource random
    ) {

        /*
         * Blue Ice raro.
         */

        if (
                random.nextFloat()
                        <
                        0.055F
        ) {

            return Blocks.BLUE_ICE
                    .defaultBlockState();
        }


        return Blocks.PACKED_ICE
                .defaultBlockState();
    }


    /*
     * =========================================================
     * REPLACE
     * =========================================================
     */

   private boolean canReplace(
        BlockState state
) {

    return state.isAir()

            ||

            state.getFluidState()
                    .is(
                            FluidTags.WATER
                    );
}
    /*
     * =========================================================
     * HASH
     * =========================================================
     */

    private double hash01(
            int x,
            int z
    ) {

        long value =
                918273645L;


        value ^=
                (long) x
                        *
                        341873128712L;


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


        return (
                (
                        value >>> 11
                )
                        /
                        (double) (
                                1L << 53
                        )
        );
    }
    private static boolean canWriteColumn(WorldGenLevel level, int blockX, int blockZ) {
        if (!isChunkAvailable(level, blockX, blockZ)) {
            return false;
        }

        // FEATURES permits writes within one chunk of the region center.
        // hasChunk also includes more distant chunks available only for reading.
        if (level instanceof WorldGenRegion region) {
            return region.getCenter().getChessboardDistance(blockX >> 4, blockZ >> 4) <= 1;
        }

        return true;
    }

    private static boolean isChunkAvailable(
        WorldGenLevel level,
        int blockX,
        int blockZ
) {

    int chunkX =
            blockX >> 4;

    int chunkZ =
            blockZ >> 4;

    return level.hasChunk(
            chunkX,
            chunkZ
    );
}       


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                +
                (
                        b - a
                )
                        *
                        t;
    }


    private static double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
