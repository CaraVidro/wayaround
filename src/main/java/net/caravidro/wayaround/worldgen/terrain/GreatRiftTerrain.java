package net.caravidro.wayaround.worldgen.terrain;

import net.caravidro.wayaround.worldgen.geography.GreatRiftField;

/**
 * Tall shoulders, steep walls and a long irregular rift floor.
 */
public final class GreatRiftTerrain {

    private GreatRiftTerrain() {
    }

    public static double surfaceHeight(
            int x,
            int z
    ) {
        GreatRiftField.Rift rift =
                GreatRiftField.nearest(
                        x,
                        z
                );

        if (rift == null) {
            return 72.0;
        }

        GreatRiftField.Sample sample =
                rift.sample(
                        x,
                        z
                );

        double floor =
                rift.floorY()
                        + GreatRiftField.detail(
                        x,
                        z,
                        140.0
                )
                        * (
                        rift.abyssal()
                                ? 4.0
                                : 6.0
                );

        double plateau =
                rift.rimY()
                        + GreatRiftField.detail(
                        x + 331.0,
                        z - 173.0,
                        510.0
                )
                        * 10.0;

        double wallStart =
                rift.halfWidth()
                        * 0.55;

        double wallEnd =
                rift.halfWidth()
                        * 1.12;

        double wall =
                smoothstep(
                        wallStart,
                        wallEnd,
                        sample.crossDistance()
                );

        /*
         * Exponent sharpens the middle of the transition into an actual cliff
         * face instead of a smooth valley.
         */
        wall =
                Math.pow(
                        wall,
                        2.45
                );

        double surface =
                floor
                        + (
                        plateau - floor
                )
                        * wall;

        double rimBand =
                Math.exp(
                        -Math.pow(
                                (
                                        sample.crossDistance()
                                                - rift.halfWidth()
                                                * 1.08
                                )
                                        / (
                                        rift.halfWidth()
                                                * 0.22
                                ),
                                2.0
                        )
                );

        surface +=
                rimBand
                        * (
                        10.0
                                + GreatRiftField.detail(
                                x,
                                z,
                                76.0
                        )
                                * 4.0
                );

        double terraces =
                Math.abs(
                        GreatRiftField.detail(
                                x - 811.0,
                                z + 509.0,
                                62.0
                        )
                )
                        * 5.0
                        * (
                        1.0 - wall
                );

        surface +=
                terraces;

        return Math.max(
                -48.0,
                Math.min(
                        300.0,
                        surface
                )
        );
    }

    public static double sampleDensity(
            int x,
            int y,
            int z
    ) {
        return (
                surfaceHeight(
                        x,
                        z
                )
                        - y
        )
                / 14.0;
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                (
                                        value - edge0
                                )
                                        / (
                                        edge1 - edge0
                                )
                        )
                );

        return t
                * t
                * (
                3.0
                        - 2.0
                        * t
        );
    }
}
