package net.caravidro.wayaround.worldgen.weather.frost;

/** Three bits per face, keeping accumulation and washing independent on each side. */
public final class FrostLayers {
    private FrostLayers() {}
    public static int get(int mask, int face) { return (mask >>> (face * 3)) & 7; }
    public static int clear(int mask, int face) { return mask & ~(7 << (face * 3)); }
    public static int add(int mask, int face) {
        return clear(mask, face) | (Math.min(4, get(mask, face) + 1) << (face * 3));
    }
}
