package net.caravidro.wayaround.media.broadcast;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BroadcastMicrophoneBlockEntity extends BlockEntity {
    public BroadcastMicrophoneBlockEntity(BlockPos pos, BlockState state) {
        super(MediaContent.BROADCAST_MICROPHONE_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, BroadcastMicrophoneBlockEntity microphone) {
        if (level instanceof ServerLevel server && level.getGameTime() % 20L == 0L) {
            BroadcastManager.heartbeatMicrophone(server, pos);
        }
    }
}
