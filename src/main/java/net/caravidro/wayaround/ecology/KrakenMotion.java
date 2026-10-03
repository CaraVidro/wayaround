package net.caravidro.wayaround.ecology;

/** Pure deterministic animation shared by the event clock and renderer. */
public final class KrakenMotion {
    private KrakenMotion() {}
    public record Point(double x, double y, double z, double radius) {}
    /** The packet carries the top water block, not its visible upper surface. */
    public static double waterSurface(int topWaterBlock) { return topWaterBlock + 1.02; }
    public static int duration(int kind) { return kind == 5 ? 300 : kind == 1 ? 280 : kind >= 3 ? 200 : 340; }
    public static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t*t*(3-2*t); }
    public static double emergence(double age, int kind) {
        int duration = duration(kind);
        return smooth(age / 100.0) * (1 - smooth((age - (duration - 90)) / 90.0));
    }
    public static int breachAge(int kind) {
        for (int age=1; age<duration(kind)/2; age++) {
            double y=kind==1 ? -36+84*emergence(age,1) : tentacle(1,age,kind).y();
            if (y>=0) return age;
        }
        return 24;
    }
    public static int impactAge(int kind) {
        for (int age=duration(kind)/2; age<=duration(kind); age++) {
            double y=kind==1 ? -36+84*emergence(age,1) : tentacle(1,age,kind).y();
            if (y<=0) return age;
        }
        return duration(kind)-1;
    }
    public static Point tentacle(double t, double age, int kind) {
        double rise = smooth(age / 100.0);
        double sink = smooth((age - 270) / 70.0);
        double throwPhase = smooth((age - 170) / 100);
        double angle = throwPhase * 1.62;
        double travel = kind == 2 ? smooth((age - 80) / 150) * 58 : 0;
        // Root leads; the flexible tip follows a travelling bend with increasing lag.
        double bend = Math.sin(age * 0.045 - t * 4.5) * 11 * t*t * rise;
        double curl = Math.sin(t * Math.PI * 1.5) * 18 * t * rise;
        double length = 250 * rise;
        double x = travel + Math.sin(angle) * length * t*t + bend + curl;
        double y = -24 + throwPhase * 24 - sink * 90
                + Math.cos(angle) * length * t - throwPhase * 6 * t*t;
        double z = Math.cos(age * 0.034 - t * 4) * 8 * t*t * rise;
        double radius = 0.30 + 7.6 * Math.pow(1-t, 1.25);
        return new Point(x, y, z, radius);
    }
}
