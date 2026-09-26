package net.caravidro.wayaround.thermal;

/** Gameplay degrees; rates are probabilities per sampled exposure, not real thermodynamics. */
public final class TemperatureCurve {
    public static final double AMBIENT = 20, MAX = 3200;
    private TemperatureCurve() {}
    public static double chance(double temperature, double threshold, double scale, double cap) {
        if (!Double.isFinite(temperature) || temperature <= threshold) return 0;
        return Math.min(cap, Math.expm1(Math.min(8, (temperature - threshold) / scale)) * 0.002);
    }
    public static double cool(double temperature, long elapsedTicks) {
        return AMBIENT + Math.max(0, temperature - AMBIENT) * Math.exp(-Math.max(0, elapsedTicks) / 240.0);
    }
}
