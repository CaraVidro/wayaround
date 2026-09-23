package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Structural support/beam for water wheels.
 *
 * It has no mechanics of its own. The six connection properties only make
 * touching supports, solid structures and wheel bodies join visually.
 */
public final class WaterWheelSupportBlock
        extends PipeBlock {

    public static final MapCodec<WaterWheelSupportBlock> CODEC =
            simpleCodec(WaterWheelSupportBlock::new);

    public WaterWheelSupportBlock(
            Properties properties
    ) {
        super(
                0.1875F,
                properties
        );

        BlockState initial =
                stateDefinition.any();

        for (Direction direction :
                Direction.values()) {
            initial =
                    initial.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            false
                    );
        }

        registerDefaultState(
                initial
        );
    }

    @Override
    protected MapCodec<WaterWheelSupportBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockState state =
                defaultBlockState();

        for (Direction direction :
                Direction.values()) {

            BlockPos neighbor =
                    context.getClickedPos()
                            .relative(direction);

            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            connects(
                                    context.getLevel(),
                                    neighbor,
                                    direction.getOpposite()
                            )
                    );
        }

        return state;
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
        return state.setValue(
                PROPERTY_BY_DIRECTION.get(direction),
                connects(
                        level,
                        neighborPos,
                        direction.getOpposite()
                )
        );
    }

    private static boolean connects(
            LevelAccessor level,
            BlockPos pos,
            Direction neighborFace
    ) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }

        BlockState state =
                level.getBlockState(pos);

        if (state.getBlock()
                instanceof WaterWheelSupportBlock
                || state.getBlock()
                instanceof WaterWheelHubBlock) {
            return true;
        }

        return state.isFaceSturdy(
                level,
                pos,
                neighborFace
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                NORTH,
                EAST,
                SOUTH,
                WEST,
                UP,
                DOWN
        );
    }
}
