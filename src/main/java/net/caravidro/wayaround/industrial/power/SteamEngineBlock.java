package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;

public final class SteamEngineBlock
        extends BaseEntityBlock {

    public static final MapCodec<SteamEngineBlock> CODEC =
            simpleCodec(
                    SteamEngineBlock::new
            );

    public static final BooleanProperty LIT =
            BlockStateProperties.LIT;

    public SteamEngineBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                LIT,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<SteamEngineBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                LIT
        );
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SteamEngineBlockEntity(
                pos,
                state
        );
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        PowerContent.STEAM_ENGINE_ENTITY.get(),
                        SteamEngineBlockEntity::serverTick
                );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(
                PowerContent.STEAM_PRESSURE_GAUGE.get()
        )
                && level.getBlockEntity(
                pos
        ) instanceof SteamEngineBlockEntity engine) {

            if (!level.isClientSide) {
                player.displayClientMessage(
                        engine.measurement(),
                        true
                );
            }

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof SteamEngineBlockEntity engine) {

            player.displayClientMessage(
                    engine.status(),
                    true
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }
}
