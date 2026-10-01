package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A narrow piece of living wood used for roots, branches and tapered trunk
 * tips. The block reaches the edge of its cell along its axis, so neighbouring
 * pieces visually connect instead of becoming floating sticks.
 */
public final class TreeWoodSegmentBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.AXIS;

    public static final IntegerProperty THICKNESS =
            IntegerProperty.create(
                    "thickness",
                    1,
                    4
            );

    public static final BooleanProperty ROOT =
            BooleanProperty.create(
                    "root"
            );

    public static final MapCodec<TreeWoodSegmentBlock> CODEC =
            simpleCodec(
                    TreeWoodSegmentBlock::new
            );

    public TreeWoodSegmentBlock(
            BlockBehaviour.Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                AXIS,
                                Direction.Axis.Y
                        )
                        .setValue(
                                THICKNESS,
                                4
                        )
                        .setValue(
                                ROOT,
                                false
                        )
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
        return shapeFor(
                state
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(
                state
        );
    }

    private static VoxelShape shapeFor(
            BlockState state
    ) {
        int thickness =
                state.getValue(
                        THICKNESS
                );

        double width =
                switch (thickness) {
                    case 1 -> 4.0;
                    case 2 -> 7.0;
                    case 3 -> 10.0;
                    default -> 14.0;
                };

        double min =
                8.0
                        - width * 0.5;

        double max =
                8.0
                        + width * 0.5;

        Direction.Axis axis =
                state.getValue(
                        AXIS
                );

        if (axis
                == Direction.Axis.Y) {
            return box(
                    min,
                    0.0,
                    min,
                    max,
                    16.0,
                    max
            );
        }

        if (state.getValue(
                ROOT
        )) {
            double rootHeight =
                    2.0
                            + thickness;

            if (axis
                    == Direction.Axis.X) {
                return box(
                        0.0,
                        0.0,
                        min,
                        16.0,
                        rootHeight,
                        max
                );
            }

            return box(
                    min,
                    0.0,
                    0.0,
                    max,
                    rootHeight,
                    16.0
            );
        }

        if (axis
                == Direction.Axis.X) {
            return box(
                    0.0,
                    min,
                    min,
                    16.0,
                    max,
                    max
            );
        }

        return box(
                min,
                min,
                0.0,
                max,
                max,
                16.0
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                AXIS,
                THICKNESS,
                ROOT
        );
    }
}
