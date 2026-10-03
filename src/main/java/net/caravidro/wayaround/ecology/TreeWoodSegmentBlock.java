package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
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
                properties.dynamicShape()
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
        return connectedShape(state, level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return connectedShape(state, level, pos);
    }

    /** Only six adjacent cells are inspected, never requesting an unloaded chunk. */
    public static int branchConnections(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(ROOT)) return 0;
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == state.getValue(AXIS)) continue;
            BlockPos neighbor = pos.relative(direction);
            if (level instanceof LevelReader reader && !reader.hasChunkAt(neighbor)) continue;
            BlockState other = level.getBlockState(neighbor);
            if (other.is(BlockTags.LOGS) || (other.getBlock() instanceof TreeWoodSegmentBlock && !other.getValue(ROOT)))
                mask |= 1 << direction.ordinal();
        }
        return mask;
    }

    public static double branchWidth(int thickness) {
        return switch (thickness) { case 1 -> 4; case 2 -> 7; case 3 -> 10; default -> 14; };
    }

    private static final java.util.Map<Integer, VoxelShape> JOINT_SHAPES = new java.util.concurrent.ConcurrentHashMap<>();
    private static VoxelShape connectedShape(BlockState state, BlockGetter level, BlockPos pos) {
        int mask = branchConnections(state, level, pos);
        if (mask == 0) return shapeFor(state);
        int key = index(state.getValue(AXIS), state.getValue(THICKNESS), false) * 64 + mask;
        return JOINT_SHAPES.computeIfAbsent(key, ignored -> {
            VoxelShape result = shapeFor(state);
            double min = 8 - branchWidth(state.getValue(THICKNESS)) / 2, max = 16 - min;
            for (Direction direction : Direction.values()) {
                if ((mask & (1 << direction.ordinal())) == 0) continue;
                double x0=min,y0=min,z0=min,x1=max,y1=max,z1=max;
                switch (direction) {
                    case DOWN -> { y0=0; y1=min; }
                    case UP -> { y0=max; y1=16; }
                    case NORTH -> { z0=0; z1=min; }
                    case SOUTH -> { z0=max; z1=16; }
                    case WEST -> { x0=0; x1=min; }
                    case EAST -> { x0=max; x1=16; }
                }
                result = Shapes.or(result, box(x0,y0,z0,x1,y1,z1));
            }
            return result.optimize();
        });
    }

    private static VoxelShape shapeFor(
            BlockState state
    ) {
        return SHAPES[index(state.getValue(AXIS), state.getValue(THICKNESS), state.getValue(ROOT))];
    }

    private static int index(Direction.Axis axis, int thickness, boolean root) {
        return (root ? 12 : 0) + axis.ordinal() * 4 + thickness - 1;
    }

    private static final VoxelShape[] SHAPES = buildShapes();
    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[24];
        for (Direction.Axis axis : Direction.Axis.values())
            for (int thickness = 1; thickness <= 4; thickness++)
                for (boolean root : new boolean[]{false, true})
                    shapes[index(axis, thickness, root)] = createShape(axis, thickness, root);
        return shapes;
    }

    private static VoxelShape createShape(Direction.Axis axis, int thickness, boolean root) {
        double width = branchWidth(thickness);
        double min = 8.0 - width * .5, max = 8.0 + width * .5;

        if (root
                && axis
                == Direction.Axis.Y) {

            /*
             * A vertical root segment is a real terrain-following knuckle:
             * a central descending stem plus low X/Z feet. The feet make the
             * piece touch the previous/next horizontal root instead of looking
             * like an isolated stick when the terrain drops.
             */
            double rootHeight =
                    2.0
                            + thickness;

            return Shapes.or(
                    box(
                            min,
                            0.0,
                            min,
                            max,
                            16.0,
                            max
                    ),
                    box(
                            0.0,
                            0.0,
                            min,
                            16.0,
                            rootHeight,
                            max
                    ),
                    box(
                            min,
                            0.0,
                            0.0,
                            max,
                            rootHeight,
                            16.0
                    )
            );
        }

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

        if (root) {
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
