package net.caravidro.wayaround.worldgen.weather.cold;

/** One step is one second of game time, independent of wall-clock time. */
public final class ColdExposure {
    public static final double SLOWNESS_THRESHOLD = 30;
    public static final double FROSTBITE_THRESHOLD = 60;
    public static final double MAX_COLD = 120;
    public static final int RECOVERY_SECONDS = 180;

    public record State(double cold, double tremor) {
        public static final State WARM = new State(0, 0);
    }

    private ColdExposure() {}

    public static State step(State previous, boolean exposed, double night, double storm) {
        double cold = finiteClamp(previous.cold(), MAX_COLD);
        double tremor = Math.max(0, finiteClamp(previous.tremor(), 1) - 1.0 / RECOVERY_SECONDS);
        if (exposed) {
            // Full storm: frostbite after ~40s by day, ~30s at night.
            // Clear night: ~100s. Clear day: ~500s.
            cold = Math.min(MAX_COLD, cold + 0.12 + 0.48 * finiteClamp(night, 1)
                    + 1.4 * finiteClamp(storm, 1));
            if (cold >= 10) tremor = Math.max(tremor, Math.min(1, 0.2 + (cold - 10) / 100));
        } else {
            cold = Math.max(0, cold - 1);
        }
        return new State(cold, tremor);
    }

    public static State stepTemperature(
            State previous,
            boolean exposed,
            double temperatureCelsius
    ) {
        double cold = finiteClamp(
                previous.cold(),
                MAX_COLD
        );

        double tremor = Math.max(
                0,
                finiteClamp(
                        previous.tremor(),
                        1
                )
                        - 1.0 / RECOVERY_SECONDS
        );

        if (exposed) {
            double severity =
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    (5.0 - temperatureCelsius)
                                            / 60.0
                            )
                    );

            cold =
                    Math.min(
                            MAX_COLD,
                            cold
                                    + 0.04
                                    + severity * 1.90
                    );

            if (cold >= 10) {
                tremor =
                        Math.max(
                                tremor,
                                Math.min(
                                        1,
                                        0.2
                                                + (cold - 10)
                                                / 100
                                )
                        );
            }
        } else {
            cold =
                    Math.max(
                            0,
                            cold - 1
                    );
        }

        return new State(
                cold,
                tremor
        );
    }

    public static double nightFactor(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        if (time >= 11500 && time < 14500) return smoothstep((time - 11500) / 3000.0);
        if (time >= 14500 && time <= 21500) return 1;
        if (time > 21500 && time <= 23500) return 1 - smoothstep((time - 21500) / 2000.0);
        return 0;
    }

    private static double smoothstep(double value) {
        return value * value * (3 - 2 * value);
    }

    private static double finiteClamp(double value, double maximum) {
        return Double.isFinite(value) ? Math.max(0, Math.min(maximum, value)) : 0;
    }
}
