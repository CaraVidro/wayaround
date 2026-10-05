package net.caravidro.wayaround.physical;

import java.util.Objects;

/**
 * Small runtime snapshot of actual matter.
 *
 * <p>Definition answers "what is it?". MatterState answers "what is this amount
 * of it doing right now?". It is intentionally solver-agnostic so thermal,
 * pressure and flow systems can share it later.</p>
 */
public record MatterState(
        MaterialDefinition material,
        MatterPhase phase,
        double massKg,
        double volumeM3,
        double temperatureC,
        double pressureKPa
) {
    public static final double STANDARD_PRESSURE_KPA =
            101.325;

    public MatterState {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(phase, "phase");

        if (!material.supports(phase)) {
            throw new IllegalArgumentException(
                    material.id()
                            + " does not support "
                            + phase
            );
        }

        massKg = nonNegativeFinite(
                massKg,
                "massKg"
        );

        volumeM3 = positiveFinite(
                volumeM3,
                "volumeM3"
        );

        if (!Double.isFinite(temperatureC)) {
            throw new IllegalArgumentException(
                    "temperatureC must be finite"
            );
        }

        pressureKPa =
                nonNegativeFinite(
                        pressureKPa,
                        "pressureKPa"
                );
    }

    public static MatterState forVolume(
            MaterialDefinition material,
            MatterPhase phase,
            double volumeM3,
            double temperatureC,
            double pressureKPa
    ) {
        double mass =
                material.phase(phase)
                        .densityKgPerM3()
                        * volumeM3;

        return new MatterState(
                material,
                phase,
                mass,
                volumeM3,
                temperatureC,
                pressureKPa
        );
    }

    public double densityKgPerM3() {
        return massKg / volumeM3;
    }

    public MatterState withTemperature(
            double newTemperatureC
    ) {
        return new MatterState(
                material,
                phase,
                massKg,
                volumeM3,
                newTemperatureC,
                pressureKPa
        );
    }

    public MatterState withPressure(
            double newPressureKPa
    ) {
        return new MatterState(
                material,
                phase,
                massKg,
                volumeM3,
                temperatureC,
                newPressureKPa
        );
    }

    private static double nonNegativeFinite(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and >= 0"
            );
        }

        return value;
    }

    private static double positiveFinite(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value <= 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and > 0"
            );
        }

        return value;
    }
}
