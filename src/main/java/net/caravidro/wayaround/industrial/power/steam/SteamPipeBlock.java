package net.caravidro.wayaround.industrial.power.steam;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.industrial.power.thermal.BoilerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class SteamPipeBlock extends PipeBlock implements EntityBlock {
    public static final MapCodec<SteamPipeBlock> CODEC = simpleCodec(SteamPipeBlock::new);

    public SteamPipeBlock(Properties properties) {
        super(0.1875F, properties);
        BlockState initial = stateDefinition.any();
        for (Direction direction : Direction.values())
            initial = initial.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        registerDefaultState(initial);
    }

    @Override protected MapCodec<SteamPipeBlock> codec() { return CODEC; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(
                context.getLevel(), context.getClickedPos().relative(direction)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(level, neighborPos));
    }

    private static boolean connects(LevelAccessor level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof SteamPipeBlock || block instanceof SafetyValveBlock || block instanceof BoilerBlock)
            return true;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SteamNode;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != PowerContent.STEAM_PIPE_ENTITY.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof SteamPipeBlockEntity pipe)
                SteamPipeBlockEntity.serverTick(tickLevel, tickPos, tickState, pipe);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
}
