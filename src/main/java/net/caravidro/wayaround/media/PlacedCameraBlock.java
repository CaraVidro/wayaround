package net.caravidro.wayaround.media;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.caravidro.wayaround.network.PlacedCameraPickupS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlacedCameraBlock
        extends BaseEntityBlock {

    public static final MapCodec<PlacedCameraBlock> CODEC =
            simpleCodec(
                    PlacedCameraBlock::new
            );

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty
            FACING =
            HorizontalDirectionalBlock.FACING;

    public PlacedCameraBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected MapCodec<PlacedCameraBlock> codec() {
        return CODEC;
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
        return new PlacedCameraBlockEntity(
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
                MediaContent.PLACED_CAMERA_ENTITY.get(),
                PlacedCameraBlockEntity::serverTick
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
                instanceof PlacedCameraBlockEntity camera)) {

            return InteractionResult.PASS;
        }

        if (!camera.isOwner(
                player.getUUID()
        )) {
            return InteractionResult.FAIL;
        }

        level.removeBlock(
                pos,
                false
        );

        ItemStack cameraItem =
                new ItemStack(
                        MediaContent.CAMERA.get()
                );

        if (player.getMainHandItem()
                .isEmpty()) {

            player.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    cameraItem
            );

        } else {
            MediaInventory.giveOrDrop(
                    player,
                    cameraItem
            );
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new PlacedCameraPickupS2CPayload()
            );
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING
        );
    }
}
