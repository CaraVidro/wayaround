package net.caravidro.wayaround.thermal;

import java.util.Optional;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla compatibility adapter from known heat-producing blocks into physical
 * power and source temperature.
 *
 * <p>These values are gameplay-scale thermal outputs. The universal thermal
 * API itself does not know block identities; adapters do.</p>
 */
public record ThermalSourceProfile(
        double powerW,
        double sourceTemperatureC
) {

    public ThermalSourceProfile {
        if (!Double.isFinite(
                powerW
        )
                || powerW <= 0.0) {
            throw new IllegalArgumentException(
                    "powerW must be finite and > 0"
            );
        }

        if (!Double.isFinite(
                sourceTemperatureC
        )) {
            throw new IllegalArgumentException(
                    "sourceTemperatureC must be finite"
            );
        }
    }

    public static Optional<ThermalSourceProfile> forBlock(
            BlockState state
    ) {
        if (state.is(
                Blocks.LAVA
        )) {
            return Optional.of(
                    new ThermalSourceProfile(
                            65_000.0,
                            1100.0
                    )
            );
        }

        if (state.is(
                Blocks.FIRE
        )
                || state.is(
                Blocks.SOUL_FIRE
        )) {
            return Optional.of(
                    new ThermalSourceProfile(
                            8_000.0,
                            620.0
                    )
            );
        }

        if ((state.is(
                Blocks.CAMPFIRE
        )
                || state.is(
                Blocks.SOUL_CAMPFIRE
        ))
                && state.hasProperty(
                CampfireBlock.LIT
        )
                && state.getValue(
                CampfireBlock.LIT
        )) {
            return Optional.of(
                    new ThermalSourceProfile(
                            6_000.0,
                            310.0
                    )
            );
        }

        if (state.is(
                Blocks.MAGMA_BLOCK
        )) {
            return Optional.of(
                    new ThermalSourceProfile(
                            1_200.0,
                            180.0
                    )
            );
        }

        return Optional.empty();
    }

    public double couplingAt(
            double currentTemperatureC,
            double ambientTemperatureC
    ) {
        double span =
                sourceTemperatureC
                        - ambientTemperatureC;

        if (span <= 1.0) {
            return 0.0;
        }

        return Math.clamp(
                (
                        sourceTemperatureC
                                - currentTemperatureC
                )
                        / span,
                0.0,
                1.0
        );
    }
}
