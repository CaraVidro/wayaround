package net.caravidro.wayaround.worldgen.water.wave;

public final class OceanWaveFieldTest {

    public static void main(String[] args) {
        OceanWaveField.Profile coast =
                new OceanWaveField.Profile(
                        0.24F,
                        0.86F,
                        0.0F,
                        0.0F,
                        0.16F,
                        11.0F,
                        2.4F,
                        1.0,
                        0.0,
                        1.0,
                        0.0
                );

        OceanWaveField.Profile ocean =
                new OceanWaveField.Profile(
                        1.0F,
                        0.04F,
                        0.0F,
                        0.0F,
                        0.90F,
                        43.0F,
                        0.95F,
                        1.0,
                        0.0,
                        0.0,
                        0.0
                );

        OceanWaveField.Profile weatherTaggedCoast =
                new OceanWaveField.Profile(
                        0.24F,
                        0.86F,
                        1.0F,
                        1.0F,
                        0.16F,
                        11.0F,
                        2.4F,
                        1.0,
                        0.0,
                        1.0,
                        0.0
                );

        long time =
                18_400L;

        double x =
                218.75;

        double z =
                -91.25;

        OceanWaveField.Sample a =
                OceanWaveField.sample(
                        ocean,
                        x,
                        z,
                        time
                );

        OceanWaveField.Sample b =
                OceanWaveField.sample(
                        ocean,
                        x,
                        z,
                        time
                );

        require(
                Double.doubleToLongBits(
                        a.height()
                ) == Double.doubleToLongBits(
                        b.height()
                ),
                "Same coordinate/time/profile must produce the same wave"
        );

        require(
                Math.abs(
                        a.normal()
                                .length()
                                - 1.0
                ) < 1.0E-9,
                "Wave normal must stay normalized"
        );

        require(
                Double.isFinite(
                        a.height()
                )
                        && Double.isFinite(
                        a.verticalVelocity()
                )
                        && Double.isFinite(
                        a.horizontalVelocity()
                                .length()
                ),
                "Wave sample must stay finite"
        );

        OceanWaveField.Sample later =
                OceanWaveField.sample(
                        ocean,
                        x,
                        z,
                        time + 40L
                );

        require(
                Math.abs(
                        later.height()
                                - a.height()
                ) > 1.0E-5,
                "Wave field must evolve with time"
        );

        OceanWaveField.Profile quietSameShape =
                new OceanWaveField.Profile(
                        ocean.exposure(),
                        ocean.shore(),
                        ocean.rain(),
                        ocean.storm(),
                        0.30F,
                        ocean.wavelength(),
                        ocean.maxRunup(),
                        ocean.directionX(),
                        ocean.directionZ(),
                        ocean.shoreX(),
                        ocean.shoreZ()
                );

        OceanWaveField.Sample quiet =
                OceanWaveField.sample(
                        quietSameShape,
                        x,
                        z,
                        time
                );

        double loudEnergy =
                0.0;

        double quietEnergy =
                0.0;

        for (int i = 0;
             i < 48;
             i++) {
            double sampleX =
                    x + i * 1.73;

            double sampleZ =
                    z - i * 0.91;

            loudEnergy +=
                    Math.abs(
                            OceanWaveField.sample(
                                    ocean,
                                    sampleX,
                                    sampleZ,
                                    time + i * 7L
                            ).height()
                    );

            quietEnergy +=
                    Math.abs(
                            OceanWaveField.sample(
                                    quietSameShape,
                                    sampleX,
                                    sampleZ,
                                    time + i * 7L
                            ).height()
                    );
        }

        require(
                loudEnergy
                        > quietEnergy
                        * 1.45,
                "Increasing spectrum amplitude must increase average wave energy"
        );

        OceanWaveField.Sample dryRunup =
                OceanWaveField.sample(
                        coast,
                        x,
                        z,
                        time
                );

        OceanWaveField.Sample taggedRunup =
                OceanWaveField.sample(
                        weatherTaggedCoast,
                        x,
                        z,
                        time
                );

        require(
                Math.abs(
                        taggedRunup.height()
                                - dryRunup.height()
                ) < 1.0E-12
                        && Math.abs(
                        taggedRunup.runup()
                                - dryRunup.runup()
                ) < 1.0E-6,
                "Reserved weather channels must not affect the self-contained realistic spectrum"
        );

        WaveForcing external =
                new WaveForcing(
                        1.35,
                        1.15,
                        Math.toRadians(
                                12.0
                        ),
                        1.12,
                        0.08,
                        1.40
                );

        OceanWaveField.Sample forced =
                OceanWaveField.sample(
                        coast,
                        x,
                        z,
                        time,
                        external
                );

        require(
                forced.runup()
                        > dryRunup.runup(),
                "External forcing hook must be able to modify run-up when a future system plugs in"
        );

        require(
                a.crest() >= 0.0F
                        && a.crest() <= 1.0F
                        && a.breaking() >= 0.0F
                        && a.breaking() <= 1.0F,
                "Crest and breaking values must remain normalized"
        );

        for (int tick = 0;
             tick < 200_000;
             tick += 137) {
            OceanWaveField.Sample sample =
                    OceanWaveField.sample(
                            ocean,
                            tick * 0.031,
                            -tick * 0.017,
                            tick
                    );

            require(
                    Double.isFinite(
                            sample.height()
                    )
                            && sample.runup() >= 0.0F,
                    "Long-running deterministic field must remain finite"
            );
        }

        System.out.println(
                "Ocean wave field regression checks passed"
        );
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(
                    message
            );
        }
    }
}
