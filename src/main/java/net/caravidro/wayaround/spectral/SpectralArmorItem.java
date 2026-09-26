package net.caravidro.wayaround.spectral;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class SpectralArmorItem
        extends ArmorItem
        implements SpectralObject {

    public SpectralArmorItem(
            Properties properties
    ) {
        super(
                ArmorMaterials.IRON,
                Type.CHESTPLATE,
                properties
        );
    }

    @Override
    public boolean isFoil(
            ItemStack stack
    ) {
        return SpectralTraitData.armorAscended(
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
                        SpectralTraitData.armorAscended(
                                stack
                        )
                                ? "Remain in darkness to ascend the armor."
                                : "Go to the side of darkness."
                ).withStyle(
                        SpectralTraitData.armorAscended(
                                stack
                        )
                                ? ChatFormatting.AQUA
                                : ChatFormatting.DARK_PURPLE
                )
        );

        tooltip.add(
                Component.literal(
                        "Something else is stitched beneath the metal."
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );

        SpectralTraitData.appendHiddenHint(
                stack,
                tooltip
        );
    }
}
