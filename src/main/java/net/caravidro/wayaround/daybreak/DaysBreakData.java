package net.caravidro.wayaround.daybreak;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class DaysBreakData extends SavedData {
    private boolean active;
    private long started;
    public boolean active(){return active;}
    public long started(){return started;}
    public void set(boolean enabled,long tick){if(active==enabled)return;active=enabled;if(enabled)started=tick;setDirty();}
    public static DaysBreakData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(DaysBreakData::new,DaysBreakData::load),"wayaround_then_days_break");}
    public static DaysBreakData load(CompoundTag tag,HolderLookup.Provider registries){var data=new DaysBreakData();data.active=tag.getBoolean("Active");data.started=tag.getLong("Started");return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){tag.putBoolean("Active",active);tag.putLong("Started",started);return tag;}
}
