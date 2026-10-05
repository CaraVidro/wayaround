package net.caravidro.wayaround.worldgen.geography;

import java.util.ArrayList;
import java.util.List;

/**
 * Kilometer-scale continental rifts: long, curved valleys with steep walls.
 */
public final class GreatRiftField {

    private static final int CELL =
            7600;

    private static final double SPAWN_THRESHOLD =
            0.64;

    private static final long SEED =
            0x4752454154524946L;

    private static double edgeFade(int x,int z){return net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.FINITE_WORLD)?net.caravidro.wayaround.worldgen.planet.PlanetMath.edgeFade(x,z):1;}

    private GreatRiftField() {
    }

    public record Rift(
            int centerX,
            int centerZ,
            double angle,
            double halfLength,
            double halfWidth,
            double shoulderWidth,
            double floorY,
            double rimY,
            double phase,
            boolean abyssal
    ) {
        public Sample sample(
                double x,
                double z
        ) {
            double dx =
                    x - centerX;

            double dz =
                    z - centerZ;

            double cos =
                    Math.cos(
                            angle
                    );

            double sin =
                    Math.sin(
                            angle
                    );

            double along =
                    dx * cos
                            + dz * sin;

            double across =
                    -dx * sin
                            + dz * cos;

            double meander =
                    Math.sin(
                            along / 430.0
                                    + phase
                    )
                            * halfWidth
                            * 0.34
                            + Math.sin(
                            along / 185.0
                                    + phase * 1.71
                    )
                            * halfWidth
                            * 0.11;

            double crossDistance =
                    Math.abs(
                            across - meander
                    );

            double lengthFade =
                    1.0
                            - smoothstep(
                            halfLength * 0.80,
                            halfLength,
                            Math.abs(
                                    along
                            )
                    );

            double region =
                    (
                            1.0
                                    - smoothstep(
                                    halfWidth,
                                    shoulderWidth,
                                    crossDistance
                            )
                    )
                            * lengthFade;

            double canyon =
                    (
                            1.0
                                    - smoothstep(
                                    halfWidth * 0.58,
                                    halfWidth * 1.08,
                                    crossDistance
                            )
                    )
                            * lengthFade;

            return new Sample(
                    along,
                    crossDistance,
                    clamp01(
                            region
                    ),
                    clamp01(
                            canyon
                    )
            );
        }
    }

    public record Sample(
            double along,
            double crossDistance,
            double region,
            double canyon
    ) {
    }

    private record Cache(
            int cellX,
            int cellZ,
            List<Rift> rifts
    ) {
    }

    private static final ThreadLocal<Cache> CACHE =
            ThreadLocal.withInitial(
                    () ->
                            new Cache(
                                    Integer.MIN_VALUE,
                                    Integer.MIN_VALUE,
                                    List.of()
                            )
            );

    public static Rift nearest(
            int blockX,
            int blockZ
    ) {
        if (AntarcticField.polarInfluence(
                blockX,
                blockZ
        ) > 0.08
                || VolcanicField.influence(
                blockX,
                blockZ
        ) > 0.12) {
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

        Cache cache =
                CACHE.get();

        if (cache.cellX()
                != cellX
                || cache.cellZ()
                != cellZ) {

            ArrayList<Rift> rifts =
                    new ArrayList<>(
                            9
                    );

            for (int ox = -1;
                 ox <= 1;
                 ox++) {
                for (int oz = -1;
                     oz <= 1;
                     oz++) {

                    Rift candidate =
                            candidate(
                                    cellX + ox,
                                    cellZ + oz
                            );

                    if (candidate != null) {
                        rifts.add(
                                candidate
                        );
                    }
                }
            }

            cache =
                    new Cache(
                            cellX,
                            cellZ,
                            List.copyOf(
                                    rifts
                            )
                    );

            CACHE.set(
                    cache
            );
        }

        Rift best =
                null;

        double bestRegion =
                0.0;

        for (Rift rift :
                cache.rifts()) {

            double region =
                    rift.sample(
                            blockX,
                            blockZ
                    )
                            .region();

            if (region
                    > bestRegion) {
                bestRegion =
                        region;

                best =
                        rift;
            }
        }

        return best;
    }

    private static Rift candidate(
            int regionX,
            int regionZ
    ) {
        long base =
                mix(
                        SEED
                                ^ (long) regionX
                                * 341873128712L
                                ^ (long) regionZ
                                * 132897987541L
                );

        if (unit(
                base
        ) < SPAWN_THRESHOLD) {
            return null;
        }

        double jitterX =
                signed(
                        mix(
                                base
                                        ^ 0x9E3779B97F4A7C15L
                        )
                )
                        * CELL
                        * 0.22;

        double jitterZ =
                signed(
                        mix(
                                base
                                        ^ 0xD1B54A32D192ED03L
                        )
                )
                        * CELL
                        * 0.22;

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

        double angle =
                unit(
                        mix(
                                base
                                        ^ 0x94D049BB133111EBL
                        )
                )
                        * Math.PI;

        double halfLength =
                1500.0
                        + unit(
                        mix(
                                base
                                        ^ 0xA24BAED4963EE407L
                        )
                )
                        * 1500.0;

        boolean abyssal =
                unit(
                        mix(
                                base
                                        ^ 0xC13FA9A902A6328FL
                        )
                )
                        > 0.92;

        double halfWidth =
                170.0
                        + unit(
                        mix(
                                base
                                        ^ 0x91E10DA5C79E7B1DL
                        )
                )
                        * 250.0;

        double shoulder =
                halfWidth
                        + 420.0
                        + unit(
                        mix(
                                base
                                        ^ 0x632BE59BD9B4E019L
                        )
                )
                        * 520.0;

        double floorY =
                abyssal
                        ? 18.0
                                + unit(
                                mix(
                                        base
                                                ^ 0xDB4F0B9175AE2165L
                                )
                        )
                                * 26.0
                        : 66.0
                                + unit(
                                mix(
                                        base
                                                ^ 0xBBE0563303A4615FL
                                )
                        )
                                * 18.0;

        double rimY =
                126.0
                        + unit(
                        mix(
                                base
                                        ^ 0x9FB21C651E98DF25L
                        )
                )
                        * 72.0;

        double phase =
                unit(
                        mix(
                                base
                                        ^ 0x4CF5AD432745937FL
                        )
                )
                        * Math.PI
                        * 2.0;

        return new Rift(
                centerX,
                centerZ,
                angle,
                halfLength,
                halfWidth,
                shoulder,
                floorY,
                rimY,
                phase,
                abyssal
        );
    }

    public static double influence(
            int blockX,
            int blockZ
    ) {
        Rift rift =
                nearest(
                        blockX,
                        blockZ
                );

        if (rift == null) {
            return 0.0;
        }

        return edgeFade(blockX,blockZ)*rift.sample(
                blockX,
                blockZ
        )
                .region();
    }

    public static double canyonStrength(
            int blockX,
            int blockZ
    ) {
        Rift rift =
                nearest(
                        blockX,
                        blockZ
                );

        if (rift == null) {
            return 0.0;
        }

        return edgeFade(blockX,blockZ)*rift.sample(
                blockX,
                blockZ
        )
                .canyon();
    }

    public static boolean isGreatRift(
            int blockX,
            int blockZ
    ) {
        return influence(
                blockX,
                blockZ
        ) >= 0.24;
    }

    public static double detail(
            double x,
            double z,
            double scale
    ) {
        double a =
                Math.sin(
                        x / scale
                                + z / (
                                scale
                                        * 1.37
                        )
                );

        double b =
                Math.sin(
                        x / (
                                scale
                                        * 0.47
                        )
                                - z / (
                                scale
                                        * 0.73
                        )
                                + 1.91
                );

        return a * 0.68
                + b * 0.32;
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

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                clamp01(
                        (
                                value - edge0
                        )
                                / (
                                edge1 - edge0
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

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
