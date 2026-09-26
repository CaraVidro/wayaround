package net.caravidro.wayaround.spectral;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

public final class SpectralSwordItem
        extends SwordItem
        implements SpectralObject {

    public SpectralSwordItem(
            Tier tier,
            Properties properties
    ) {
        super(
                tier,
                properties
        );
    }

    @Override
    public boolean isFoil(
            ItemStack stack
    ) {
        return SpectralTraitData.discovered(
                stack
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal(
                        "It learned something before you did."
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        SpectralTraitData.appendHiddenHint(
                stack,
                tooltip
        );
    }
}
