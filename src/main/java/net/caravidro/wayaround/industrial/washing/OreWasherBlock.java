package net.caravidro.wayaround.industrial.washing;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.industrial.crushing.CrusherContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OreWasherBlock extends BaseEntityBlock {

    public static final MapCodec<OreWasherBlock> CODEC =
            simpleCodec(
                    OreWasherBlock::new
            );

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public OreWasherBlock(
            Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return box(
                1.0,
                0.0,
                1.0,
                15.0,
                15.0,
                15.0
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new OreWasherBlockEntity(
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
                        OreWashingContent.ORE_WASHER_ENTITY.get(),
                        OreWasherBlockEntity::serverTick
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
        ) instanceof OreWasherBlockEntity washer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(
                OreWashingContent.WASHER_DRUM.get()
        )
                || stack.is(
                OreWashingContent.WASHER_SCREEN.get()
        )) {

            if (!level.isClientSide) {
                washer.installPart(
                        player,
                        stack
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(
                CrusherContent.CRUSHED_IRON.get()
        )
                || stack.is(
                CrusherContent.CRUSHED_COPPER.get()
        )
                || stack.is(
                CrusherContent.CRUSHED_GOLD.get()
        )) {

            if (!level.isClientSide) {
                washer.insertOre(
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
        ) instanceof OreWasherBlockEntity washer)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                washer.removePart(
                        player
                );

            } else if (!washer.collectOutput(
                    player
            )) {
                washer.describe(
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
        ) instanceof OreWasherBlockEntity washer) {

            washer.dropContents();
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING
        );
    }
}
