package net.caravidro.wayaround.ecology;

/** Deterministic ecology rules kept separate from world/entity access. */
public final class EcologyRules {

    private EcologyRules() {}

    public static int enlargedCluster(
            int original,
            boolean fish
    ) {
        int safe =
                Math.max(
                        1,
                        original
                );

        if (fish) {
            return Math.min(
                    32,
                    Math.max(
                            12,
                            safe + 6
                    )
            );
        }

        return Math.min(
                22,
                Math.max(
                        8,
                        safe + 4
                )
        );
    }

    public static boolean fishMigrationSeason(
            long minecraftDay
    ) {
        return Math.floorMod(
                minecraftDay,
                8L
        )
                >= 5L;
    }

    public static double pebbleMoveChance(
            double currentSpeed
    ) {
        if (currentSpeed < 0.045) {
            return 0.0;
        }

        return Math.min(
                0.58,
                currentSpeed * 2.15
        );
    }

    public static float recruitment(
            float organicGrowth,
            float moistureMemory
    ) {
        float value =
                organicGrowth * 0.65F
                        + moistureMemory * 0.35F;

        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        value
                )
        );
    }
}
