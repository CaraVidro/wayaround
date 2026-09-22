package net.caravidro.wayaround.industrial.power;

/** The server-side solar model, independent of client fog and view-distance settings. */
public final class SolarPower {
    public static final int PEAK_FE_PER_TICK = 64;
    public static final double POLAR_LATITUDE_Z = 30_000.0;

    private SolarPower() {}

    public static double latitudeEfficiency(double z) {
        double latitude = Math.min(1.0, Math.abs(z) / POLAR_LATITUDE_Z);
        return 0.18 + 0.82 * Math.cos(latitude * Math.PI * 0.5);
    }

    public static double generation(long dayTime, double z, double rain, double thunder,
                                    double fog, boolean exposedToSun) {
        if (!exposedToSun) return 0;
        long time = Math.floorMod(dayTime, 24_000L);
        if (time >= 12_000L) return 0;
        double sunlight = Math.sin(Math.PI * time / 12_000.0);
        double weather = (1.0 - 0.55 * clamp(rain)) * (1.0 - 0.35 * clamp(thunder));
        double visibility = 1.0 - 0.9 * clamp(fog);
        return PEAK_FE_PER_TICK * sunlight * latitudeEfficiency(z) * weather * visibility;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
