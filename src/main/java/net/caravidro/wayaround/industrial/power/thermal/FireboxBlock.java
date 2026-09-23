package net.caravidro.wayaround.industrial.power.thermal;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class FireboxBlock extends BaseEntityBlock {
    public static final MapCodec<FireboxBlock> CODEC = simpleCodec(FireboxBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final IntegerProperty FUEL_LEVEL = IntegerProperty.create("fuel_level", 0, 3);

    public FireboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(LIT, false)
            .setValue(OPEN, false)
            .setValue(FUEL_LEVEL, 0));
    }

    @Override protected MapCodec<FireboxBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, OPEN, FUEL_LEVEL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireboxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
            : createTickerHelper(type, PowerContent.FIREBOX_ENTITY.get(), FireboxBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.COAL) && !stack.is(Items.FLINT_AND_STEEL))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Fuel and ignition are deliberately physical: open the lid first.
        if (!state.getValue(OPEN))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox) {
            if (stack.is(Items.COAL)) {
                if (firebox.addCoal()) {
                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS,
                        0.35F, 0.65F + level.random.nextFloat() * 0.15F);
                }
            } else if (firebox.ignite()) {
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                    1.0F, 0.9F + level.random.nextFloat() * 0.2F);
                stack.hurtAndBreak(1, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean opening = !state.getValue(OPEN);
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(OPEN, opening), 3);
            level.playSound(null, pos,
                opening ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.65F, opening ? 0.85F : 0.75F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        burnOnContact(level, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        burnOnContact(level, state, entity);
        super.entityInside(state, level, pos, entity);
    }

    private static void burnOnContact(Level level, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(LIT) && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().hotFloor(), 1.0F);
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 30));
        }
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox
                && firebox.storedCoal() > 0) {
            popResource(level, pos, new ItemStack(Items.COAL, firebox.storedCoal()));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
