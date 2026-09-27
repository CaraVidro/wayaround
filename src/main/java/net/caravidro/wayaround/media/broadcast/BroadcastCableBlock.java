package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BroadcastCableBlock extends Block {
    public static final MapCodec<BroadcastCableBlock> CODEC = simpleCodec(BroadcastCableBlock::new);

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public BroadcastCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        return defaultBlockState()
                .setValue(NORTH, connects(level.getBlockState(pos.north())))
                .setValue(EAST, connects(level.getBlockState(pos.east())))
                .setValue(SOUTH, connects(level.getBlockState(pos.south())))
                .setValue(WEST, connects(level.getBlockState(pos.west())));
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
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, connects(neighborState));
            case EAST -> state.setValue(EAST, connects(neighborState));
            case SOUTH -> state.setValue(SOUTH, connects(neighborState));
            case WEST -> state.setValue(WEST, connects(neighborState));
            default -> super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        };
    }

    private static boolean connects(BlockState state) {
        return state.is(MediaContent.BROADCAST_CABLE.get())
                || state.is(MediaContent.BROADCAST_ANTENNA.get())
                || state.is(MediaContent.EDITORIAL.get())
                || state.is(MediaContent.BROADCAST_MICROPHONE.get())
                || state.is(MediaContent.PLACED_CAMERA.get())
                || state.is(MediaContent.TELEVISION.get())
                || state.is(MediaContent.TV_ANTENNA.get());
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
        builder.add(NORTH, EAST, SOUTH, WEST);
    }
}
