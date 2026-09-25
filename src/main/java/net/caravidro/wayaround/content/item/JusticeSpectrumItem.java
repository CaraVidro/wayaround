package net.caravidro.wayaround.content.item;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumItem;

import java.util.List;

import net.caravidro.wayaround.justice.JusticeDomainManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class JusticeSpectrumItem extends SpectrumItem {

    public JusticeSpectrumItem(
            Properties properties
    ) {
        super(SpectrumType.JUSTICE, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!player.level()
                .isClientSide
                && player
                instanceof ServerPlayer owner) {

            JusticeDomainManager.beginTrial(
                    owner,
                    target
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        if (!level.isClientSide) {
            player.sendSystemMessage(
                    Component.translatable(
                            "message.wayaround.justice.hint"
                    )
            );
        }

        return InteractionResultHolder.success(
                stack
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.justice_spectrum"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.justice_spectrum.domain"
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );
    }
}
