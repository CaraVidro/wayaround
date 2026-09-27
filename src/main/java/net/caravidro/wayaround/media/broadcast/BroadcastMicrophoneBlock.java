package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BroadcastMicrophoneBlock extends BaseEntityBlock {
    public static final MapCodec<BroadcastMicrophoneBlock> CODEC = simpleCodec(BroadcastMicrophoneBlock::new);

    public BroadcastMicrophoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BroadcastMicrophoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BroadcastMicrophoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, MediaContent.BROADCAST_MICROPHONE_ENTITY.get(), BroadcastMicrophoneBlockEntity::serverTick);
    }
}
