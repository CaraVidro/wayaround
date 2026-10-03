package net.caravidro.wayaround.client;

/** Section-level distance bands with hysteresis to avoid repeated mesh rebuilds. */
public final class VegetationLodMath {
    private VegetationLodMath() {}
    public static int tier(double squaredDistance, int previous) {
        if (previous < 0) return squaredDistance < 48 * 48 ? 0 : squaredDistance < 80 * 80 ? 1 : 2;
        if (previous == 0 && squaredDistance <= 56 * 56) return 0;
        if (previous == 2 && squaredDistance >= 72 * 72) return 2;
        if (squaredDistance < 40 * 40) return 0;
        if (squaredDistance > 88 * 88) return 2;
        return 1;
    }
}
