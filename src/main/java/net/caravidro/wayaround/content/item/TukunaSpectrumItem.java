package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.cursed.Desmartelar;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class TukunaSpectrumItem extends Item {

    public TukunaSpectrumItem(
            Properties properties
    ) {
        super(properties);
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

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {

            if (!serverPlayer.getCooldowns()
                    .isOnCooldown(
                            this
                    )) {

                Desmartelar.cast(
                        serverPlayer,
                        20
                );

                serverPlayer.getCooldowns()
                        .addCooldown(
                                this,
                                28
                        );
            }
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
                                "tooltip.wayaround.tukuna_spectrum"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        )
        );

        tooltip.add(
                Component.translatable(
                                "tooltip.wayaround.tukuna_spectrum.desmartelar"
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }
}
