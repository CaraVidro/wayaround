package net.caravidro.wayaround.worldgen.weather;

/** Shared direction and bounded gusts for server snow emitters and client visuals. */
public final class BlizzardWind {
    private BlizzardWind() {}

    public static double angle(double time) {
        return 0.7 + Math.sin(time / 5100.0) * 0.55 + Math.sin(time / 1700.0) * 0.17;
    }

    public static double speed(double time, double intensity) {
        double gust = 0.82 + Math.sin(time / 47.0) * 0.12 + Math.sin(time / 113.0) * 0.06;
        return Math.clamp(intensity, 0, 1) * 0.48 * gust;
    }

    public static double response(double previous, double target) {
        return previous + (target - previous) * 0.18;
    }

    /** Top of a falling column is upstream; its bottom stays at the original surface. */
    public static double precipitationOffset(double heightAboveGround, double windComponent) {
        return -Math.clamp(heightAboveGround, 0, 16) * windComponent * 2.4;
    }
}
