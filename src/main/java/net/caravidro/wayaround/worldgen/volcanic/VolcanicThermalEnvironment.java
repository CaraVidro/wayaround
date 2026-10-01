package net.caravidro.wayaround.worldgen.volcanic;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Ambient geothermal contribution for volcanic provinces.
 */
public final class VolcanicThermalEnvironment {

    private VolcanicThermalEnvironment() {
    }

    public static double modify(
            ServerLevel level,
            BlockPos pos,
            double currentCelsius
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.VOLCANIC_REGIONS
        )) {
            return currentCelsius;
        }

        double influence =
                VolcanicField.influence(
                        pos.getX(),
                        pos.getZ()
                );

        if (influence
                <= 0.01) {
            return currentCelsius;
        }

        double crater =
                VolcanicField.craterStrength(
                        pos.getX(),
                        pos.getZ()
                );

        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        pos.getX(),
                        pos.getZ()
                );

        double activity =
                volcano == null
                        ? 0.0
                        : volcano.activity();

        double geothermal =
                24.0
                        + influence
                        * 44.0
                        + crater
                        * (
                        30.0
                                + activity
                                * 62.0
                );

        return Math.max(
                currentCelsius,
                geothermal
        );
    }
}
