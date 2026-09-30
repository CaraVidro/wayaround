package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Physical table section used to extend the compact saw into a long saw line.
 *
 * Four sections (two on each side of the saw, along its facing axis) promote
 * the existing sawmill into long-line mode. These are real world blocks so the
 * larger machine has footprint/collision instead of being a renderer illusion.
 */
public final class SawmillTableExtensionBlock extends Block {

    public static final MapCodec<SawmillTableExtensionBlock> CODEC =
            simpleCodec(
                    SawmillTableExtensionBlock::new
            );

    private static final VoxelShape TOP =
            Block.box(
                    0.0,
                    9.0,
                    0.0,
                    16.0,
                    13.0,
                    16.0
            );

    private static final VoxelShape LEG_A =
            Block.box(
                    2.0,
                    0.0,
                    2.0,
                    5.0,
                    9.0,
                    5.0
            );

    private static final VoxelShape LEG_B =
            Block.box(
                    11.0,
                    0.0,
                    11.0,
                    14.0,
                    9.0,
                    14.0
            );

    private static final VoxelShape SHAPE =
            Shapes.or(
                    TOP,
                    LEG_A,
                    LEG_B
            );

    public SawmillTableExtensionBlock(
            BlockBehaviour.Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }
}
