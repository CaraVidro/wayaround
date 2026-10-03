package net.caravidro.wayaround.daybreak;

/** Pure calendar and bounded visual growth, independent of client/server registries. */
public final class DaysBreakMath {
    private DaysBreakMath() {}
    public static boolean day(long time) { return Math.floorMod(time,24000)<12000; }
    public static float sunScale(double elapsed) { return (float)Math.min(12,Math.pow(2,Math.max(0,elapsed)/1200)); }
    public static int slowLevel(int exposure) { return Math.min(4,Math.max(0,exposure/3)); }
    public static float damage(int exposure) { return 1f+Math.min(20,Math.max(0,exposure))*.35f; }
}
