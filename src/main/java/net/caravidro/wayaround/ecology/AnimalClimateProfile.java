package net.caravidro.wayaround.ecology;

import java.util.Map;

import net.caravidro.wayaround.worldgen.planet.PlanetMath;

/**
 * Coarse preferred climate for WayAround fauna.
 *
 * <p>Habitat still decides whether the geometry/biome is valid. This layer
 * only answers whether the world's latitude feels right for that species.</p>
 */
public final class AnimalClimateProfile {

    public enum Band {
        TROPICAL(0.06, 0.28),
        WARM(0.22, 0.34),
        TEMPERATE(0.46, 0.38),
        COOL(0.68, 0.34),
        POLAR(0.90, 0.24),
        BROAD(0.50, 0.78);

        private final double center;
        private final double width;

        Band(
                double center,
                double width
        ) {
            this.center = center;
            this.width = width;
        }

        public double suitability(
                double coldness
        ) {
            if (this == BROAD) {
                return 1.0;
            }

            double distance =
                    Math.abs(
                            Math.clamp(
                                    coldness,
                                    0.0,
                                    1.0
                            )
                                    - center
                    );

            if (distance >= width) {
                return 0.0;
            }

            double normalized =
                    1.0
                            - distance
                                    / width;

            return normalized
                    * normalized
                    * (
                    3.0
                            - 2.0
                                    * normalized
            );
        }
    }

    private static final Map<String, Band> BANDS =
            Map.ofEntries(
                    // Birds
                    Map.entry("wayaround:hummingbird", Band.TROPICAL),
                    Map.entry("wayaround:mimic_parrot", Band.TROPICAL),
                    Map.entry("wayaround:woodland_thrush", Band.TEMPERATE),
                    Map.entry("wayaround:crow", Band.BROAD),
                    Map.entry("wayaround:seagull", Band.TEMPERATE),

                    // Coast / surface ocean
                    Map.entry("wayaround:crab", Band.WARM),
                    Map.entry("wayaround:sardine", Band.TEMPERATE),
                    Map.entry("wayaround:sunfish", Band.WARM),
                    Map.entry("wayaround:flying_fish", Band.WARM),
                    Map.entry("wayaround:clownfish", Band.TROPICAL),
                    Map.entry("wayaround:reef_shark", Band.TROPICAL),
                    Map.entry("wayaround:manta_ray", Band.WARM),
                    Map.entry("wayaround:barracuda", Band.WARM),
                    Map.entry("wayaround:seahorse", Band.WARM),
                    Map.entry("wayaround:moray_eel", Band.TROPICAL),
                    Map.entry("wayaround:jellyfish", Band.BROAD),
                    Map.entry("wayaround:whale", Band.COOL),
                    Map.entry("wayaround:sperm_whale", Band.BROAD),

                    // Deep ocean
                    Map.entry("wayaround:oarfish", Band.COOL),
                    Map.entry("wayaround:lanternfish", Band.COOL),
                    Map.entry("wayaround:anglerfish", Band.COOL),
                    Map.entry("wayaround:toothfish", Band.POLAR),
                    Map.entry("wayaround:icefish", Band.POLAR),

                    // Inland fish
                    Map.entry("wayaround:carp", Band.WARM),
                    Map.entry("wayaround:catfish", Band.WARM),
                    Map.entry("wayaround:archerfish", Band.TROPICAL),
                    Map.entry("wayaround:perch", Band.TEMPERATE),
                    Map.entry("wayaround:trout", Band.COOL),

                    // Colony insects
                    Map.entry("wayaround:black_ant", Band.TEMPERATE),
                    Map.entry("wayaround:red_ant", Band.WARM),
                    Map.entry("wayaround:honey_ant", Band.WARM),
                    Map.entry("wayaround:termite", Band.TROPICAL),

                    // Cleinton is intentionally geographically absurd.
                    Map.entry("wayaround:cleinton", Band.BROAD)
            );

    private AnimalClimateProfile() {
    }

    public static boolean managed(
            String entityId
    ) {
        return BANDS.containsKey(
                entityId
        );
    }

    public static Band band(
            String entityId
    ) {
        return BANDS.get(
                entityId
        );
    }

    /**
     * 0 = equatorial/warmest portion of the wrapped world, 1 = polar/coldest.
     */
    public static double coldnessAt(
            int z
    ) {
        return Math.abs(
                PlanetMath.latitude(
                        z
                )
        );
    }

    public static double suitability(
            String entityId,
            int z
    ) {
        Band band =
                BANDS.get(
                        entityId
                );

        return band == null
                ? 1.0
                : band.suitability(
                        coldnessAt(
                                z
                        )
                );
    }

    /**
     * Natural spawn gate. A tiny fringe remains possible around a preferred
     * band so ecology does not end at an invisible hard line.
     */
    public static boolean permits(
            String entityId,
            int z,
            double roll
    ) {
        double suitability =
                suitability(
                        entityId,
                        z
                );

        if (suitability <= 0.025) {
            return false;
        }

        double chance =
                0.12
                        + suitability
                                * 0.88;

        return Math.clamp(
                roll,
                0.0,
                1.0
        )
                <= chance;
    }

    public static boolean warmOrTemperateCoast(
            String entityId
    ) {
        Band band =
                BANDS.get(
                        entityId
                );

        return band == Band.TROPICAL
                || band == Band.WARM
                || band == Band.TEMPERATE;
    }
}
