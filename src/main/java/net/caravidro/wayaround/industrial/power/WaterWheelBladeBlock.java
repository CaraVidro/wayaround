package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Construction marker for a water-wheel paddle.
 *
 * Right click rotates the paddle face. The hub later compares this orientation
 * to local water current, so badly oriented paddles really are less efficient.
 */
public final class WaterWheelBladeBlock
        extends HorizontalDirectionalBlock
        implements SimpleWaterloggedBlock {

    public static final MapCodec<WaterWheelBladeBlock> CODEC =
            simpleCodec(WaterWheelBladeBlock::new);

    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    private static final VoxelShape NORTH_SOUTH =
            Block.box(
                    1.0,
                    0.5,
                    6.0,
                    15.0,
                    15.5,
                    10.0
            );

    private static final VoxelShape EAST_WEST =
            Block.box(
                    6.0,
                    0.5,
                    1.0,
                    10.0,
                    15.5,
                    15.0
            );

    public WaterWheelBladeBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                WATERLOGGED,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<WaterWheelBladeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        FluidState fluid =
                context.getLevel()
                        .getFluidState(
                                context.getClickedPos()
                        );

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                )
                .setValue(
                        WATERLOGGED,
                        fluid.getType()
                                == Fluids.WATER
                );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            Direction next =
                    state.getValue(FACING)
                            .getClockWise();

            level.setBlock(
                    pos,
                    state.setValue(
                            FACING,
                            next
                    ),
                    Block.UPDATE_ALL
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.wayaround.water_wheel_blade.angle",
                            next.getName()
                    ),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
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
    protected FluidState getFluidState(
            BlockState state
    ) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return state.getValue(FACING)
                        .getAxis()
                        == Direction.Axis.Z
                ? NORTH_SOUTH
                : EAST_WEST;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                WATERLOGGED
        );
    }
}
