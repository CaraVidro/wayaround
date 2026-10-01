package net.caravidro.wayaround.industrial.grid;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class HighVoltageCableBlock
        extends PipeBlock {

    public static final MapCodec<HighVoltageCableBlock> CODEC =
            simpleCodec(
                    HighVoltageCableBlock::new
            );

    public HighVoltageCableBlock(
            Properties properties
    ) {
        super(
                0.085F,
                properties
        );

        BlockState initial =
                stateDefinition.any();

        for (Direction direction :
                Direction.values()) {
            initial =
                    initial.setValue(
                            PROPERTY_BY_DIRECTION.get(
                                    direction
                            ),
                            false
                    );
        }

        registerDefaultState(
                initial
        );
    }

    @Override
    protected MapCodec<HighVoltageCableBlock> codec() {
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
            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(
                                    direction
                            ),
                            connects(
                                    context.getLevel(),
                                    context.getClickedPos()
                                            .relative(
                                                    direction
                                            )
                            )
                    );
        }

        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        return state.setValue(
                PROPERTY_BY_DIRECTION.get(
                        side
                ),
                connects(
                        level,
                        neighborPos
                )
        );
    }

    private static boolean connects(
            LevelAccessor level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(
                pos
        )) {
            return false;
        }

        Block block =
                level.getBlockState(
                        pos
                )
                        .getBlock();

        return block instanceof HighVoltageCableBlock
                || block instanceof TransformerBlock;
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
