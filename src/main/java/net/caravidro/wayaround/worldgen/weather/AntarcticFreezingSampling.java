package net.caravidro.wayaround.worldgen.weather;

/** Visits every column in a ticking chunk without a long random wait for small pools. */
public final class AntarcticFreezingSampling {
    public static final int COLUMNS = 16 * 16;

    private AntarcticFreezingSampling() {}

    public static int column(long gameTime, int chunkX, int chunkZ) {
        int offset = chunkX * 73428767 ^ chunkZ * 912931;
        // An odd stride permutes all 256 columns; neighboring chunks start elsewhere.
        return ((int) gameTime * 73 + offset) & (COLUMNS - 1);
    }
}
