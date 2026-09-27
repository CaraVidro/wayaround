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
                    20,
                    Math.max(
                            8,
                            safe + 3
                    )
            );
        }

        return Math.min(
                16,
                Math.max(
                        6,
                        safe + 2
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
                0.42,
                currentSpeed * 1.8
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
