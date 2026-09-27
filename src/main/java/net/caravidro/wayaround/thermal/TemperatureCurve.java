package net.caravidro.wayaround.thermal;

/** Gameplay degrees; rates are probabilities per sampled exposure, not real thermodynamics. */
public final class TemperatureCurve {
    public static final double AMBIENT = 20;
    public static final double MIN = -120;
    public static final double MAX = 3200;

    private TemperatureCurve() {}

    public static double chance(double temperature, double threshold, double scale, double cap) {
        if (!Double.isFinite(temperature) || temperature <= threshold) return 0;
        return Math.min(cap, Math.expm1(Math.min(8, (temperature - threshold) / scale)) * 0.002);
    }

    public static double relax(
            double temperature,
            double ambient,
            long elapsedTicks
    ) {
        double safeAmbient =
                clamp(
                        ambient
                );

        if (!Double.isFinite(
                temperature
        )) {
            return safeAmbient;
        }

        double factor =
                Math.exp(
                        -Math.max(
                                0L,
                                elapsedTicks
                        )
                                / 240.0
                );

        return clamp(
                safeAmbient
                        + (temperature - safeAmbient)
                        * factor
        );
    }

    /** Compatibility helper for older heat-only callers. */
    public static double cool(double temperature, long elapsedTicks) {
        return relax(
                temperature,
                AMBIENT,
                elapsedTicks
        );
    }

    public static double clamp(
            double temperature
    ) {
        if (!Double.isFinite(
                temperature
        )) {
            return AMBIENT;
        }

        return Math.max(
                MIN,
                Math.min(
                        MAX,
                        temperature
                )
        );
    }
}
