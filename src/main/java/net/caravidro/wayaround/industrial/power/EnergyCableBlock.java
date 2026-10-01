package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Wires have no ticking block entity; a generating panel routes power through them. */
public final class EnergyCableBlock extends PipeBlock {
    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    public EnergyCableBlock(Properties properties) {
        super(0.125F, properties);
        BlockState initial = stateDefinition.any();
        for (Direction direction : Direction.values()) {
            initial = initial.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        }
        registerDefaultState(initial);
    }

    @Override
    protected MapCodec<EnergyCableBlock> codec() { return CODEC; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(
                context.getLevel(), context.getClickedPos().relative(direction), direction.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(level, neighborPos, side.getOpposite()));
    }

    private static boolean connects(LevelAccessor access, BlockPos pos, Direction side) {
        if (!access.hasChunkAt(pos)) return false;
        Block block = access.getBlockState(pos).getBlock();
        if (block instanceof EnergyCableBlock || block instanceof SolarPanelBlock) return true;
        return access instanceof Level level && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
}
