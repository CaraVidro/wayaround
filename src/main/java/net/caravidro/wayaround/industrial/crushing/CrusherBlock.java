package net.caravidro.wayaround.industrial.crushing;

import javax.annotation.Nullable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CrusherBlock extends BaseEntityBlock {
    public static final MapCodec<CrusherBlock> CODEC = simpleCodec(p -> new CrusherBlock(CrusherSize.SMALL, p));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final CrusherSize size;
    public CrusherBlock(CrusherSize size, Properties properties) {
        super(properties); this.size = size;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public CrusherSize size() { return size; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return size == CrusherSize.SMALL ? box(2,0,2,14,12,14) : box(0,0,0,16,16,16);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrusherBlockEntity(pos,state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CrusherContent.ENTITY.get(), CrusherBlockEntity::serverTick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof MachinePartItem) {
            if (!level.isClientSide) crusher.install(player, stack);
            return ItemInteractionResult.SUCCESS;
        }
        if (CrushingRecipe.find(stack) != null) {
            if (!level.isClientSide) { int inserted = crusher.offer(stack); if (inserted == 0) crusher.describe(player);
                else if (!player.getAbilities().instabuild) stack.shrink(inserted); }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) crusher.removePart(player, hit);
                else if (!crusher.collectOutput(player)) crusher.describe(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } return InteractionResult.PASS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher)
            crusher.dropContents();
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
}
