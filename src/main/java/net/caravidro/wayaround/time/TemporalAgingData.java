package net.caravidro.wayaround.time;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Sparse per-dimension storage for low-frequency aging state. */
public final class TemporalAgingData extends SavedData {

    private static final String NAME = "wayaround_temporal_aging_v1";
    private static final int MAX_ENTRIES = 8192;

    private final LinkedHashMap<Long, TemporalState> entries = new LinkedHashMap<>();

    public static TemporalAgingData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        TemporalAgingData::new,
                        TemporalAgingData::load
                ),
                NAME
        );
    }

    public TemporalState state(BlockPos pos) {
        long key = pos.asLong();
        TemporalState existing = entries.get(key);
        if (existing != null) {
            entries.remove(key);
            entries.put(key, existing);
            return existing;
        }

        if (entries.size() >= MAX_ENTRIES) {
            Long oldest = entries.keySet().iterator().next();
            entries.remove(oldest);
        }

        TemporalState created = new TemporalState();
        entries.put(key, created);
        setDirty();
        return created;
    }

    public void forget(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag list = new ListTag();

        for (Map.Entry<Long, TemporalState> entry : entries.entrySet()) {
            CompoundTag row = entry.getValue().save();
            row.putLong("Pos", entry.getKey());
            list.add(row);
        }

        tag.put("Entries", list);
        return tag;
    }

    private static TemporalAgingData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        TemporalAgingData data = new TemporalAgingData();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size() && data.entries.size() < MAX_ENTRIES; i++) {
            CompoundTag row = list.getCompound(i);
            data.entries.put(
                    row.getLong("Pos"),
                    TemporalState.load(row)
            );
        }

        return data;
    }
}
