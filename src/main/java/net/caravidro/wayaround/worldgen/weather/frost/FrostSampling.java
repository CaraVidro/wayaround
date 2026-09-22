package net.caravidro.wayaround.worldgen.weather.frost;

/** A bounded sweep visits every nearby column instead of relying on random hits. */
public final class FrostSampling {
    public static final int INTERVAL = 5;
    public static final int WIDTH = 49;
    public static final int BATCH = 80;
    private FrostSampling() {}
    public static int column(long ticks, int index) {
        return (int) Math.floorMod(ticks / INTERVAL * BATCH + index, (long) WIDTH * WIDTH);
    }
}
