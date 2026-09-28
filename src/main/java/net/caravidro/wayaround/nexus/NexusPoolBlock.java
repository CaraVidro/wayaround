package net.caravidro.wayaround.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NexusPoolBlock extends Block {

    private static final VoxelShape SHAPE =
            Block.box(
                    0.0,
                    0.0,
                    0.0,
                    16.0,
                    1.5,
                    16.0
            );

    public NexusPoolBlock(
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
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        BlockPos below =
                pos.below();

        return level.getBlockState(below)
                .isFaceSturdy(
                        level,
                        below,
                        Direction.UP
                );
    }
}
