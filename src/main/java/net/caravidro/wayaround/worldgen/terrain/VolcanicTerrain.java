package net.caravidro.wayaround.worldgen.terrain;

import net.caravidro.wayaround.worldgen.geography.VolcanicField;

/**
 * Terrain profile for giant stratovolcanoes / volcanic massifs.
 */
public final class VolcanicTerrain {

    private VolcanicTerrain() {
    }

    public static double surfaceHeight(
            int x,
            int z
    ) {
        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        x,
                        z
                );

        if (volcano == null) {
            return 64.0;
        }

        double distance =
                volcano.distanceTo(
                        x,
                        z
                );

        double normalized =
                distance
                        / volcano.radius();

        double base =
                68.0
                        + VolcanicField.detailNoise(
                        x,
                        z,
                        640.0
                )
                        * 5.0;

        if (normalized
                >= 1.18) {
            return base;
        }

        /*
         * A sub-linear radial profile makes the mountain begin rising very
         * early. The result is the intended "it just keeps climbing" feeling
         * instead of a small cone glued on top of normal terrain.
         */
        double cone =
                Math.pow(
                        Math.max(
                                0.0,
                                1.0 - normalized
                        ),
                        0.78
                );

        double surface =
                base
                        + (
                        volcano.summitY()
                                - base
                )
                        * cone;

        double roughness =
                VolcanicField.detailNoise(
                        x,
                        z,
                        155.0
                )
                        * 11.0
                        * Math.max(
                        0.0,
                        1.0 - normalized
                );

        double ridges =
                Math.abs(
                        VolcanicField.detailNoise(
                                x + 913.0,
                                z - 457.0,
                                88.0
                        )
                )
                        * 7.0
                        * Math.max(
                        0.0,
                        1.0 - normalized
                );

        surface +=
                roughness
                        + ridges;

        double craterDistance =
                distance
                        / Math.max(
                        1.0,
                        volcano.craterRadius()
                );

        /*
         * Raised crater rim before the center collapses inward.
         */
        double rim =
                Math.exp(
                        -Math.pow(
                                (
                                        craterDistance - 1.0
                                )
                                        / 0.20,
                                2.0
                        )
                )
                        * (
                        volcano.ceilingPeak()
                                ? 7.0
                                : 11.0
                );

        surface +=
                rim;

        double crater =
                1.0
                        - smoothstep(
                        0.20,
                        1.0,
                        craterDistance
                );

        surface -=
                crater
                        * volcano.craterDepth();

        return clamp(
                surface,
                -48.0,
                318.0
        );
    }

    public static double sampleDensity(
            int x,
            int y,
            int z
    ) {
        double surface =
                surfaceHeight(
                        x,
                        z
                );

        double density =
                (
                        surface - y
                )
                        / 16.0;

        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        x,
                        z
                );

        if (volcano == null) {
            return density;
        }

        double distance =
                volcano.distanceTo(
                        x,
                        z
                );

        /*
         * A narrow central conduit under the crater. The lava-filling feature
         * handles the visible lake; this cut keeps the interior from becoming a
         * completely solid blackstone plug.
         */
        double conduitRadius =
                volcano.craterRadius()
                        * 0.22;

        if (distance
                < conduitRadius
                && y > volcano.lavaLevel()
                        - 30
                && y < volcano.summitY()
                        - 5) {

            double conduit =
                    1.0
                            - distance
                            / conduitRadius;

            density -=
                    conduit
                            * 1.45;
        }

        return density;
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                clamp(
                        (
                                value - edge0
                        )
                                / (
                                edge1 - edge0
                        ),
                        0.0,
                        1.0
                );

        return t
                * t
                * (
                3.0
                        - 2.0
                        * t
        );
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}
