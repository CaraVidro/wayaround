package net.caravidro.wayaround.media;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class TelevisionBlock
        extends BaseEntityBlock {

    public static final MapCodec<TelevisionBlock> CODEC =
            simpleCodec(
                    TelevisionBlock::new
            );

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty
            FACING =
            HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty EJECTED =
            BooleanProperty.create(
                    "ejected"
            );

    public TelevisionBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                EJECTED,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<TelevisionBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                )
                .setValue(
                        EJECTED,
                        false
                );
    }

    @Override
    public boolean hasDynamicLightEmission(
            BlockState state
    ) {
        return true;
    }

    @Override
    public int getLightEmission(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        BlockEntity entity =
                level.getBlockEntity(
                        pos
                );

        if (entity
                instanceof TelevisionBlockEntity television
                && (
                television.isPlaying()
                        || television.isCountingDown()
        )) {

            return 5;
        }

        return 0;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new TelevisionBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
            BlockEntityTicker<T> getTicker(
                    Level level,
                    BlockState state,
                    BlockEntityType<T> type
            ) {
        return level.isClientSide
                ? createTickerHelper(
                        type,
                        MediaContent.TELEVISION_ENTITY.get(),
                        TelevisionBlockEntity::clientTick
                )
                : createTickerHelper(
                        type,
                        MediaContent.TELEVISION_ENTITY.get(),
                        TelevisionBlockEntity::serverTick
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
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.MEDIA)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(
                MediaContent.VHS.get()
        )) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (VhsData.read(stack)
                .isEmpty()) {

            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.media.invalid_vhs"
                        ),
                        true
                );
            }

            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof TelevisionBlockEntity television) {

            if (!television.insert(
                    stack
            )) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.media.tv_busy"
                        ),
                        true
                );

                return ItemInteractionResult.FAIL;
            }

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.media.vhs_inserted"
                    ),
                    true
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.MEDIA)) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos)
                instanceof TelevisionBlockEntity television
                && television.isEjected()) {

            if (!level.isClientSide) {
                ItemStack tape =
                        television.takeEjected();

                if (!tape.isEmpty()
                        && !player.addItem(tape)) {

                    player.drop(
                            tape,
                            false
                    );
                }
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(
                replacement.getBlock()
        )
                && !level.isClientSide
                && level.getBlockEntity(pos)
                instanceof TelevisionBlockEntity television) {

            television.dropTape();
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                EJECTED
        );
    }
}
