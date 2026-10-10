import net.caravidro.wayaround.appearance.PuddleSilhouette;

public final class PuddleSilhouetteTest {
    public static void main(String[] args) {
        long[] seeds = {0, 1, 1234, -7654321, Long.MAX_VALUE};
        boolean different = false;
        int prev = PuddleSilhouette.span(seeds[0], 11);
        for (long seed : seeds) {
            int nonempty = 0, changes = 0, last = -1;
            for (int row = 0; row < PuddleSilhouette.ROWS; row++) {
                int span = PuddleSilhouette.span(seed, row);
                if (span != PuddleSilhouette.span(seed, row)) throw new AssertionError("unstable stencil");
                if (span == 0) continue;
                int left = span >>> 8, right = span & 255;
                if (left < 0 || right > PuddleSilhouette.PIXELS || left >= right)
                    throw new AssertionError("invalid width");
                nonempty++;
                if (last >= 0 && span != last) changes++;
                last = span;
            }
            if (nonempty < 18 || changes < 6) throw new AssertionError("puddle should be continuous and irregular");
            if (PuddleSilhouette.span(seed, 11) != prev) different = true;
        }
        if (!different) throw new AssertionError("unique silhouettes");
        System.out.println("PuddleSilhouette passed: deterministic, non-square, bounded shapes");
    }
}
