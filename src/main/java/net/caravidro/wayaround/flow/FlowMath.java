package net.caravidro.wayaround.flow;

/**
 * Dependency-free shared flow math.
 *
 * <p>Macroscopic WayAround physics uses metres, seconds, kilograms, kPa and
 * cubic metres. Legacy Minecraft fluid throughput is converted at adapters:
 * one bucket (1000 mB) is treated as one cubic metre.</p>
 */
public final class FlowMath {

    public static final double TICKS_PER_SECOND =
            20.0;

    public static final double MILLIBUCKETS_PER_M3 =
            1000.0;

    private static final double MAX_ORIFICE_SPEED_M_S =
            80.0;

    private FlowMath() {
    }

    public static double blocksPerTickToMPerS(
            double blocksPerTick
    ) {
        return finite(
                blocksPerTick
        )
                * TICKS_PER_SECOND;
    }

    public static double mPerSecondToBlocksPerTick(
            double metresPerSecond
    ) {
        return finite(
                metresPerSecond
        )
                / TICKS_PER_SECOND;
    }

    public static double minecraftFluidRateM3PerS(
            double milliBucketsPerTick
    ) {
        return Math.max(
                0.0,
                finite(
                        milliBucketsPerTick
                )
        )
                / MILLIBUCKETS_PER_M3
                * TICKS_PER_SECOND;
    }

    public static double minecraftFluidRateMbPerTick(
            double cubicMetresPerSecond
    ) {
        return Math.max(
                0.0,
                finite(
                        cubicMetresPerSecond
                )
        )
                * MILLIBUCKETS_PER_M3
                / TICKS_PER_SECOND;
    }

    public static double velocityFromVolumetricRateMPerS(
            double volumetricRateM3PerS,
            double crossSectionM2
    ) {
        if (!Double.isFinite(
                crossSectionM2
        )
                || crossSectionM2 <= 0.0) {
            return 0.0;
        }

        return Math.max(
                0.0,
                finite(
                        volumetricRateM3PerS
                )
        )
                / crossSectionM2;
    }

    public static double volumetricRateM3PerS(
            double velocityMPerS,
            double crossSectionM2
    ) {
        if (!Double.isFinite(
                crossSectionM2
        )
                || crossSectionM2 <= 0.0) {
            return 0.0;
        }

        return Math.max(
                0.0,
                finite(
                        velocityMPerS
                )
        )
                * crossSectionM2;
    }

    public static double massRateKgPerS(
            double volumetricRateM3PerS,
            double densityKgPerM3
    ) {
        return Math.max(
                0.0,
                finite(
                        volumetricRateM3PerS
                )
        )
                * Math.max(
                0.0,
                finite(
                        densityKgPerM3
                )
        );
    }

    public static double dynamicPressureKPa(
            double densityKgPerM3,
            double velocityMPerS
    ) {
        double density =
                Math.max(
                        0.0,
                        finite(
                                densityKgPerM3
                        )
                );

        double speed =
                Math.max(
                        0.0,
                        Math.abs(
                                finite(
                                        velocityMPerS
                                )
                        )
                );

        return 0.5
                * density
                * speed
                * speed
                / 1000.0;
    }

    /**
     * Bernoulli-style velocity from pressure drop with a coarse loss
     * coefficient. Different solvers are free to supply different losses.
     */
    public static double pressureDrivenVelocityMPerS(
            double differentialKPa,
            double densityKgPerM3,
            double lossCoefficient
    ) {
        double density =
                Math.max(
                        1.0E-9,
                        finite(
                                densityKgPerM3
                        )
                );

        double loss =
                Math.max(
                        1.0,
                        finite(
                                lossCoefficient
                        )
                );

        double deltaPa =
                Math.abs(
                        finite(
                                differentialKPa
                        )
                )
                        * 1000.0;

        double velocity =
                Math.sqrt(
                        2.0
                                * deltaPa
                                / (
                                density
                                        * loss
                        )
                );

        return Math.min(
                MAX_ORIFICE_SPEED_M_S,
                velocity
        );
    }

    /**
     * Signed orifice flow. Positive means from the first pressure toward the
     * second; negative means the reverse.
     */
    public static double signedOrificeRateM3PerS(
            double firstPressureKPa,
            double secondPressureKPa,
            double densityKgPerM3,
            double openingAreaM2,
            double dischargeCoefficient
    ) {
        if (!Double.isFinite(
                openingAreaM2
        )
                || openingAreaM2 <= 0.0) {
            return 0.0;
        }

        double delta =
                finite(
                        firstPressureKPa
                )
                        - finite(
                        secondPressureKPa
                );

        if (Math.abs(
                delta
        ) < 1.0E-9) {
            return 0.0;
        }

        double coefficient =
                Math.clamp(
                        Double.isFinite(
                                dischargeCoefficient
                        )
                                ? dischargeCoefficient
                                : 0.0,
                        0.0,
                        1.0
                );

        double speed =
                pressureDrivenVelocityMPerS(
                        delta,
                        densityKgPerM3,
                        1.0
                );

        return Math.copySign(
                coefficient
                        * openingAreaM2
                        * speed,
                delta
        );
    }

    public static double pressureDropKPa(
            double densityKgPerM3,
            double velocityMPerS,
            double lossCoefficient
    ) {
        double dynamic =
                dynamicPressureKPa(
                        densityKgPerM3,
                        velocityMPerS
                );

        return dynamic
                * Math.max(
                0.0,
                finite(
                        lossCoefficient
                )
        );
    }

    private static double finite(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? value
                : 0.0;
    }
}
