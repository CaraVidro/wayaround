package net.caravidro.wayaround.industrial.power.mechanical;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class ShaftBlock extends BaseEntityBlock {
    public static final MapCodec<ShaftBlock> CODEC = simpleCodec(ShaftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public ShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override protected MapCodec<ShaftBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
            : createTickerHelper(type, PowerContent.SHAFT_ENTITY.get(), ShaftBlockEntity::serverTick);
    }
}
