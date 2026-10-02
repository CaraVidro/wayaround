package net.caravidro.wayaround.ecology;

/** Pure deterministic animation shared by the event clock and renderer. */
public final class KrakenMotion {
    private KrakenMotion() {}
    public record Point(double x, double y, double z, double radius) {}
    public static int duration(int kind) { return kind == 1 ? 280 : kind >= 3 ? 200 : 340; }
    public static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t*t*(3-2*t); }
    public static double emergence(double age, int kind) {
        int duration = duration(kind);
        return smooth(age / 100.0) * (1 - smooth((age - (duration - 90)) / 90.0));
    }
    public static Point tentacle(double t, double age, int kind) {
        double rise = emergence(age, kind);
        double throwPhase = smooth((age - 170) / 100);
        double angle = throwPhase * 1.62;
        double travel = kind == 2 ? smooth((age - 80) / 150) * 58 : 0;
        // Root leads; the flexible tip follows a travelling bend with increasing lag.
        double bend = Math.sin(age * 0.045 - t * 4.5) * 11 * t*t * rise;
        double curl = Math.sin(t * Math.PI * 1.5) * 18 * t * rise;
        double length = 250 * rise;
        double x = travel + Math.sin(angle) * length * t*t + bend + curl;
        double y = -24 + Math.cos(angle) * length * t - throwPhase * 22 * t*t;
        double z = Math.cos(age * 0.034 - t * 4) * 8 * t*t * rise;
        double radius = 0.30 + 7.6 * Math.pow(1-t, 1.25);
        return new Point(x, y, z, radius);
    }
}
