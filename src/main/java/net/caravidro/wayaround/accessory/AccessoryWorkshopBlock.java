package net.caravidro.wayaround.accessory;

import com.mojang.serialization.MapCodec;

import net.caravidro.wayaround.network.AccessoryWorkshopOpenS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AccessoryWorkshopBlock
        extends Block {

    public static final MapCodec<AccessoryWorkshopBlock> CODEC =
            simpleCodec(
                    AccessoryWorkshopBlock::new
            );

    public AccessoryWorkshopBlock(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    protected MapCodec<AccessoryWorkshopBlock> codec() {
        return CODEC;
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
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.ACCESSORIES
        )) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(stack.getItem()
                instanceof AccessoryItem accessory)
                || !AccessoryCustomizationData.supported(
                accessory.kind()
        )) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            open(
                    serverPlayer,
                    pos,
                    hand,
                    stack,
                    accessory.kind()
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
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.ACCESSORIES
        )) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        for (InteractionHand hand :
                InteractionHand.values()) {
            ItemStack stack =
                    player.getItemInHand(
                            hand
                    );

            if (stack.getItem()
                    instanceof AccessoryItem accessory
                    && AccessoryCustomizationData.supported(
                    accessory.kind()
            )) {
                open(
                        serverPlayer,
                        pos,
                        hand,
                        stack,
                        accessory.kind()
                );

                return InteractionResult.SUCCESS;
            }
        }

        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "accessory.workshop.need_hat"
                ),
                true
        );

        return InteractionResult.SUCCESS;
    }

    private static void open(
            ServerPlayer player,
            BlockPos pos,
            InteractionHand hand,
            ItemStack stack,
            AccessoryKind kind
    ) {
        AccessoryCustomizationData.Config config =
                AccessoryCustomizationData.read(
                        stack,
                        kind
                );

        PacketDistributor.sendToPlayer(
                player,
                new AccessoryWorkshopOpenS2CPayload(
                        pos.asLong(),
                        hand == InteractionHand.MAIN_HAND
                                ? 0
                                : 1,
                        kind.path(),
                        config.material(),
                        config.size(),
                        config.extras(),
                        config.woolColor()
                )
        );
    }
}
