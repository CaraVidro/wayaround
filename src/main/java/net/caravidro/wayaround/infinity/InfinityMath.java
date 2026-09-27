package net.caravidro.wayaround.infinity;

/** Deterministic balance rules, independent of client/server and projectile implementation. */
public final class InfinityMath {
    private InfinityMath() {}
    public static float meleeFraction(int learnedSteps) {
        return Math.min(15, Math.max(0, learnedSteps)) * 0.05F;
    }
    public static double slowedSpeed(double speed, double gap) {
        if (!Double.isFinite(speed) || !Double.isFinite(gap) || speed <= 0 || gap <= 0) return 0;
        return Math.min(speed * (speed > 3.0 ? 0.20 : 0.86), gap * 0.28);
    }
}
