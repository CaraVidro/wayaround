package net.caravidro.wayaround.pressure;

import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * First lumped gas-pressure model for physical regions.
 *
 * <p>Sealed rooms retain their reference amount of air, so temperature changes
 * change pressure. Vented/indeterminate rooms remain coupled to natural
 * atmosphere until the future flow solver tracks real gas transfer.</p>
 */
public record RegionPressureModel(
        PressureState pressure,
        double temperatureC,
        double ambientTemperatureC
) {

    public static RegionPressureModel at(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            BlockPos sample
    ) {
        PressureState outside =
                NaturalPressure.at(
                        level,
                        sample
                );

        double currentTemperature =
                EnvironmentalTemperature.at(
                        level,
                        sample
                );

        double ambientTemperature =
                EnvironmentalTemperature.ambientAt(
                        level,
                        sample
                );

        if (!region.sealed()
                || outside.source()
                == PressureState.Source.HYDROSTATIC) {
            return new RegionPressureModel(
                    outside,
                    currentTemperature,
                    ambientTemperature
            );
        }

        double absolute =
                outside.absoluteKPa()
                        * (
                        PressureMath.celsiusToKelvin(
                                currentTemperature
                        )
                                / PressureMath.celsiusToKelvin(
                                ambientTemperature
                        )
                );

        return new RegionPressureModel(
                new PressureState(
                        Math.max(
                                0.0,
                                absolute
                        ),
                        outside.absoluteKPa(),
                        0.0,
                        PhysicalMaterials.AIR,
                        PressureState.Source.GAS
                ),
                currentTemperature,
                ambientTemperature
        );
    }
}
