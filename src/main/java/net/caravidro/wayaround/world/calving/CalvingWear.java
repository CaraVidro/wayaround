package net.caravidro.wayaround.world.calving;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent fatigue per coastal chunk, measured only while players are nearby. */
final class CalvingWear extends SavedData {
    private final Map<Long, Double> wear = new HashMap<>();

    static CalvingWear get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CalvingWear::new, CalvingWear::load), "wayaround_calving_wear");
    }

    private static CalvingWear load(CompoundTag tag, HolderLookup.Provider registries) {
        CalvingWear data = new CalvingWear();
        for (String key : tag.getAllKeys()) {
            data.wear.put(Long.parseLong(key), tag.getDouble(key));
        }
        return data;
    }

    double add(long key, double amount) {
        double value = wear.getOrDefault(key, 0.0) + amount;
        wear.put(key, value);
        setDirty();
        return value;
    }

    double value(long key) { return wear.getOrDefault(key, 0.0); }

    void reset(long key) { wear.remove(key); setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        wear.forEach((key, value) -> tag.putDouble(Long.toString(key), value));
        return tag;
    }
}
