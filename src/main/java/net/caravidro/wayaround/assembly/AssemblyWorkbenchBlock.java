package net.caravidro.wayaround.assembly;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

public final class AssemblyWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<AssemblyWorkbenchBlock> CODEC = simpleCodec(AssemblyWorkbenchBlock::new);

    public AssemblyWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
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
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(AssemblyContent.ASSEMBLY_HAMMER.get())) {
            if (!level.isClientSide) workbench.useHammer(player, player.isShiftKeyDown());
            return ItemInteractionResult.SUCCESS;
        }

        if (AssemblyWorkbenchBlockEntity.isSupportedInput(stack)) {
            if (!level.isClientSide) {
                int orientation = Math.floorMod(Math.round(player.getYRot() / 90.0F) * 90, 360);
                workbench.insert(player, stack, orientation);
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
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) workbench.removeLast(player);
            else workbench.describe(player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench) {
            workbench.dropParts(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
