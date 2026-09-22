package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class BehaviorData extends SavedData {
    private final Map<UUID,PlayerBehaviorProfile> profiles=new HashMap<>();
    public static BehaviorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BehaviorData::new,BehaviorData::load),"wayaround_behavior");
    }
    public PlayerBehaviorProfile profile(UUID id) { return profiles.computeIfAbsent(id,key->new PlayerBehaviorProfile()); }
    private static BehaviorData load(CompoundTag tag,HolderLookup.Provider registries) {
        BehaviorData data=new BehaviorData();
        for(String key:tag.getAllKeys()) try { data.profiles.put(UUID.fromString(key),PlayerBehaviorProfile.load(tag.getCompound(key))); }
        catch(IllegalArgumentException ignored) {}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        profiles.forEach((id,p)->tag.put(id.toString(),p.save()));return tag;
    }
}
