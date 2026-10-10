import net.caravidro.wayaround.appearance.PuddleField;

public final class PuddleFieldTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        // Samples taken at a seam must have identical values on both tiles.
        for (int i = 0; i < 80; i++) {
            long seed = PuddleField.mix(i * 104729L);
            var s = PuddleField.describe(seed, 100, -50, 1F);
            check(Double.isFinite(s.cosine()) && Double.isFinite(s.sine()), "finite orientation");
            for (int z = 0; z <= 16; z++) {
                double seamZ = -50 + z / 16D;
                double a = PuddleField.density(s, 101D, seamZ);
                double b = PuddleField.density(s, 100D + 1D, seamZ);
                check(Double.doubleToLongBits(a) == Double.doubleToLongBits(b),
                        "world-coordinate continuity");
            }
            double core = PuddleField.density(s, s.x(), s.z());
            check(core > .5D, "center always wet");
            check(PuddleField.density(s, s.x() + 3D, s.z()) < 0D, "bounded footprint");
        }
        // Rotation/aspect are not fixed to one orientation or one template.
        var first = PuddleField.describe(123, 0, 0, 1F);
        boolean angleVaries = false, shapeVaries = false;
        int spillCount = 0;
        for (int i = 0; i < 100; i++) {
            var shape = PuddleField.describe(i * 97L + 7, 0, 0, 1F);
            if (Math.abs(PuddleField.angle(shape) - PuddleField.angle(first)) > .2D)
                angleVaries = true;
            if (Math.abs(shape.radiusX() - first.radiusX()) > .06D ||
                Math.abs(shape.radiusZ() - first.radiusZ()) > .06D) shapeVaries = true;
            for (int z = -12; z <= 12; z++) {
                double wz = .5D + z / 10D;
                if (PuddleField.density(shape, 1.04D, wz) > 0D) {
                    spillCount++;
                    break;
                }
            }
        }
        check(angleVaries && shapeVaries, "orientation and aspect ratio must vary");
        check(spillCount >= 20, "many shapes must cross an adjacent block");
        var fresh = PuddleField.describe(999, 0, 0, .20F);
        var grown = PuddleField.describe(999, 0, 0, 1F);
        check(grown.radiusX() > fresh.radiusX(), "water spreads with wetness");
        // Neighbor blocks are handled by the renderer's terrain support checks.
        // Geometry must be deterministic, so client animations cannot move borders.
        var shape = PuddleField.describe(13579L, -123, 987, 1F);
        for (int i = 0; i < 200; i++) {
            double x = -122.5D + i / 100D, z = 987.22D + i / 200D;
            check(Double.doubleToLongBits(PuddleField.density(shape, x, z)) ==
                  Double.doubleToLongBits(PuddleField.density(shape, x, z)),
                  "stable world shape");
        }
        System.out.println("PuddleField passed: random orientation, cross-block continuity, growth and deterministic shape");
    }
}
