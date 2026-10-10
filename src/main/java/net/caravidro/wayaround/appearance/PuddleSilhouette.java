package net.caravidro.wayaround.appearance;

/**
 * A procedural 32-pixel-wide stencil, designed to look like one authored
 * pixel-art puddle, not a collection of visible tiled square decals.
 * Each horizontal scanline has an irregular, stepped boundary and a single
 * filled span. Shapes are deterministic per block and require no PNG uploads.
 *
 * Standalone geometry math: no Minecraft dependencies; unit-testable via javac.
 */
public final class PuddleSilhouette {
    public static final int PIXELS = 32;
    public static final int ROWS = 24;
    private PuddleSilhouette() {}

    /** Packed [left,right) in 32nds of a block; 0 when the row is empty. */
    public static int span(long block, int row) {
        if (row < 0 || row >= ROWS) return 0;
        int seed = hash(block ^ 0x623A927C6124D9ABL);
        int noise = hash(block + (long) row * 0x9E3779B97F4A7C15L);
        double t = (row + 0.5D - ROWS / 2.0D) / (ROWS / 2.0D);
        double profile = Math.sqrt(Math.max(0D, 1D - t * t));
        // Two asymmetrical lobes give the outline a hand-painted silhouette.
        double wave = Math.sin(t * 5.2D + (seed & 255) * .015D) * 1.8D;
        int lean = (int) Math.round(t * (((seed >>> 8) & 7) - 3) * .50D);
        int cx = 16 + (((seed >>> 12) & 7) - 3) / 2 + lean + (int) Math.round(wave);
        double width = (10.5D + ((seed >>> 19) & 3))
                * profile * (0.88D + ((noise >>> 16) & 15) / 100D);
        int left = clamp((int) Math.floor(cx - width)
                + (((noise >>> 5) & 3) == 0 ? -1 : 0), 2, 29);
        int right = clamp((int) Math.ceil(cx + width)
                + (((noise >>> 10) & 3) == 0 ? 1 : 0), 3, 30);
        return right <= left ? 0 : (left << 8) | right;
    }

    public static int hash(long x) {
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        return (int) (x ^ (x >>> 31));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
