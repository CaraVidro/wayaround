package net.caravidro.wayaround.nexus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class NexustorBaseBlock extends BaseEntityBlock {

    public static final MapCodec<NexustorBaseBlock> CODEC =
            simpleCodec(NexustorBaseBlock::new);

    public NexustorBaseBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new NexustorBaseBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                NexusContent.NEXUSTOR_BASE_ENTITY.get(),
                NexustorBaseBlockEntity::serverTick
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (!level.isClientSide) {
            NexustorStructure.initializeBase(
                    level,
                    pos
            );
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof NexustorBaseBlockEntity reactor)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(NexusContent.NEXUSTOR_BODY.get())) {
            reactor.installBody(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(NexusContent.NEXUSTOR_FINGERS.get())) {
            reactor.installFinger(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(NexusContent.NEXUSTOR_HEAD.get())) {
            reactor.installHead(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(NexusContent.NEXUSTOETOR_ITEM.get())) {
            reactor.installCore(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof NexustorBaseBlockEntity reactor)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    reactor.status(),
                    false
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }
}
