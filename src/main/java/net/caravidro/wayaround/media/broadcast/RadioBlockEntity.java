package net.caravidro.wayaround.media.broadcast;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioBlockEntity extends BlockEntity {
    private int frequencyKHz = BroadcastFrequency.DEFAULT_KHZ;
    private int volumePercent = 70;
    private long lastSignalTick = -1000L;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(MediaContent.RADIO_ENTITY.get(), pos, state);
    }

    public int frequencyKHz() { return frequencyKHz; }
    public int volumePercent() { return volumePercent; }
    public float volume() { return volumePercent / 100.0F; }
    public long lastSignalTick() { return lastSignalTick; }

    public void tune(int direction) {
        frequencyKHz = BroadcastFrequency.step(frequencyKHz, direction);
        setChanged();
    }

    public void cycleVolume() {
        volumePercent += 25;
        if (volumePercent > 100) volumePercent = 0;
        setChanged();
    }

    public void markSignal(long tick) {
        lastSignalTick = tick;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, RadioBlockEntity radio) {
        if (level instanceof ServerLevel server && level.getGameTime() % 20L == 0L) {
            BroadcastManager.heartbeatRadio(server, pos);
            BroadcastManager.tickRadio(server, radio);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FrequencyKHz", frequencyKHz);
        tag.putInt("VolumePercent", volumePercent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequencyKHz = BroadcastFrequency.clamp(
                tag.contains("FrequencyKHz") ? tag.getInt("FrequencyKHz") : BroadcastFrequency.DEFAULT_KHZ);
        volumePercent = Math.max(0, Math.min(100,
                tag.contains("VolumePercent") ? tag.getInt("VolumePercent") : 70));
    }
}
