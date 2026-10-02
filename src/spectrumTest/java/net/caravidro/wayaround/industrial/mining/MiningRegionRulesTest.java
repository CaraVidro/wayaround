package net.caravidro.wayaround.industrial.mining;

import java.util.EnumSet;

public final class MiningRegionRulesTest {
    private static int checks;

    private MiningRegionRulesTest() {}

    public static void main(String[] args) {
        deterministicAddressing();
        distributionStaysRareButPresent();
        geometryBoundsStayInsideCells();
        everyOreFamilyAppears();

        System.out.println(
                "PASS: "
                        + checks
                        + " mining-region determinism and distribution checks"
        );
    }

    private static void deterministicAddressing() {
        long seed = 92837465L;

        for (int x = -18; x <= 18; x++) {
            for (int z = -18; z <= 18; z++) {
                var a = MiningRegionRules.region(seed, x, z);
                var b = MiningRegionRules.region(seed, x, z);
                check(a.equals(b), "same seed/cell must resolve the same geological address");
            }
        }
    }

    private static void distributionStaysRareButPresent() {
        long seed = 73L;
        int active = 0;
        int total = 0;
        int open = 0;

        for (int x = -70; x <= 70; x++) {
            for (int z = -70; z <= 70; z++) {
                total++;
                var region = MiningRegionRules.region(seed, x, z);
                if (region.isPresent()) {
                    active++;
                    if (region.get().openPit()) open++;
                }
            }
        }

        double ratio = active / (double) total;
        double openRatio = open / (double) Math.max(1, active);

        check(ratio > 0.30 && ratio < 0.38,
                "mining regions should stay uncommon without becoming effectively absent");
        check(openRatio > 0.23 && openRatio < 0.35,
                "open-pit mines should remain a minority of mining regions");
    }

    private static void geometryBoundsStayInsideCells() {
        long seed = 88119L;

        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                var optional = MiningRegionRules.region(seed, x, z);
                if (optional.isEmpty()) continue;

                var region = optional.get();
                int localX = region.centerX() - x * MiningRegionRules.CELL_SIZE;
                int localZ = region.centerZ() - z * MiningRegionRules.CELL_SIZE;

                check(localX >= 58 && localX < MiningRegionRules.CELL_SIZE - 58,
                        "region center X stays away from cell boundary");
                check(localZ >= 58 && localZ < MiningRegionRules.CELL_SIZE - 58,
                        "region center Z stays away from cell boundary");
                check(region.radius() >= 18 && region.radius() <= 28,
                        "mining-region radius remains bounded");
                check(Math.abs(region.entryX() - region.centerX()) <= region.radius() + 8,
                        "entry X remains local to the deposit");
                check(Math.abs(region.entryZ() - region.centerZ()) <= region.radius() + 8,
                        "entry Z remains local to the deposit");
            }
        }
    }

    private static void everyOreFamilyAppears() {
        EnumSet<ComplexOreKind> found = EnumSet.noneOf(ComplexOreKind.class);

        for (int x = -80; x <= 80 && found.size() < ComplexOreKind.values().length; x++) {
            for (int z = -80; z <= 80; z++) {
                MiningRegionRules.region(551992L, x, z)
                        .ifPresent(region -> found.add(region.kind()));
            }
        }

        check(found.size() == ComplexOreKind.values().length,
                "iron, coal, copper and gold regions must all exist in deterministic distribution");
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
}
