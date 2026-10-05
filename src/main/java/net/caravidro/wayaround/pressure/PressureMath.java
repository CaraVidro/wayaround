package net.caravidro.wayaround.pressure;

/**
 * Dependency-free pressure math shared by ocean, hydraulic and gas adapters.
 *
 * <p>Pressure is canonical in kPa. Bar remains a presentation/compatibility
 * unit for older machinery UIs.</p>
 */
public final class PressureMath {

    public static final double STANDARD_ATMOSPHERE_KPA =
            101.325;

    public static final double KPA_PER_BAR =
            100.0;

    public static final double GRAVITY_M_S2 =
            9.80665;

    public static final double ABSOLUTE_ZERO_C =
            -273.15;

    private PressureMath() {
    }

    public static double hydrostaticGaugeKPa(
            double densityKgPerM3,
            double depthM
    ) {
        if (!Double.isFinite(
                densityKgPerM3
        )
                || densityKgPerM3 < 0.0
                || !Double.isFinite(
                depthM
        )
                || depthM <= 0.0) {
            return 0.0;
        }

        return densityKgPerM3
                * GRAVITY_M_S2
                * depthM
                / 1000.0;
    }

    public static double absoluteFromGaugeKPa(
            double referenceAbsoluteKPa,
            double gaugeKPa
    ) {
        return Math.max(
                0.0,
                finiteNonNegative(
                        referenceAbsoluteKPa
                )
                        + finiteNonNegative(
                        gaugeKPa
                )
        );
    }

    public static double gaugeKPa(
            double absoluteKPa,
            double referenceAbsoluteKPa
    ) {
        return finiteNonNegative(
                absoluteKPa
        )
                - finiteNonNegative(
                referenceAbsoluteKPa
        );
    }

    public static double differentialKPa(
            double outsideAbsoluteKPa,
            double insideAbsoluteKPa
    ) {
        return finiteNonNegative(
                outsideAbsoluteKPa
        )
                - finiteNonNegative(
                insideAbsoluteKPa
        );
    }

    public static double barToKPa(
            double bar
    ) {
        return finiteNonNegative(
                bar
        )
                * KPA_PER_BAR;
    }

    public static double kPaToBar(
            double kPa
    ) {
        return finiteNonNegative(
                kPa
        )
                / KPA_PER_BAR;
    }

    public static double celsiusToKelvin(
            double celsius
    ) {
        if (!Double.isFinite(
                celsius
        )) {
            return 273.15;
        }

        return Math.max(
                1.0E-6,
                celsius
                        - ABSOLUTE_ZERO_C
        );
    }

    /**
     * Ideal-gas scaling from a known reference density/temperature/pressure.
     */
    public static double gasAbsoluteKPa(
            double massKg,
            double volumeM3,
            double temperatureC,
            double referenceDensityKgPerM3,
            double referenceTemperatureC,
            double referencePressureKPa
    ) {
        if (!Double.isFinite(
                massKg
        )
                || massKg <= 0.0
                || !Double.isFinite(
                volumeM3
        )
                || volumeM3 <= 0.0
                || !Double.isFinite(
                referenceDensityKgPerM3
        )
                || referenceDensityKgPerM3 <= 0.0) {
            return 0.0;
        }

        double density =
                massKg
                        / volumeM3;

        double densityRatio =
                density
                        / referenceDensityKgPerM3;

        double temperatureRatio =
                celsiusToKelvin(
                        temperatureC
                )
                        / celsiusToKelvin(
                        referenceTemperatureC
                );

        return finiteNonNegative(
                referencePressureKPa
        )
                * densityRatio
                * temperatureRatio;
    }

    public static double pressureForceN(
            double differentialKPa,
            double areaM2
    ) {
        if (!Double.isFinite(
                differentialKPa
        )
                || !Double.isFinite(
                areaM2
        )
                || areaM2 <= 0.0) {
            return 0.0;
        }

        return Math.abs(
                differentialKPa
        )
                * 1000.0
                * areaM2;
    }

    /**
     * Thin-wall hoop stress approximation. Useful for cylinders/pipes once
     * geometry is known; material yield/failure belongs to structural physics.
     */
    public static double thinWallHoopStressPa(
            double differentialKPa,
            double radiusM,
            double wallThicknessM
    ) {
        if (!Double.isFinite(
                radiusM
        )
                || radiusM <= 0.0
                || !Double.isFinite(
                wallThicknessM
        )
                || wallThicknessM <= 0.0) {
            return 0.0;
        }

        return Math.abs(
                differentialKPa
        )
                * 1000.0
                * radiusM
                / wallThicknessM;
    }

    /**
     * Shared bounded overload damage curve used by legacy pressure-rated
     * machinery until structural failure consumes real stress directly.
     */
    public static double overloadDamage(
            double actualGaugeKPa,
            double ratedGaugeKPa,
            double integrity
    ) {
        double rating =
                Math.max(
                        0.1,
                        finiteNonNegative(
                                ratedGaugeKPa
                        )
                );

        double over =
                Math.max(
                        0.0,
                        finiteNonNegative(
                                actualGaugeKPa
                        )
                                / rating
                                - 1.0
                );

        double safeIntegrity =
                Math.clamp(
                        Double.isFinite(
                                integrity
                        )
                                ? integrity
                                : 0.0,
                        0.0,
                        1.0
                );

        double weakness =
                1.0
                        + (
                        1.0
                                - safeIntegrity
                )
                        * 0.80;

        return Math.clamp(
                over
                        * over
                        * 0.006
                        * weakness,
                0.0,
                0.08
        );
    }

    private static double finiteNonNegative(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? Math.max(
                        0.0,
                        value
                )
                : 0.0;
    }
}
