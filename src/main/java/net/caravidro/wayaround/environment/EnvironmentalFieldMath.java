package net.caravidro.wayaround.environment;

/**
 * Dependency-free regional environmental math.
 */
public final class EnvironmentalFieldMath {

    private EnvironmentalFieldMath() {
    }

    public static double evaporation(
            double waterAvailability,
            double humidity,
            double temperatureC,
            double windSpeedMPerS
    ) {
        double water =
                unit(
                        waterAvailability
                );

        double dry =
                1.0
                        - unit(
                        humidity
                );

        double warmth =
                Math.clamp(
                        (
                                temperatureC
                                        + 5.0
                        )
                                / 40.0,
                        0.0,
                        1.25
                );

        double wind =
                Math.clamp(
                        0.45
                                + Math.max(
                                0.0,
                                finite(
                                        windSpeedMPerS
                                )
                        )
                                        * 0.10,
                        0.45,
                        1.75
                );

        return Math.clamp(
                water
                        * dry
                        * warmth
                        * wind
                        * 0.018,
                0.0,
                0.025
        );
    }

    public static double saturationThreshold(
            double temperatureC
    ) {
        return Math.clamp(
                0.50
                        + (
                        temperatureC
                                + 5.0
                )
                        * 0.008,
                0.42,
                0.88
        );
    }

    public static double condensation(
            double humidity,
            double cloudWater,
            double temperatureC
    ) {
        double excess =
                Math.max(
                        0.0,
                        unit(
                                humidity
                        )
                                - saturationThreshold(
                                temperatureC
                        )
                );

        double capacity =
                1.0
                        - unit(
                        cloudWater
                );

        return Math.clamp(
                excess
                        * capacity
                        * 0.12,
                0.0,
                0.035
        );
    }

    public static double precipitation(
            double rainIntensity,
            double cloudWater
    ) {
        return Math.min(
                unit(
                        cloudWater
                ),
                unit(
                        rainIntensity
                )
                        * 0.030
        );
    }

    public static double soilDrying(
            double temperatureC,
            double windSpeedMPerS,
            double humidity
    ) {
        double warmth =
                Math.clamp(
                        (
                                temperatureC
                                        + 8.0
                        )
                                / 42.0,
                        0.0,
                        1.4
                );

        double wind =
                Math.clamp(
                        Math.max(
                                0.0,
                                finite(
                                        windSpeedMPerS
                                )
                        )
                                / 12.0,
                        0.0,
                        1.0
                );

        return Math.clamp(
                (
                        0.0005
                                + warmth
                                        * 0.0012
                                + wind
                                        * 0.0008
                )
                        * (
                        0.35
                                + (
                                1.0
                                        - unit(
                                        humidity
                                )
                        )
                                * 0.65
                ),
                0.0002,
                0.0035
        );
    }

    public static double advectionFraction(
            double speedMPerS
    ) {
        return Math.clamp(
                Math.max(
                        0.0,
                        finite(
                                speedMPerS
                        )
                )
                        / 18.0
                        * 0.10,
                0.0,
                0.12
        );
    }

    public static double rainWashFraction(
            double rainIntensity
    ) {
        return Math.clamp(
                unit(
                        rainIntensity
                )
                        * 0.16,
                0.0,
                0.16
        );
    }

    public static float unitF(
            double value
    ) {
        return (float) unit(
                value
        );
    }

    private static double unit(
            double value
    ) {
        return Math.clamp(
                finite(
                        value
                ),
                0.0,
                1.0
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
