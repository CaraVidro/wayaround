package net.caravidro.wayaround.ecology;

public final class EcologyRulesTest {

    public static void main(String[] args) {
        require(
                EcologyRules.enlargedCluster(4, false) == 8,
                "Land animal bands should be expanded"
        );

        require(
                EcologyRules.enlargedCluster(6, true) == 10,
                "Fish bands should stay visibly grouped without oversized schools"
        );

        require(
                EcologyRules.enlargedCluster(99, true) == 16,
                "Fish cluster expansion must stay performance-bounded"
        );

        require(
                EcologyRules.enlargedCluster(1, true) == 6,
                "Small natural fish groups should remain visually alive"
        );

        require(
                !EcologyRules.fishMigrationSeason(4),
                "Migration must not start before day phase 5/8"
        );

        require(
                EcologyRules.fishMigrationSeason(5)
                        && EcologyRules.fishMigrationSeason(7),
                "Days 5-7 in the 8-day ecology cycle are migration season"
        );

        require(
                !EcologyRules.fishMigrationSeason(8),
                "Migration cycle must wrap every eight Minecraft days"
        );

        require(
                EcologyRules.pebbleMoveChance(0.02) == 0.0,
                "Very weak water must not move pebbles"
        );

        require(
                EcologyRules.pebbleMoveChance(0.20)
                        > EcologyRules.pebbleMoveChance(0.08),
                "Stronger current must move pebbles more often"
        );

        require(
                EcologyRules.pebbleMoveChance(10.0) <= 0.58,
                "Pebble migration probability must remain bounded"
        );

        float dry =
                EcologyRules.recruitment(
                        0.10F,
                        0.0F
                );

        float matureWet =
                EcologyRules.recruitment(
                        0.70F,
                        0.90F
                );

        require(
                matureWet > dry,
                "Persistent organic growth and moisture must improve recruitment"
        );

        require(
                EcologyRules.recruitment(5.0F, 5.0F) <= 1.0F,
                "Recruitment must remain bounded"
        );

        System.out.println(
                "Living Ecology regression tests passed"
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
