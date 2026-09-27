package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

public final class BroadcastAntennaBlock extends BaseEntityBlock {
    public static final MapCodec<BroadcastAntennaBlock> CODEC = simpleCodec(BroadcastAntennaBlock::new);

    public BroadcastAntennaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BroadcastAntennaBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BroadcastAntennaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, MediaContent.BROADCAST_ANTENNA_ENTITY.get(), BroadcastAntennaBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.MEDIA)) return InteractionResult.PASS;
        BlockPos controller =
                pos;

        while (level.getBlockState(
                controller.below()
        ).is(
                MediaContent.BROADCAST_ANTENNA.get()
        )) {
            controller =
                    controller.below();
        }

        if (level.getBlockEntity(controller) instanceof BroadcastAntennaBlockEntity antenna) {
            if (!level.isClientSide) {
                antenna.tune(player.isShiftKeyDown() ? -1 : 1);
                player.displayClientMessage(
                        Component.literal("Antena: " + BroadcastFrequency.display(antenna.frequencyKHz())
                                + " | altura " + antenna.towerHeight()
                                + " | alcance ~" + Math.round(antenna.rangeBlocks()) + " blocos"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
