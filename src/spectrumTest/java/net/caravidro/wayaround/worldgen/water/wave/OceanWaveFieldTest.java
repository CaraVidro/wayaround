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

        OceanWaveField.Profile rainyCoast =
                new OceanWaveField.Profile(
                        0.24F,
                        0.86F,
                        1.0F,
                        0.42F,
                        0.16F,
                        11.0F,
                        5.8F,
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

        require(
                Math.abs(
                        a.height()
                ) > Math.abs(
                        quiet.height()
                ),
                "Increasing amplitude must increase the same wave shape"
        );

        OceanWaveField.Sample dryRunup =
                OceanWaveField.sample(
                        coast,
                        x,
                        z,
                        time
                );

        OceanWaveField.Sample rainyRunup =
                OceanWaveField.sample(
                        rainyCoast,
                        x,
                        z,
                        time
                );

        require(
                rainyRunup.runup()
                        > dryRunup.runup(),
                "Rain/storm profile must be able to push the same crest farther inland"
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
