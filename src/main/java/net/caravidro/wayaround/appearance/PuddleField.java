package net.caravidro.wayaround.appearance;

/**
 * World-space implicit decal shape, inspired by Blender's coordinate rotation,
 * fBm noise and domain warping. One anchor creates ONE puddle that can span
 * multiple horizontal block faces without resetting its pattern at their UVs.
 *
 * All math is Minecraft-independent for fast deterministic CI tests.
 */
public final class PuddleField {
    private PuddleField() {}

    /** The renderer queries a 3x3 block footprint; all shapes fit inside it. */
    public static final int BLOCK_REACH = 1;
    public static final int RESOLUTION = 6;

    public record Shape(double x, double z, double cosine, double sine,
                        double radiusX, double radiusZ, long seed) {}

    public static Shape describe(long seed, int blockX, int blockZ, float wetness) {
        long a = mix(seed ^ 0x8A5CD789635D2DFFL);
        long b = mix(a ^ 0x6381ABDDC4B18521L);
        double theta = fraction(a >>> 3) * Math.PI * 2.0D;
        // Different position, orientation, aspect ratio and size for every puddle.
        double growth = .42D + .58D * Math.min(1D, wetness * 1.65D);
        return new Shape(blockX + .5D + (fraction(b >>> 10) - .5D) * .12D,
                blockZ + .5D + (fraction(a >>> 15) - .5D) * .12D,
                Math.cos(theta), Math.sin(theta),
                (.79D + fraction(b >>> 22) * .30D) * growth,
                (.56D + fraction(a >>> 26) * .31D) * growth, seed);
    }

    /** >0 is wet; zero is its boundary. Continuous across integer block edges. */
    public static double density(Shape shape, double worldX, double worldZ) {
        double dx = worldX - shape.x(), dz = worldZ - shape.z();
        if (Math.abs(dx) > 1.50D || Math.abs(dz) > 1.50D) return -2D;
        double u = (dx * shape.cosine() + dz * shape.sine()) / shape.radiusX();
        double v = (-dx * shape.sine() + dz * shape.cosine()) / shape.radiusZ();

        // Low-frequency deformation + a weaker second octave.
        // Off-grid coordinate offsets avoid the constant-at-integers issue.
        double n1 = valueNoise(u * 1.85D + .39D, v * 1.85D - .71D, shape.seed());
        double n2 = valueNoise(u * 4.60D - .53D, v * 4.60D + .17D,
                shape.seed() ^ 0xD1B54A32D192ED03L);
        double distortedU = u + n1 * .10D;
        double distortedV = v + n2 * .08D;
        double radius = Math.sqrt(distortedU * distortedU + distortedV * distortedV);

        // Subtle irregular lobes, using angular harmonics rather than a fixed ellipse.
        double angle = Math.atan2(v, u);
        double lobe = Math.sin(angle * 3D + fraction(shape.seed()) * 6.28318D) * .075D
                + Math.cos(angle * 5D + fraction(shape.seed() >>> 16) * 6.28318D) * .038D;
        return 1D - radius + n1 * .125D + n2 * .062D + lobe;
    }

    public static double angle(Shape s) {
        return Math.atan2(s.sine(), s.cosine());
    }

    /** Lattice-value noise with C1-continuous cubic smoothstep interpolation. */
    public static double valueNoise(double x, double z, long seed) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = fade(x - ix), fz = fade(z - iz);
        double a = lattice(ix, iz, seed);
        double b = lattice(ix + 1, iz, seed);
        double c = lattice(ix, iz + 1, seed);
        double d = lattice(ix + 1, iz + 1, seed);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    private static double fade(double t) { return t * t * (3D - 2D * t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double lattice(int x, int z, long seed) {
        return fraction(mix(seed ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) z * 0xD1B54A32D192ED03L))) * 2D - 1D;
    }

    private static double fraction(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    public static long mix(long n) {
        n ^= n >>> 30;
        n *= 0xBF58476D1CE4E5B9L;
        n ^= n >>> 27;
        n *= 0x94D049BB133111EBL;
        return n ^ (n >>> 31);
    }
}
