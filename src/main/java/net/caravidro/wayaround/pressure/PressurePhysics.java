package net.caravidro.wayaround.pressure;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.MatterState;

/**
 * Material-aware pressure calculations.
 */
public final class PressurePhysics {

    private PressurePhysics() {
    }

    public static double hydrostaticGaugeKPa(
            MaterialDefinition liquid,
            double depthM
    ) {
        return hydrostaticGaugeKPa(
                liquid,
                MatterPhase.LIQUID,
                depthM
        );
    }

    public static double hydrostaticGaugeKPa(
            MaterialDefinition material,
            MatterPhase phase,
            double depthM
    ) {
        return PressureMath.hydrostaticGaugeKPa(
                material.phase(
                        phase
                ).densityKgPerM3(),
                depthM
        );
    }

    public static double gasAbsoluteKPa(
            MatterState gas
    ) {
        if (gas.phase()
                != MatterPhase.GAS) {
            throw new IllegalArgumentException(
                    "gasAbsoluteKPa requires GAS matter"
            );
        }

        return gasAbsoluteKPa(
                gas.material(),
                gas.massKg(),
                gas.volumeM3(),
                gas.temperatureC()
        );
    }

    public static double gasAbsoluteKPa(
            MaterialDefinition material,
            double massKg,
            double volumeM3,
            double temperatureC
    ) {
        var phase =
                material.phase(
                        MatterPhase.GAS
                );

        double referenceTemperature =
                material.transitions()
                        .hasBoilingPoint()
                        ? material.transitions()
                                .boilingPointC()
                        : 20.0;

        return PressureMath.gasAbsoluteKPa(
                massKg,
                volumeM3,
                temperatureC,
                phase.densityKgPerM3(),
                referenceTemperature,
                PressureMath.STANDARD_ATMOSPHERE_KPA
        );
    }

    public static double gasDensityKgPerM3(
            MaterialDefinition material,
            double absolutePressureKPa,
            double temperatureC
    ) {
        var phase =
                material.phase(
                        MatterPhase.GAS
                );

        double referenceTemperature =
                material.transitions()
                        .hasBoilingPoint()
                        ? material.transitions()
                                .boilingPointC()
                        : 20.0;

        double pressureRatio =
                Math.max(
                        0.0,
                        absolutePressureKPa
                )
                        / PressureMath.STANDARD_ATMOSPHERE_KPA;

        double temperatureRatio =
                PressureMath.celsiusToKelvin(
                        referenceTemperature
                )
                        / PressureMath.celsiusToKelvin(
                        temperatureC
                );

        return phase.densityKgPerM3()
                * pressureRatio
                * temperatureRatio;
    }

    public static double gasGaugeKPa(
            MaterialDefinition material,
            double massKg,
            double volumeM3,
            double temperatureC,
            double outsideAbsoluteKPa
    ) {
        return Math.max(
                0.0,
                PressureMath.gaugeKPa(
                        gasAbsoluteKPa(
                                material,
                                massKg,
                                volumeM3,
                                temperatureC
                        ),
                        outsideAbsoluteKPa
                )
        );
    }
}
