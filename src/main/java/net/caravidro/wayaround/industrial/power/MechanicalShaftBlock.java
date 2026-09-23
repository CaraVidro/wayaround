package net.caravidro.wayaround.industrial.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MechanicalShaftBlock
        extends RotatedPillarBlock {

    private static final VoxelShape SHAFT_Y =
            Block.box(
                    5.0,
                    0.0,
                    5.0,
                    11.0,
                    16.0,
                    11.0
            );

    private static final VoxelShape SHAFT_X =
            Block.box(
                    0.0,
                    5.0,
                    5.0,
                    16.0,
                    11.0,
                    11.0
            );

    private static final VoxelShape SHAFT_Z =
            Block.box(
                    5.0,
                    5.0,
                    0.0,
                    11.0,
                    11.0,
                    16.0
            );

    public MechanicalShaftBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction.Axis axis =
                state.getValue(
                        AXIS
                );

        return axis == Direction.Axis.X
                ? SHAFT_X
                : axis == Direction.Axis.Z
                        ? SHAFT_Z
                        : SHAFT_Y;
    }
}
