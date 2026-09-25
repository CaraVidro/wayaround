package net.caravidro.wayaround.content.item;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumItem;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class TukunaSpectrumItem extends SpectrumItem {

    public TukunaSpectrumItem(
            Properties properties
    ) {
        super(SpectrumType.TUKUNA, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        /*
         * The Spectrum only grants access. Desmartelar is now an empty-hand
         * gesture so the item never has to be held like a wand.
         */
        return InteractionResultHolder.pass(
                player.getItemInHand(
                        hand
                )
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
