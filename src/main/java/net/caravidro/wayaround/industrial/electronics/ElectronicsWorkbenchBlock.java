package net.caravidro.wayaround.industrial.electronics;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ElectronicsWorkbenchBlock
        extends BaseEntityBlock {

    public static final MapCodec<ElectronicsWorkbenchBlock> CODEC =
            simpleCodec(
                    ElectronicsWorkbenchBlock::new
            );

    public ElectronicsWorkbenchBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ElectronicsWorkbenchBlockEntity(
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
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
        ) instanceof ElectronicsWorkbenchBlockEntity workbench)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player.isShiftKeyDown()
                && workbench.hasCircuitBoard()) {
            if (!level.isClientSide
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                        workbench
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(
                ElectronicsContent.CIRCUIT_BOARD.get()
        )) {
            if (!level.isClientSide) {
                if (!workbench.installCircuitBoard(
                        player,
                        stack
                )) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.wayaround.circuit_workbench.board_present"
                            ),
                            true
                    );
                }
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
        ) instanceof ElectronicsWorkbenchBlockEntity workbench)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (!workbench.hasCircuitBoard()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.wayaround.circuit_workbench.no_board"
                        ),
                        true
                );

            } else if (player.isShiftKeyDown()
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                        workbench
                );

            } else {
                workbench.removeCircuitBoard(
                        player
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
        ) instanceof ElectronicsWorkbenchBlockEntity workbench) {
            workbench.dropCircuitBoard();
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
