package net.caravidro.wayaround.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.caravidro.wayaround.worldgen.geography.GreatRiftField;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record GreatRiftDensityFunction(
        DensityFunction input
) implements DensityFunction {

    public static final MapCodec<GreatRiftDensityFunction> DATA_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                    DensityFunction.HOLDER_HELPER_CODEC
                                            .fieldOf(
                                                    "input"
                                            )
                                            .forGetter(
                                                    GreatRiftDensityFunction::input
                                            )
                            )
                                    .apply(
                                            instance,
                                            GreatRiftDensityFunction::new
                                    )
            );

    public static final KeyDispatchDataCodec<GreatRiftDensityFunction> CODEC =
            KeyDispatchDataCodec.of(
                    DATA_CODEC
            );

    @Override
    public double compute(
            FunctionContext context
    ) {
        double influence =
                GreatRiftField.influence(
                        context.blockX(),
                        context.blockZ()
                );

        if (influence
                <= 0.001) {
            return input.compute(
                    context
            );
        }

        double rift =
                GreatRiftTerrain.sampleDensity(
                        context.blockX(),
                        context.blockY(),
                        context.blockZ()
                );

        if (influence
                >= 0.999) {
            return rift;
        }

        double base =
                input.compute(
                        context
                );

        double blend =
                influence
                        * influence
                        * (
                        3.0
                                - 2.0
                                * influence
                );

        return base
                + (
                rift - base
        )
                * blend;
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

            double influence =
                    GreatRiftField.influence(
                            context.blockX(),
                            context.blockZ()
                    );

            if (influence
                    <= 0.001) {
                continue;
            }

            double rift =
                    GreatRiftTerrain.sampleDensity(
                            context.blockX(),
                            context.blockY(),
                            context.blockZ()
                    );

            double blend =
                    influence >= 0.999
                            ? 1.0
                            : influence
                            * influence
                            * (
                            3.0
                                    - 2.0
                                    * influence
                    );

            values[i] =
                    values[i]
                            + (
                            rift - values[i]
                    )
                            * blend;
        }
    }

    @Override
    public DensityFunction mapAll(
            Visitor visitor
    ) {
        return visitor.apply(
                new GreatRiftDensityFunction(
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
