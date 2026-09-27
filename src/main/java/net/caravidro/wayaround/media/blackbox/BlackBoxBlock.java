package net.caravidro.wayaround.media.blackbox;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class BlackBoxBlock
        extends BaseEntityBlock {

    public static final MapCodec<BlackBoxBlock> CODEC =
            simpleCodec(
                    BlackBoxBlock::new
            );

    public static final BooleanProperty POWERED =
            BooleanProperty.create(
                    "powered"
            );

    public BlackBoxBlock(
            Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                POWERED,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<BlackBoxBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        POWERED,
                        context.getLevel()
                                .hasNeighborSignal(
                                        context.getClickedPos()
                                )
                );
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        boolean powered =
                level.hasNeighborSignal(
                        pos
                );

        if (powered
                != state.getValue(
                POWERED
        )) {
            level.setBlock(
                    pos,
                    state.setValue(
                            POWERED,
                            powered
                    ),
                    3
            );
        }
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (level.getBlockEntity(
                pos
        )
                instanceof BlackBoxBlockEntity box) {
            box.loadFromItem(
                    stack
            );
        }
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new BlackBoxBlockEntity(
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
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                MediaContent.BLACK_BOX_ENTITY.get(),
                BlackBoxBlockEntity::serverTick
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(
                pos
        )
                instanceof BlackBoxBlockEntity box)
                || !(player
                instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (box.isSealed()) {
            if (box.recordingId() != null) {
                BlackBoxManager.rewind(
                        serverPlayer,
                        box.recordingId()
                );
            }

            return InteractionResult.CONSUME;
        }

        if (!state.getValue(
                POWERED
        )) {
            player.displayClientMessage(
                    Component.literal(
                            "A Black Box precisa de sinal de redstone."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (box.isRecording()) {
            player.displayClientMessage(
                    Component.literal(
                            "● REC — a Black Box continua gravando."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (box.startRecording(
                serverPlayer
        )) {
            player.displayClientMessage(
                    Component.literal(
                            "● REC — Black Box ativa."
                    ),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal(
                            "A Black Box não conseguiu iniciar a gravação."
                    ),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        if (!level.isClientSide
                && player
                instanceof ServerPlayer serverPlayer
                && blockEntity
                instanceof BlackBoxBlockEntity box) {
            ItemStack sealed =
                    box.sealToItem(
                            serverPlayer
                    );

            ItemEntity entity =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.4,
                            pos.getZ() + 0.5,
                            sealed
                    );

            entity.setUnlimitedLifetime();

            level.addFreshEntity(
                    entity
            );
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                POWERED
        );
    }
}
