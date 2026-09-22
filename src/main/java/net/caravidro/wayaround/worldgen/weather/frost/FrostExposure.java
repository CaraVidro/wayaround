package net.caravidro.wayaround.worldgen.weather.frost;

import java.util.function.Predicate;

/** Snow can blow under a short eave, but cannot pass through walls or glass. */
public final class FrostExposure {
    private FrostExposure() {}
    public record Offset(int x, int y, int z) {}

    public static boolean reachesSky(int dx, int dy, int dz,
                                     Predicate<Offset> clear, Predicate<Offset> sky) {
        if (dy < 0) return false;
        int steps = dy > 0 ? 1 : 12;
        for (int step = 0; step < steps; step++) {
            Offset offset = new Offset(dx * (step + 1), dy > 0 ? 1 : step / 3, dz * (step + 1));
            if (!clear.test(offset)) return false;
            if (sky.test(offset)) return true;
        }
        return false;
    }
}
