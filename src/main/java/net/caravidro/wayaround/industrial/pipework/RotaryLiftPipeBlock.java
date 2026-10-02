package net.caravidro.wayaround.industrial.pipework;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A mechanically-driven intake head built from one existing liquid-pipe family.
 *
 * The lower mouth is the intake. FACING is the discharge direction into the
 * ordinary pipe network. Rotation is supplied by an adjacent mechanical source.
 */
public final class RotaryLiftPipeBlock
        extends IndustrialPipeBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public RotaryLiftPipeBlock(
            PipeSpec spec,
            Properties properties
    ) {
        super(
                spec,
                properties
        );

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    public boolean physicalTransfer() {
        return RotaryLiftRules.physicalTransfer(
                spec()
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockState state =
                super.getStateForPlacement(
                        context
                );

        return state == null
                ? null
                : state.setValue(
                        FACING,
                        context.getHorizontalDirection()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    net.minecraft.world.level.block.Block,
                    BlockState
                    > builder
    ) {
        super.createBlockStateDefinition(
                builder
        );

        builder.add(
                FACING
        );
    }
}
