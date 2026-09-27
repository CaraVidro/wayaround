package net.caravidro.wayaround.media.broadcast;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BroadcastAntennaBlockEntity extends BlockEntity {
    private int frequencyKHz = BroadcastFrequency.DEFAULT_KHZ;

    public BroadcastAntennaBlockEntity(BlockPos pos, BlockState state) {
        super(MediaContent.BROADCAST_ANTENNA_ENTITY.get(), pos, state);
    }

    public int frequencyKHz() {
        return frequencyKHz;
    }

    public void tune(int direction) {
        frequencyKHz = BroadcastFrequency.step(frequencyKHz, direction);
        setChanged();
    }

    public int towerHeight() {
        if (level == null) return 1;
        int height = 1;
        BlockPos cursor = worldPosition.above();
        while (height < 16 && level.getBlockState(cursor).is(MediaContent.BROADCAST_ANTENNA.get())) {
            height++;
            cursor = cursor.above();
        }
        cursor = worldPosition.below();
        while (height < 16 && level.getBlockState(cursor).is(MediaContent.BROADCAST_ANTENNA.get())) {
            height++;
            cursor = cursor.below();
        }
        return height;
    }

    public double rangeBlocks() {
        return Math.min(5_120.0, 640.0 + (towerHeight() - 1) * 320.0);
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, BroadcastAntennaBlockEntity antenna) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && level.getGameTime() % 20L == 0L) {
            BroadcastManager.heartbeatAntenna(server, pos);
            BroadcastManager.tickAntenna(server, antenna);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FrequencyKHz", frequencyKHz);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequencyKHz = BroadcastFrequency.clamp(
                tag.contains("FrequencyKHz") ? tag.getInt("FrequencyKHz") : BroadcastFrequency.DEFAULT_KHZ);
    }
}
