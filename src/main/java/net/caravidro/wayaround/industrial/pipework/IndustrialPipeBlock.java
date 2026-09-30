package net.caravidro.wayaround.industrial.pipework;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class IndustrialPipeBlock extends PipeBlock implements net.minecraft.world.level.block.EntityBlock {

    private final PipeSpec spec;

    public IndustrialPipeBlock(
            PipeSpec spec,
            Properties properties
    ) {
        super(spec.radius(), properties);
        this.spec = spec;

        BlockState initial =
                stateDefinition.any();

        for (Direction direction : Direction.values()) {
            initial = initial.setValue(
                    PROPERTY_BY_DIRECTION.get(direction),
                    false
            );
        }

        registerDefaultState(initial);
    }

    public PipeSpec spec() {
        return spec;
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return MapCodec.unit(
                () -> this
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockState state =
                defaultBlockState();

        for (Direction direction : Direction.values()) {
            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            connects(
                                    context.getLevel(),
                                    context.getClickedPos().relative(direction)
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
                PROPERTY_BY_DIRECTION.get(side),
                connects(level, neighborPos)
        );
    }

    private boolean connects(
            LevelAccessor level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }

        Block other =
                level.getBlockState(pos)
                        .getBlock();

        return other instanceof IndustrialPipeBlock pipe
                && spec.compatible(pipe.spec());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) pipe.turn(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == PipeworkContent.PIPE_ENTITY.get() ? (world, position, block, entity) -> PipeBlockEntity.tick(world, position, block, (PipeBlockEntity) entity) : null;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.is(PipeworkContent.VALVE.get()) && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            if (!level.isClientSide) pipe.installValve(stack, player, player.getNearestViewDirection());
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) pipe.dropParts();
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return HollowPipeShape.shape(state, spec.radius());
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
