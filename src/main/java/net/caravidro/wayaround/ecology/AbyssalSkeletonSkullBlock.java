package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;

/** Vanilla skeleton skull geometry/block entity, with a persistent water source. */
public final class AbyssalSkeletonSkullBlock extends SkullBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<AbyssalSkeletonSkullBlock> CODEC = simpleCodec(AbyssalSkeletonSkullBlock::new);
    public AbyssalSkeletonSkullBlock(BlockBehaviour.Properties properties) {
        super(SkullBlock.Types.SKELETON, properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
    }
    @Override protected MapCodec<? extends SkullBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.WATERLOGGED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER));
    }
    @Override protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
