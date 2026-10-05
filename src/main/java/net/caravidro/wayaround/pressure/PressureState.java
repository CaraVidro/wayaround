package net.caravidro.wayaround.pressure;

import java.util.Objects;

import net.caravidro.wayaround.physical.MaterialDefinition;

/**
 * One pressure sample with an explicit absolute/reference distinction.
 */
public record PressureState(
        double absoluteKPa,
        double referenceAbsoluteKPa,
        double depthM,
        MaterialDefinition medium,
        Source source
) {
    public enum Source {
        ATMOSPHERE,
        HYDROSTATIC,
        GAS,
        HYDRAULIC,
        COMPOSITE
    }

    public PressureState {
        if (!Double.isFinite(
                absoluteKPa
        )
                || absoluteKPa < 0.0) {
            throw new IllegalArgumentException(
                    "absoluteKPa must be finite and >= 0"
            );
        }

        if (!Double.isFinite(
                referenceAbsoluteKPa
        )
                || referenceAbsoluteKPa < 0.0) {
            throw new IllegalArgumentException(
                    "referenceAbsoluteKPa must be finite and >= 0"
            );
        }

        depthM =
                Double.isFinite(
                        depthM
                )
                        ? Math.max(
                                0.0,
                                depthM
                        )
                        : 0.0;

        Objects.requireNonNull(
                medium,
                "medium"
        );

        Objects.requireNonNull(
                source,
                "source"
        );
    }

    public double gaugeKPa() {
        return PressureMath.gaugeKPa(
                absoluteKPa,
                referenceAbsoluteKPa
        );
    }

    public double absoluteBar() {
        return PressureMath.kPaToBar(
                absoluteKPa
        );
    }

    public double gaugeBar() {
        return PressureMath.kPaToBar(
                Math.max(
                        0.0,
                        gaugeKPa()
                )
        );
    }

    public double differentialKPa(
            PressureState inside
    ) {
        return PressureMath.differentialKPa(
                absoluteKPa,
                inside.absoluteKPa
        );
    }
}
