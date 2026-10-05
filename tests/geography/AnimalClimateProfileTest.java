package net.caravidro.wayaround.ecology;

import java.util.List;

public final class AnimalClimateProfileTest {

    private AnimalClimateProfileTest() {
    }

    public static void main(String[] args) {
        warmSpeciesPreferEquator();
        polarSpeciesPreferPoles();
        seagullsAvoidPolarPreference();
        allKnownFaunaHaveProfiles();
        System.out.println("AnimalClimateProfileTest passed");
    }

    private static void warmSpeciesPreferEquator() {
        double equator =
                AnimalClimateProfile.suitability(
                        "wayaround:hummingbird",
                        0
                );

        double pole =
                AnimalClimateProfile.suitability(
                        "wayaround:hummingbird",
                        16_384
                );

        require(
                equator > pole,
                "hummingbird must prefer warm/equatorial latitude"
        );
    }

    private static void polarSpeciesPreferPoles() {
        for (String species :
                List.of(
                        "wayaround:icefish",
                        "wayaround:toothfish"
                )) {

            double equator =
                    AnimalClimateProfile.suitability(
                            species,
                            0
                    );

            double pole =
                    AnimalClimateProfile.suitability(
                            species,
                            16_384
                    );

            require(
                    pole > equator,
                    species + " must prefer polar latitude"
            );
        }
    }

    private static void seagullsAvoidPolarPreference() {
        double temperate =
                AnimalClimateProfile.suitability(
                        "wayaround:seagull",
                        5_000
                );

        double pole =
                AnimalClimateProfile.suitability(
                        "wayaround:seagull",
                        16_384
                );

        require(
                temperate > pole,
                "WayAround seagulls must prefer temperate coast over polar coast"
        );
    }

    private static void allKnownFaunaHaveProfiles() {
        List<String> fauna =
                List.of(
                        "wayaround:hummingbird",
                        "wayaround:mimic_parrot",
                        "wayaround:woodland_thrush",
                        "wayaround:crow",
                        "wayaround:seagull",
                        "wayaround:crab",
                        "wayaround:sardine",
                        "wayaround:sunfish",
                        "wayaround:flying_fish",
                        "wayaround:clownfish",
                        "wayaround:reef_shark",
                        "wayaround:manta_ray",
                        "wayaround:barracuda",
                        "wayaround:seahorse",
                        "wayaround:moray_eel",
                        "wayaround:jellyfish",
                        "wayaround:whale",
                        "wayaround:sperm_whale",
                        "wayaround:oarfish",
                        "wayaround:lanternfish",
                        "wayaround:anglerfish",
                        "wayaround:toothfish",
                        "wayaround:icefish",
                        "wayaround:carp",
                        "wayaround:catfish",
                        "wayaround:archerfish",
                        "wayaround:perch",
                        "wayaround:trout",
                        "wayaround:black_ant",
                        "wayaround:red_ant",
                        "wayaround:honey_ant",
                        "wayaround:termite",
                        "wayaround:cleinton"
                );

        for (String species :
                fauna) {
            require(
                    AnimalClimateProfile.managed(
                            species
                    ),
                    "missing climate profile for " + species
            );
        }
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
