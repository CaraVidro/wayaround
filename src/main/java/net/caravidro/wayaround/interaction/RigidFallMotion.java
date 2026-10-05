package net.caravidro.wayaround.interaction;

/** Shared analytical motion: no per-block physics and identical client/server landing. */
public final class RigidFallMotion {
    private RigidFallMotion() {}
    public static double calvingDrop(double ticks, double distance) {
        double t = Math.max(0, ticks);
        int whole = (int) t;
        int accelerating = Math.min(whole, 21);
        double drop = .038 * accelerating * (accelerating + 1) / 2.0
                + Math.max(0, whole - 21) * .82;
        return Math.min(distance, drop + (t - whole) * Math.min(.82, .038 * (whole + 1)));
    }
    public static int calvingTicks(int distance) {
        for (int tick = 1; tick <= 660; tick++)
            if (calvingDrop(tick, distance) >= distance) return tick;
        throw new IllegalArgumentException("Calving fall exceeds bounded world height");
    }
    public static double drift(double drop, double distance) {
        if (distance <= 0) return 1;
        double t = Math.max(0, Math.min(1, drop / distance));
        return t * t * (3 - 2 * t);
    }
}
