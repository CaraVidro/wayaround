package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared substrate for ocean-floor organisms. It keeps its water source and
 * disappears back into water if the supporting seabed is removed.
 */
public final class AquaticFloorLifeBlock
        extends Block
        implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public static final MapCodec<AquaticFloorLifeBlock> CODEC =
            simpleCodec(
                    AquaticFloorLifeBlock::new
            );

    private static final VoxelShape SHAPE =
            box(
                    2.0,
                    0.0,
                    2.0,
                    14.0,
                    10.0,
                    14.0
            );

    public AquaticFloorLifeBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                WATERLOGGED,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
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
                        WATERLOGGED,
                        fluid.is(
                                FluidTags.WATER
                        )
                );
    }

    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        if (!state.getValue(WATERLOGGED)
                && !level.getFluidState(pos)
                        .is(
                                FluidTags.WATER
                        )) {
            return false;
        }

        BlockPos floorPos =
                pos.below();

        return level.getBlockState(
                        floorPos
                )
                .isFaceSturdy(
                        level,
                        floorPos,
                        Direction.UP
                );
    }

    @Override
    protected FluidState getFluidState(
            BlockState state
    ) {
        return state.getValue(
                WATERLOGGED
        )
                ? Fluids.WATER
                        .getSource(false)
                : super.getFluidState(
                        state
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
        if (state.getValue(
                WATERLOGGED
        )) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER
                            .getTickDelay(
                                    level
                            )
            );
        }

        if (direction == Direction.DOWN
                && !state.canSurvive(
                level,
                pos
        )) {
            return state.getValue(
                    WATERLOGGED
            )
                    ? Fluids.WATER
                            .defaultFluidState()
                            .createLegacyBlock()
                    : net.minecraft.world.level.block.Blocks.AIR
                            .defaultBlockState();
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
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                WATERLOGGED
        );
    }
}
