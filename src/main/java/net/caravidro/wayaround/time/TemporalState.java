package net.caravidro.wayaround.time;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Persistent low-frequency temporal condition for one world position. */
public final class TemporalState {

    private long ageTicks;
    private long inactiveTicks;
    private long lastSampleGameTime;
    private float weathering;
    private float corrosion;
    private float organicGrowth;
    private float moistureMemory;

    public long ageTicks() { return ageTicks; }
    public long inactiveTicks() { return inactiveTicks; }
    public long lastSampleGameTime() { return lastSampleGameTime; }
    public float weathering() { return weathering; }
    public float corrosion() { return corrosion; }
    public float organicGrowth() { return organicGrowth; }
    public float moistureMemory() { return moistureMemory; }

    void advance(
            long elapsed,
            boolean active,
            float wetness,
            float weatheringRate,
            float corrosionRate,
            float organicRate
    ) {
        long dt = Math.max(0L, elapsed);

        ageTicks = safeAdd(ageTicks, dt);

        inactiveTicks = active
                ? 0L
                : safeAdd(inactiveTicks, dt);

        float days = dt / 24000.0F;

        moistureMemory = Mth.clamp(
                moistureMemory
                        + (wetness - moistureMemory)
                                * Math.min(1.0F, days * 0.75F + 0.04F),
                0.0F,
                1.0F
        );

        weathering = Mth.clamp(
                weathering + weatheringRate * days,
                0.0F,
                1.0F
        );

        corrosion = Mth.clamp(
                corrosion + corrosionRate * days,
                0.0F,
                1.0F
        );

        organicGrowth = Mth.clamp(
                organicGrowth + organicRate * days,
                0.0F,
                1.0F
        );
    }

    void markSample(long gameTime) {
        lastSampleGameTime = Math.max(0L, gameTime);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("AgeTicks", ageTicks);
        tag.putLong("InactiveTicks", inactiveTicks);
        tag.putLong("LastSample", lastSampleGameTime);
        tag.putFloat("Weathering", weathering);
        tag.putFloat("Corrosion", corrosion);
        tag.putFloat("OrganicGrowth", organicGrowth);
        tag.putFloat("MoistureMemory", moistureMemory);
        return tag;
    }

    public static TemporalState load(CompoundTag tag) {
        TemporalState state = new TemporalState();
        state.ageTicks = Math.max(0L, tag.getLong("AgeTicks"));
        state.inactiveTicks = Math.max(0L, tag.getLong("InactiveTicks"));
        state.lastSampleGameTime = Math.max(0L, tag.getLong("LastSample"));
        state.weathering = Mth.clamp(tag.getFloat("Weathering"), 0.0F, 1.0F);
        state.corrosion = Mth.clamp(tag.getFloat("Corrosion"), 0.0F, 1.0F);
        state.organicGrowth = Mth.clamp(tag.getFloat("OrganicGrowth"), 0.0F, 1.0F);
        state.moistureMemory = Mth.clamp(tag.getFloat("MoistureMemory"), 0.0F, 1.0F);
        return state;
    }

    private static long safeAdd(long value, long add) {
        if (Long.MAX_VALUE - value < add) return Long.MAX_VALUE;
        return value + add;
    }
}
