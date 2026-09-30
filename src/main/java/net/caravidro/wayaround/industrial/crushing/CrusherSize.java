package net.caravidro.wayaround.industrial.crushing;

/** Scale changes admitted loads and drive demand; it never creates free power. */
public enum CrusherSize {
    SMALL(1, 1, 1.5F, 80, 1.0F),
    MEDIUM(64, 8, 7.0F, 100, 2.2F),
    LARGE(256, 32, 22.0F, 140, 3.8F);

    public final int capacity, batch, cycleTicks;
    public final float power, hardness;
    CrusherSize(int capacity, int batch, float power, int cycleTicks, float hardness) {
        this.capacity = capacity;
        this.batch = batch;
        this.power = power;
        this.cycleTicks = cycleTicks;
        this.hardness = hardness;
    }
    public int inputCapacity(int feedMultiplier) { return this == SMALL ? 1 : capacity * Math.max(1, feedMultiplier); }
    public int boundedBatch(int availableInput, int outputRoom, int outputPerInput, int feedMultiplier) {
        if (availableInput <= 0 || outputRoom <= 0 || outputPerInput <= 0 || feedMultiplier <= 0) return 0;
        int requested = this == SMALL ? 1 : batch * Math.min(2, feedMultiplier);
        return Math.min(requested, Math.min(availableInput, outputRoom / outputPerInput));
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}
