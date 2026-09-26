package net.caravidro.wayaround.spectral;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class SpectralCompassItem
        extends CompassItem
        implements SpectralObject {

    public SpectralCompassItem(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public boolean isFoil(
            ItemStack stack
    ) {
        return true;
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
                        "Its north is not geographic."
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        SpectralTraitData.appendHiddenHint(
                stack,
                tooltip
        );
    }
}
