package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class SawmillBlock extends BaseEntityBlock {
    public static final MapCodec<SawmillBlock> CODEC = simpleCodec(SawmillBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SawmillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SawmillBlockEntity(pos, state);
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

        if (!level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof SawmillBlockEntity sawmill) {

            sawmill.restoreBodyFromItem(
                    stack
            );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, PowerContent.SAWMILL_ENTITY.get(), SawmillBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof SawmillBlockEntity sawmill)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(PowerContent.ASSEMBLY_HAMMER.get())) {
            if (!level.isClientSide) {
                sawmill.service(
                        player
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(PowerContent.SAW_BLADE.get())) {
            if (!level.isClientSide) sawmill.installBlade(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(PowerContent.MECHANICAL_SHAFT_ITEM.get())) {
            if (!level.isClientSide) sawmill.installShaft(player, stack);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(PowerContent.SAWMILL_CRANK.get())) {
            if (!level.isClientSide) {
                sawmill.crank(
                        player
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(ItemTags.LOGS)) {
            if (!level.isClientSide) sawmill.insertLog(player, stack);
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
        if (!(level.getBlockEntity(pos) instanceof SawmillBlockEntity sawmill)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                sawmill.removeLast(
                        player
                );
            } else if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                        sawmill
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
        if (!state.is(replacement.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof SawmillBlockEntity sawmill) {
            sawmill.dropAssembly();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
