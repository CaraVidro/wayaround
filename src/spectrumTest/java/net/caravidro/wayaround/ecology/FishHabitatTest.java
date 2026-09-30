package net.caravidro.wayaround.ecology;

import java.util.HashSet;

public final class FishHabitatTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var covered = new HashSet<String>();
        for (var habitat : FishHabitat.values()) {
            check(habitat.fish.size() >= 2 && habitat.fish.size() <= 5, "2..5 fish per region: " + habitat);
            for (String biome : habitat.biomes) check(covered.add(biome), "Region overlap " + biome);
        }
        var deep = FishHabitat.forBiome("minecraft:deep_ocean");
        check(!deep.permits("wayaround:anglerfish", 54), "Angler must not appear at the surface");
        check(deep.permits("wayaround:anglerfish", 55), "Angler enters the abyss at 55 blocks");
        check(!deep.permits("wayaround:lanternfish", 27) && deep.permits("wayaround:lanternfish", 28), "Lantern depth boundary");
        check(!deep.permits("wayaround:sardine", 31), "Sardines must not fill the trench floor");
        check(!FishHabitat.ANTARCTIC_SEA.permits("wayaround:clownfish", 5), "No tropical reef fish under Antarctic ice");
        check(FishHabitat.ANTARCTIC_SEA.permits("wayaround:toothfish", 8), "Polar predator has polar habitat");
        check(FishHabitat.TEMPERATE_LAKE.permits("wayaround:carp", -70), "High-altitude lakes remain available");
        check(!FishHabitat.TEMPERATE_LAKE.permits("wayaround:reef_shark", 0), "No reef sharks in freshwater lakes");
        check(FishHabitat.RIVER.fish.contains("wayaround:carp") && FishHabitat.SWAMP.fish.contains("wayaround:carp"), "Multi-biome fish");
        check(FishHabitat.forBiome("minecraft:the_end") == null, "No fish added to dry dimensions");
        System.out.println("Regional fish habitat invariants passed: " + covered.size() + " biomes");
    }
}
