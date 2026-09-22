package net.caravidro.wayaround.worldgen.weather;

public final class BlizzardWindTest {
    public static void main(String[] args) {
        for (int tick = 0; tick < 240_000; tick += 17) {
            require(BlizzardWind.speed(tick, 0) == 0, "Calm weather must not blow particles");
            double speed = BlizzardWind.speed(tick, 1);
            require(speed >= 0.30 && speed <= 0.480001, "Gusts stay bounded through ten days");
            require(BlizzardWind.speed(tick, 3) == speed, "Out-of-range intensity is clamped");
            double angle = BlizzardWind.angle(tick);
            require(Math.abs(Math.hypot(Math.cos(angle), Math.sin(angle)) - 1) < 1e-12,
                    "Emitters and renderers use a unit direction");
        }

        double drift = 0;
        for (int tick = 0; tick < 20_000; tick++) {
            drift = BlizzardWind.response(drift, 0.48);
            require(drift >= 0 && drift <= 0.48, "Long-lived particles must never accelerate without bound");
        }
        require(Math.abs(drift - 0.48) < 1e-12, "Wind reaches its target speed");
        for (int tick = 0; tick < 100; tick++) drift = BlizzardWind.response(drift, 0);
        require(drift < 1e-8, "Particles settle after gusts end");
        require(BlizzardWind.precipitationOffset(0, 0.4) == 0, "Precipitation stays anchored to the surface");
        require(BlizzardWind.precipitationOffset(10, 0.4) < 0, "Upper snow is upstream of its landing point");
        require(BlizzardWind.precipitationOffset(-8, 0.4) == 0, "No tilt below the ground");
        require(BlizzardWind.precipitationOffset(1000, 0.4)
                == BlizzardWind.precipitationOffset(16, 0.4), "High columns have bounded skew");
        System.out.println("Blizzard wind regression checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
