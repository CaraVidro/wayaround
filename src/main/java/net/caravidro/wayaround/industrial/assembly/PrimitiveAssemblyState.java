package net.caravidro.wayaround.industrial.assembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public final class PrimitiveAssemblyState {
    private final List<AssemblyPartProfile> parts = new ArrayList<>();
    private int operations;
    private int mistakes;

    public List<AssemblyPartProfile> parts() { return Collections.unmodifiableList(parts); }
    public int operations() { return operations; }
    public int mistakes() { return mistakes; }
    public boolean isEmpty() { return parts.isEmpty(); }
    public int size() { return parts.size(); }

    boolean add(AssemblyPartProfile part) { return parts.add(part); }
    AssemblyPartProfile removeLast() { return parts.isEmpty() ? null : parts.remove(parts.size() - 1); }
    AssemblyPartProfile last() { return parts.isEmpty() ? null : parts.get(parts.size() - 1); }

    public boolean has(AssemblyPartProfile.Kind kind) {
        for (AssemblyPartProfile part : parts) {
            if (part.kind() == kind) return true;
        }
        return false;
    }

    AssemblyPartProfile weakestPart() {
        AssemblyPartProfile weakest = null;
        float score = Float.MAX_VALUE;
        for (AssemblyPartProfile part : parts) {
            float current = part.assemblyScore();
            if (current < score) {
                weakest = part;
                score = current;
            }
        }
        return weakest;
    }

    void recordOperation(boolean mistake) {
        operations++;
        if (mistake) mistakes++;
    }

    public float averageAlignment() { return average(Metric.ALIGNMENT); }
    public float averageTension() { return average(Metric.TENSION); }
    public float averageFatigue() { return average(Metric.FATIGUE); }
    public float durabilityScore() { return average(Metric.DURABILITY); }

    public float overallQuality() {
        if (parts.isEmpty()) return 0.0F;
        float sum = 0.0F;
        for (AssemblyPartProfile part : parts) {
            sum += part.quality() * 0.30F
                    + part.alignment() * 0.25F
                    + part.balance() * 0.15F
                    + part.tension() * 0.10F
                    + part.durabilityScore() * 0.20F;
        }
        float mistakePenalty = Math.min(0.22F, mistakes * 0.025F);
        return Mth.clamp(sum / parts.size() - mistakePenalty, 0.0F, 1.0F);
    }

    public void applyToolWear(float fraction) {
        float amount = Mth.clamp(fraction, 0.0F, 1.0F);
        for (AssemblyPartProfile part : parts) part.applyWear(amount);
    }

    public void clear() {
        parts.clear();
        operations = 0;
        mistakes = 0;
    }

    public PrimitiveAssemblyState copy() { return load(save()); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (AssemblyPartProfile part : parts) list.add(part.save());
        tag.put("Parts", list);
        tag.putInt("Operations", operations);
        tag.putInt("Mistakes", mistakes);
        return tag;
    }

    public static PrimitiveAssemblyState load(CompoundTag tag) {
        PrimitiveAssemblyState state = new PrimitiveAssemblyState();
        ListTag list = tag.getList("Parts", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            if (entry instanceof CompoundTag partTag) state.parts.add(AssemblyPartProfile.load(partTag));
        }
        state.operations = Math.max(0, tag.getInt("Operations"));
        state.mistakes = Math.max(0, tag.getInt("Mistakes"));
        return state;
    }

    private float average(Metric metric) {
        if (parts.isEmpty()) return 0.0F;
        float sum = 0.0F;
        for (AssemblyPartProfile part : parts) {
            sum += switch (metric) {
                case ALIGNMENT -> part.alignment();
                case TENSION -> part.tension();
                case FATIGUE -> part.fatigue();
                case DURABILITY -> part.durabilityScore();
            };
        }
        return Mth.clamp(sum / parts.size(), 0.0F, 1.0F);
    }

    private enum Metric { ALIGNMENT, TENSION, FATIGUE, DURABILITY }
}
