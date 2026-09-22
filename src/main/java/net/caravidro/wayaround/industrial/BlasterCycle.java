package net.caravidro.wayaround.industrial;

/** Pure processing rules: an unavailable output must never consume electricity or ingredients. */
public final class BlasterCycle {
    public static final int CAPACITY = 32_000;
    public static final int ENERGY_PER_TICK = 40;
    public record Step(int progress, int energyUsed, boolean finished) {}

    private BlasterCycle() {}

    public static int duration(int recipeTicks) { return Math.max(1, (int) Math.ceil(recipeTicks * 0.4)); }

    public static Step step(int progress, int total, int energy, boolean outputAccepts) {
        if (!outputAccepts || energy < ENERGY_PER_TICK) return new Step(progress, 0, false);
        int next = progress + 1;
        return new Step(next >= total ? 0 : next, ENERGY_PER_TICK, next >= total);
    }
}
