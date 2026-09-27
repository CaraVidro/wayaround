package net.caravidro.wayaround.ecology;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent record of chunks that already received the initial ecology pass. */
public final class EcologyWorldData extends SavedData {

    private static final String ID =
            "wayaround_living_ecology_v1";

    private static final int MAX_TRACKED_CHUNKS =
            131072;

    private final Set<Long> seededChunks =
            new HashSet<>();

    public static EcologyWorldData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(
                        new Factory<>(
                                EcologyWorldData::new,
                                EcologyWorldData::load
                        ),
                        ID
                );
    }

    public boolean markSeeded(
            int chunkX,
            int chunkZ
    ) {
        long key =
                ChunkPos.asLong(
                        chunkX,
                        chunkZ
                );

        if (seededChunks.contains(key)) {
            return false;
        }

        if (seededChunks.size()
                >= MAX_TRACKED_CHUNKS) {
            return false;
        }

        seededChunks.add(key);
        setDirty();
        return true;
    }

    public boolean isSeeded(
            int chunkX,
            int chunkZ
    ) {
        return seededChunks.contains(
                ChunkPos.asLong(
                        chunkX,
                        chunkZ
                )
        );
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        long[] values =
                new long[
                        seededChunks.size()
                ];

        int index =
                0;

        for (long value : seededChunks) {
            values[index++] =
                    value;
        }

        tag.putLongArray(
                "SeededChunks",
                values
        );

        return tag;
    }

    private static EcologyWorldData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        EcologyWorldData data =
                new EcologyWorldData();

        long[] values =
                tag.getLongArray(
                        "SeededChunks"
                );

        for (long value : values) {
            if (data.seededChunks.size()
                    >= MAX_TRACKED_CHUNKS) {
                break;
            }

            data.seededChunks.add(
                    value
            );
        }

        return data;
    }
}
