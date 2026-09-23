package net.caravidro.wayaround.industrial.power.mechanical;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class GearboxBlock extends BaseEntityBlock {
    public static final MapCodec<GearboxBlock> CODEC = simpleCodec(GearboxBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final IntegerProperty RATIO = IntegerProperty.create("ratio", 0, 2);

    public GearboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(RATIO, 0));
    }

    @Override protected MapCodec<GearboxBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, RATIO);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
            : createTickerHelper(type, PowerContent.GEARBOX_ENTITY.get(), GearboxBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int next = (state.getValue(RATIO) + 1) % 3;
            level.setBlock(pos, state.setValue(RATIO, next), 3);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.45F, 0.75F + next * 0.15F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
