package net.caravidro.wayaround.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Wraps the already-modified Overworld density and blends in Way Around's
 * giant volcanic terrain only where a volcanic province exists.
 */
public record VolcanicDensityFunction(
        DensityFunction input
) implements DensityFunction {

    public static final MapCodec<VolcanicDensityFunction> DATA_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                    DensityFunction.HOLDER_HELPER_CODEC
                                            .fieldOf(
                                                    "input"
                                            )
                                            .forGetter(
                                                    VolcanicDensityFunction::input
                                            )
                            )
                                    .apply(
                                            instance,
                                            VolcanicDensityFunction::new
                                    )
            );

    public static final KeyDispatchDataCodec<VolcanicDensityFunction> CODEC =
            KeyDispatchDataCodec.of(
                    DATA_CODEC
            );

    @Override
    public double compute(
            FunctionContext context
    ) {
        int x =
                context.blockX();

        int y =
                context.blockY();

        int z =
                context.blockZ();

        double blend =
                VolcanicField.terrainBlend(
                        x,
                        z
                );

        if (blend
                <= 0.001) {
            return input.compute(
                    context
            );
        }

        double volcanic =
                VolcanicTerrain.sampleDensity(
                        x,
                        y,
                        z
                );

        if (blend
                >= 0.999) {
            return volcanic;
        }

        double vanilla =
                input.compute(
                        context
                );

        double smooth =
                blend
                        * blend
                        * (
                        3.0
                                - 2.0
                                * blend
                );

        return vanilla
                + (
                volcanic - vanilla
        )
                * smooth;
    }

    @Override
    public void fillArray(
            double[] values,
            ContextProvider contextProvider
    ) {
        input.fillArray(
                values,
                contextProvider
        );

        for (int i = 0;
             i < values.length;
             i++) {

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

            double blend =
                    VolcanicField.terrainBlend(
                            x,
                            z
                    );

            if (blend
                    <= 0.001) {
                continue;
            }

            double volcanic =
                    VolcanicTerrain.sampleDensity(
                            x,
                            y,
                            z
                    );

            if (blend
                    >= 0.999) {
                values[i] =
                        volcanic;

                continue;
            }

            double smooth =
                    blend
                            * blend
                            * (
                            3.0
                                    - 2.0
                                    * blend
                    );

            values[i] =
                    values[i]
                            + (
                            volcanic - values[i]
                    )
                            * smooth;
        }
    }

    @Override
    public DensityFunction mapAll(
            Visitor visitor
    ) {
        return visitor.apply(
                new VolcanicDensityFunction(
                        input.mapAll(
                                visitor
                        )
                )
        );
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
