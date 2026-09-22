package net.caravidro.wayaround.worldgen.weather.frost;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public final class FrostData extends SavedData {
    public record Coating(BlockState state, int faces) {}
    private final Map<Long, Map<BlockPos, Coating>> chunks = new HashMap<>();

    public static FrostData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FrostData::new, FrostData::load), "wayaround_frost");
    }

    public Map<BlockPos, Coating> chunk(long key) { return chunks.getOrDefault(key, Map.of()); }

    public Coating at(BlockPos pos) { return chunk(new ChunkPos(pos).toLong()).get(pos); }

    public void put(BlockPos pos, BlockState state, int faces) {
        long chunk = new ChunkPos(pos).toLong();
        if (faces == 0) {
            Map<BlockPos, Coating> entries = chunks.get(chunk);
            if (entries == null || entries.remove(pos) == null) return;
            if (entries.isEmpty()) chunks.remove(chunk);
        } else {
            chunks.computeIfAbsent(chunk, ignored -> new HashMap<>()).put(pos.immutable(), new Coating(state, faces));
        }
        setDirty();
    }

    private static FrostData load(CompoundTag tag, HolderLookup.Provider registries) {
        FrostData data = new FrostData();
        ListTag entries = tag.getList("coatings", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), entry.getCompound("state"));
            if (!state.isAir()) data.put(BlockPos.of(entry.getLong("pos")), state, entry.getInt("faces"));
        }
        data.setDirty(false);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        chunks.values().forEach(chunk -> chunk.forEach((pos, coating) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            entry.putInt("faces", coating.faces());
            entry.put("state", NbtUtils.writeBlockState(coating.state()));
            entries.add(entry);
        }));
        tag.put("coatings", entries);
        return tag;
    }
}
