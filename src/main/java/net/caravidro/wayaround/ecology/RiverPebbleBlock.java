package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RiverPebbleBlock
        extends Block
        implements SimpleWaterloggedBlock {

    public static final IntegerProperty COUNT =
            IntegerProperty.create("count", 1, 4);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public static final MapCodec<RiverPebbleBlock> CODEC =
            simpleCodec(RiverPebbleBlock::new);

    private static final VoxelShape[] SHAPES = {
            box(5, 0, 5, 11, 2, 11),
            box(2, 0, 3, 13, 3, 12),
            box(1, 0, 1, 15, 4, 14),
            box(1, 0, 1, 15, 5, 15)
    };

    public RiverPebbleBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(COUNT, 1)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid =
                context.getLevel()
                        .getFluidState(context.getClickedPos());

        BlockState existing =
                context.getLevel()
                        .getBlockState(context.getClickedPos());

        if (existing.is(this)) {
            return existing.setValue(
                    COUNT,
                    Math.min(4, existing.getValue(COUNT) + 1)
            );
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                )
                .setValue(
                        WATERLOGGED,
                        fluid.is(FluidTags.WATER)
                );
    }

    @Override
    protected boolean canBeReplaced(
            BlockState state,
            BlockPlaceContext context
    ) {
        return !context.isSecondaryUseActive()
                && context.getItemInHand().is(EcologyContent.RIVER_PEBBLES_ITEM.get())
                && state.getValue(COUNT) < 4
                || super.canBeReplaced(state, context);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPES[state.getValue(COUNT) - 1];
    }

    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.getValue(WATERLOGGED)) {
            return;
        }

        var current =
                WaterDynamics.current(level, pos);

        double speed =
                WaterDynamics.speed(current);

        Direction direction =
                WaterDynamics.dominantDirection(current);

        if (direction == null
                || speed < 0.045
                || random.nextDouble() > Math.min(0.42, speed * 1.8)) {
            return;
        }

        BlockPos target =
                pos.relative(direction);

        BlockState targetState =
                level.getBlockState(target);

        if (targetState.is(this)
                && targetState.getValue(WATERLOGGED)
                && targetState.getValue(COUNT) < 4) {

            level.setBlockAndUpdate(
                    target,
                    targetState
                            .setValue(
                                    COUNT,
                                    targetState.getValue(COUNT) + 1
                            )
                            .setValue(
                                    FACING,
                                    direction
                            )
            );

            removeOne(level, pos, state);
            return;
        }

        if (!level.getFluidState(target).is(FluidTags.WATER)) {
            return;
        }

        if (!level.getBlockState(target.below())
                .isCollisionShapeFullBlock(level, target.below())) {
            return;
        }

        level.setBlockAndUpdate(
                target,
                defaultBlockState()
                        .setValue(COUNT, 1)
                        .setValue(FACING, direction)
                        .setValue(WATERLOGGED, true)
        );

        removeOne(level, pos, state);
    }

    private static void removeOne(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        int count = state.getValue(COUNT);

        if (count <= 1) {
            level.setBlockAndUpdate(
                    pos,
                    state.getValue(WATERLOGGED)
                            ? Fluids.WATER.defaultFluidState()
                                    .createLegacyBlock()
                            : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            );
        } else {
            level.setBlockAndUpdate(
                    pos,
                    state.setValue(COUNT, count - 1)
            );
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(COUNT, FACING, WATERLOGGED);
    }
}
