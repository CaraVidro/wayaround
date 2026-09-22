package net.caravidro.wayaround.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.caravidro.wayaround.worldgen.geography.AntarcticField;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;


public record AntarcticDensityFunction(
        DensityFunction input
) implements DensityFunction {

    /*
     * =========================================================
     * CODEC
     * =========================================================
     *
     * A density function vanilla original fica guardada
     * dentro de "input".
     */

    public static final MapCodec<AntarcticDensityFunction> DATA_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                    DensityFunction.HOLDER_HELPER_CODEC
                                            .fieldOf("input")
                                            .forGetter(
                                                    AntarcticDensityFunction::input
                                            )
                            ).apply(
                                    instance,
                                    AntarcticDensityFunction::new
                            )
            );


    public static final KeyDispatchDataCodec<AntarcticDensityFunction> CODEC =
            KeyDispatchDataCodec.of(
                    DATA_CODEC
            );


    /*
     * =========================================================
     * COMPUTE
     * =========================================================
     */

    @Override
    public double compute(
            FunctionContext context
    ) {

        /*
         * Primeiro descobrimos ONDE estamos.
         *
         * Isso permite que regiões 100% customizadas
         * nem precisem usar a density vanilla.
         */

        int x =
                context.blockX();

        int y =
                context.blockY();

        int z =
                context.blockZ();


        /*
         * =====================================================
         * ANTÁRTIDA
         * =====================================================
         *
         * Se já estamos completamente dentro do continente,
         * usamos APENAS o terrain do WayAround.
         */

        double antarcticStrength =
                AntarcticField.sample(
                        x,
                        z
                );

        double antarcticBlend =
                IceCliffField.coastBlend(x, context.blockY(), z, antarcticStrength);


        if (
                antarcticBlend >= 0.999
        ) {

            return AntarcticTerrain.sampleDensity(
                    x,
                    y,
                    z
            );
        }


        /*
         * =====================================================
         * SOUTHERN OCEAN
         * =====================================================
         *
         * Mesma ideia.
         *
         * Se estamos completamente dentro do oceano:
         *
         * NÃO.
         * USE.
         * TERRAIN.
         * VANILLA.
         *
         * 💀
         */

        double oceanStrength =
                AntarcticField.southernOceanEntryStrength(
                        x,
                        z
                );

        if (
                oceanStrength >= 0.999

                &&

                antarcticBlend <= 0.001
        ) {

            return AntarcticTerrain.sampleIcebergOceanDensity(
                    x,
                    y,
                    z
            );
        }


        /*
         * =====================================================
         * REGIÃO DE TRANSIÇÃO
         * =====================================================
         *
         * Só aqui precisamos da density vanilla.
         */

        double vanillaDensity =
                input.compute(
                        context
                );


        return blendDensity(
                vanillaDensity,
                context,

                oceanStrength,
                antarcticBlend
        );
    }


    /*
     * =========================================================
     * BLEND
     * =========================================================
     */

    private static double blendDensity(
            double vanillaDensity,
            FunctionContext context
    ) {

        int x =
                context.blockX();

        int z =
                context.blockZ();


        double oceanStrength =
                AntarcticField.southernOceanEntryStrength(
                        x,
                        z
                );


        double antarcticStrength =
                AntarcticField.sample(
                        x,
                        z
                );


        double antarcticBlend =
                IceCliffField.coastBlend(x, context.blockY(), z, antarcticStrength);


        return blendDensity(
                vanillaDensity,
                context,

                oceanStrength,
                antarcticBlend
        );
    }


    /*
     * Versão que já recebe as strengths calculadas.
     *
     * compute() usa ela para evitar recalcular coisas
     * desnecessariamente.
     */

    private static double blendDensity(
            double vanillaDensity,
            FunctionContext context,

            double oceanStrength,
            double antarcticBlend
    ) {

        int x =
                context.blockX();

        int y =
                context.blockY();

        int z =
                context.blockZ();


        double density =
                vanillaDensity;


        /*
         * =====================================================
         * SOUTHERN OCEAN
         * =====================================================
         */

        if (
                oceanStrength > 0.0
        ) {

            double oceanDensity =
                    AntarcticTerrain.sampleIcebergOceanDensity(
                            x,
                            y,
                            z
                    );


            /*
             * Usamos smoothstep novamente para que
             * a entrada do oceano seja suave.
             */

            double oceanBlend =
                    smoothstep(
                            clamp01(
                                    oceanStrength
                            )
                    );


            density =
                    lerp(
                            density,
                            oceanDensity,
                            oceanBlend
                    );
        }


        /*
         * =====================================================
         * ANTÁRTIDA
         * =====================================================
         *
         * O continente vem DEPOIS do oceano.
         *
         * Portanto, perto da costa:
         *
         * vanilla
         *    ↓
         * Southern Ocean
         *    ↓
         * Antarctica
         */

        if (
                antarcticBlend > 0.0
        ) {

            double antarcticDensity =
                    AntarcticTerrain.sampleDensity(
                            x,
                            y,
                            z
                    );


            double continentBlend =
                    smoothstep(
                            clamp01(
                                    antarcticBlend
                            )
                    );


            density =
                    lerp(
                            density,
                            antarcticDensity,
                            continentBlend
                    );
        }


        return density;
    }


    /*
     * =========================================================
     * BULK EVALUATION
     * =========================================================
     *
     * Minecraft frequentemente calcula várias densities de uma
     * vez.
     *
     * Precisamos manter esse caminho funcionando também.
     */

    @Override
    public void fillArray(
            double[] values,
            ContextProvider contextProvider
    ) {

        /*
         * Primeiro deixa a density vanilla preencher o array.
         */

        input.fillArray(
                values,
                contextProvider
        );


        /*
         * Depois substituímos / misturamos cada ponto.
         */

        for (
                int i = 0;
                i < values.length;
                i++
        ) {

            FunctionContext context =
                    contextProvider.forIndex(
                            i
                    );


            int x =
                    context.blockX();

            int y =
                    context.blockY();

            int z =
                    context.blockZ();


            /*
             * =============================================
             * ANTÁRTIDA COMPLETA
             * =============================================
             */

            double antarcticStrength =
                    AntarcticField.sample(
                            x,
                            z
                    );


            double antarcticBlend =
                    IceCliffField.coastBlend(x, context.blockY(), z, antarcticStrength);


            if (
                    antarcticBlend >= 0.999
            ) {

                values[i] =
                        AntarcticTerrain.sampleDensity(
                                x,
                                y,
                                z
                        );

                continue;
            }


            /*
             * =============================================
             * SOUTHERN OCEAN COMPLETO
             * =============================================
             */

            double oceanStrength =
                    AntarcticField.southernOceanEntryStrength(
                            x,
                            z
                    );


            if (
                    oceanStrength >= 0.999

                    &&

                    antarcticBlend <= 0.001
            ) {

                values[i] =
                        AntarcticTerrain.sampleIcebergOceanDensity(
                                x,
                                y,
                                z
                        );

                continue;
            }


            /*
             * =============================================
             * TRANSIÇÃO
             * =============================================
             */

            values[i] =
                    blendDensity(
                            values[i],
                            context,

                            oceanStrength,
                            antarcticBlend
                    );
        }
    }


    /*
     * =========================================================
     * MAP ALL
     * =========================================================
     */

    @Override
    public DensityFunction mapAll(
            Visitor visitor
    ) {

        DensityFunction mappedInput =
                input.mapAll(
                        visitor
                );


        return visitor.apply(
                new AntarcticDensityFunction(
                        mappedInput
                )
        );
    }


    /*
     * =========================================================
     * RANGE
     * =========================================================
     */

    @Override
    public double minValue() {

        return Double.NEGATIVE_INFINITY;
    }


    @Override
    public double maxValue() {

        return Double.POSITIVE_INFINITY;
    }


    /*
     * =========================================================
     * CODEC
     * =========================================================
     */

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {

        return CODEC;
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


    private static double smoothstep(
            double t
    ) {

        return t
                *
                t
                *
                (
                        3.0
                                -
                                2.0
                                *
                                t
                );
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