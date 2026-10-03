package net.caravidro.wayaround.ecology;
/** Pure, bounded pressure exposure model: three minutes in unsafe depths. */
public final class OceanPressure {
    public static final int WARNING = 1800, CRITICAL = 3000, FAILURE = 3600;
    private OceanPressure() {}
    public static int advance(int exposure, double depth, boolean water, boolean capsule) {
        return water && depth >= (capsule ? 70 : 85)
                ? Math.min(FAILURE, exposure + 1) : Math.max(0, exposure - 4);
    }
}
