package net.caravidro.wayaround.worldgen.geography;

import net.caravidro.wayaround.worldgen.geography.AntarcticField;

/**
 * Large-scale deterministic volcanic provinces for the Overworld.
 *
 * One province occupies several thousand blocks, while the actual mountain can
 * span more than a thousand blocks from flank to flank. Rare "ceiling" peaks
 * intentionally reach just below the Overworld build limit.
 */
public final class VolcanicField {

    private static final int CELL =
            6200;

    private static final double SPAWN_THRESHOLD =
            0.57;

    private static final long SEED_PRESENT =
            0x5A17C0DEL;

    private static final long SEED_X =
            0x24B91A73L;

    private static final long SEED_Z =
            0x7F4A7C15L;

    private static final long SEED_RADIUS =
            0x2D192ED03L;

    private static final long SEED_HEIGHT =
            0x632BE59BDL;

    private static final long SEED_DETAIL =
            0x94D049BB1L;

    private VolcanicField() {
    }

    public record Volcano(
            int centerX,
            int centerZ,
            double radius,
            double summitY,
            double craterRadius,
            double craterDepth,
            int lavaLevel,
            double activity,
            boolean ceilingPeak
    ) {
        public double distanceTo(
                double x,
                double z
        ) {
            double dx =
                    x - centerX;

            double dz =
                    z - centerZ;

            return Math.sqrt(
                    dx * dx
                            + dz * dz
            );
        }
    }

    public static Volcano nearest(
            int blockX,
            int blockZ
    ) {
        /*
         * Keep the Antarctic approach geographically distinct. Volcanoes are a
         * separate large-scale destination, not black cones poking through the
         * Southern Ocean.
         */
        if (AntarcticField.polarInfluence(
                blockX,
                blockZ
        ) > 0.08) {
            return null;
        }

        int cellX =
                Math.floorDiv(
                        blockX,
                        CELL
                );

        int cellZ =
                Math.floorDiv(
                        blockZ,
                        CELL
                );

        Volcano best =
                null;

        double bestNormalized =
                Double.POSITIVE_INFINITY;

        for (int ox = -1;
             ox <= 1;
             ox++) {
            for (int oz = -1;
                 oz <= 1;
                 oz++) {

                int regionX =
                        cellX + ox;

                int regionZ =
                        cellZ + oz;

                long base =
                        mix(
                                SEED_PRESENT
                                        ^ (long) regionX
                                        * 341873128712L
                                        ^ (long) regionZ
                                        * 132897987541L
                        );

                double present =
                        unit(
                                base
                        );

                if (present
                        < SPAWN_THRESHOLD) {
                    continue;
                }

                double jitterX =
                        signed(
                                mix(
                                        base
                                                ^ SEED_X
                                )
                        )
                                * CELL
                                * 0.30;

                double jitterZ =
                        signed(
                                mix(
                                        base
                                                ^ SEED_Z
                                )
                        )
                                * CELL
                                * 0.30;

                int centerX =
                        (int) Math.round(
                                regionX
                                        * (double) CELL
                                        + CELL * 0.5
                                        + jitterX
                        );

                int centerZ =
                        (int) Math.round(
                                regionZ
                                        * (double) CELL
                                        + CELL * 0.5
                                        + jitterZ
                        );

                double heightRoll =
                        unit(
                                mix(
                                        base
                                                ^ SEED_HEIGHT
                                )
                        );

                boolean ceilingPeak =
                        heightRoll
                                > 0.985;

                double radiusRoll =
                        unit(
                                mix(
                                        base
                                                ^ SEED_RADIUS
                                )
                        );

                double radius =
                        440.0
                                + radiusRoll
                                * 410.0;

                double summitY;

                if (ceilingPeak) {
                    /*
                     * The rim is allowed to brush the ceiling. Keeping two
                     * blocks of breathing room avoids impossible surface writes.
                     */
                    summitY =
                            318.0;

                    radius =
                            Math.max(
                                    radius,
                                    900.0
                            );

                } else if (heightRoll
                        > 0.90) {

                    summitY =
                            278.0
                                    + unit(
                                    mix(
                                            base
                                                    ^ 0xA24BAED4963EE407L
                                    )
                            )
                                    * 34.0;

                    radius =
                            Math.max(
                                    radius,
                                    720.0
                            );

                } else {
                    summitY =
                            188.0
                                    + heightRoll
                                    / 0.90
                                    * 86.0;
                }

                double craterRadius =
                        radius
                                * (
                                0.09
                                        + unit(
                                        mix(
                                                base
                                                        ^ 0x9FB21C651E98DF25L
                                        )
                                )
                                        * 0.065
                        );

                double craterDepth =
                        26.0
                                + unit(
                                mix(
                                        base
                                                ^ 0xC13FA9A902A6328FL
                                )
                        )
                                * 44.0;

                int lavaLevel =
                        (int) Math.round(
                                summitY
                                        - craterDepth
                                        - 9.0
                        );

                double activity =
                        unit(
                                mix(
                                        base
                                                ^ 0x91E10DA5C79E7B1DL
                                )
                        );

                Volcano volcano =
                        new Volcano(
                                centerX,
                                centerZ,
                                radius,
                                summitY,
                                craterRadius,
                                craterDepth,
                                lavaLevel,
                                activity,
                                ceilingPeak
                        );

                double normalized =
                        volcano.distanceTo(
                                blockX,
                                blockZ
                        )
                                / volcano.radius();

                if (normalized
                        < bestNormalized) {
                    bestNormalized =
                            normalized;

                    best =
                            volcano;
                }
            }
        }

        return best;
    }

    /**
     * Broad biome/environment influence. This extends beyond the physical cone
     * so a player enters ash/basalt foothills before the actual climb.
     */
    public static double influence(
            int blockX,
            int blockZ
    ) {
        Volcano volcano =
                nearest(
                        blockX,
                        blockZ
                );

        if (volcano == null) {
            return 0.0;
        }

        double normalized =
                volcano.distanceTo(
                        blockX,
                        blockZ
                )
                        / volcano.radius();

        return 1.0
                - smoothstep(
                0.86,
                1.24,
                normalized
        );
    }

    /**
     * Terrain blend is slightly tighter than the biome footprint.
     */
    public static double terrainBlend(
            int blockX,
            int blockZ
    ) {
        Volcano volcano =
                nearest(
                        blockX,
                        blockZ
                );

        if (volcano == null) {
            return 0.0;
        }

        double normalized =
                volcano.distanceTo(
                        blockX,
                        blockZ
                )
                        / volcano.radius();

        return 1.0
                - smoothstep(
                0.90,
                1.16,
                normalized
        );
    }

    public static boolean isVolcanic(
            int blockX,
            int blockZ
    ) {
        return influence(
                blockX,
                blockZ
        ) >= 0.28;
    }

    public static double craterStrength(
            int blockX,
            int blockZ
    ) {
        Volcano volcano =
                nearest(
                        blockX,
                        blockZ
                );

        if (volcano == null) {
            return 0.0;
        }

        double distance =
                volcano.distanceTo(
                        blockX,
                        blockZ
                );

        return 1.0
                - smoothstep(
                volcano.craterRadius()
                        * 0.20,
                volcano.craterRadius(),
                distance
        );
    }

    public static double detailNoise(
            double x,
            double z,
            double scale
    ) {
        return noise2D(
                x,
                z,
                scale,
                SEED_DETAIL
        );
    }

    private static double noise2D(
            double x,
            double z,
            double scale,
            long seed
    ) {
        double px =
                x / scale;

        double pz =
                z / scale;

        int x0 =
                fastFloor(
                        px
                );

        int z0 =
                fastFloor(
                        pz
                );

        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;

        double tx =
                fade(
                        px - x0
                );

        double tz =
                fade(
                        pz - z0
                );

        double a =
                signed(
                        mix(
                                seed
                                        ^ (long) x0
                                        * 341873128712L
                                        ^ (long) z0
                                        * 132897987541L
                        )
                );

        double b =
                signed(
                        mix(
                                seed
                                        ^ (long) x1
                                        * 341873128712L
                                        ^ (long) z0
                                        * 132897987541L
                        )
                );

        double c =
                signed(
                        mix(
                                seed
                                        ^ (long) x0
                                        * 341873128712L
                                        ^ (long) z1
                                        * 132897987541L
                        )
                );

        double d =
                signed(
                        mix(
                                seed
                                        ^ (long) x1
                                        * 341873128712L
                                        ^ (long) z1
                                        * 132897987541L
                        )
                );

        return lerp(
                lerp(
                        a,
                        b,
                        tx
                ),
                lerp(
                        c,
                        d,
                        tx
                ),
                tz
        );
    }

    private static double unit(
            long value
    ) {
        return (
                value >>> 11
        )
                / (double) (
                1L << 53
        );
    }

    private static double signed(
            long value
    ) {
        return unit(
                value
        )
                * 2.0
                - 1.0;
    }

    private static long mix(
            long value
    ) {
        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        return value
                ^ value >>> 31;
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

    private static double fade(
            double value
    ) {
        return value
                * value
                * value
                * (
                value
                        * (
                        value
                                * 6.0
                                - 15.0
                )
                        + 10.0
        );
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {
        return a
                + (
                b - a
        )
                * t;
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

    private static int fastFloor(
            double value
    ) {
        int integer =
                (int) value;

        return value
                < integer
                ? integer - 1
                : integer;
    }
}
