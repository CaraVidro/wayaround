package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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

public final class MechanicalPressBlock extends BaseEntityBlock {

    public static final MapCodec<MechanicalPressBlock> CODEC =
            simpleCodec(
                    MechanicalPressBlock::new
            );

    public MechanicalPressBlock(
            Properties properties
    ) {
        super(
                properties
        );
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
        return new MechanicalPressBlockEntity(
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
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        PowerContent.MECHANICAL_PRESS_ENTITY.get(),
                        MechanicalPressBlockEntity::serverTick
                );
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
        if (!(level.getBlockEntity(
                pos
        ) instanceof MechanicalPressBlockEntity press)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (press.accepts(
                stack
        )) {
            if (!level.isClientSide) {
                press.insert(
                        player,
                        stack
                );
            }

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
        if (!(level.getBlockEntity(
                pos
        ) instanceof MechanicalPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                press.eject(
                        player
                );
            } else {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.mechanical_press.status",
                                press.inputName(),
                                press.inputCount(),
                                Math.round(
                                        press.progress()
                                ),
                                String.format(
                                        java.util.Locale.ROOT,
                                        "%.1f",
                                        press.rpm()
                                )
                        ),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(
                replacement.getBlock()
        )
                && !level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalPressBlockEntity press) {

            press.dropInput();
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }
}
