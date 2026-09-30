package net.caravidro.wayaround.industrial.economy;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Physical currency with a material reference value. The value is descriptive,
 * not a forced server exchange rate.
 */
public final class MineralCoinItem extends Item {

    private final String mineral;
    private final int referenceValue;

    public MineralCoinItem(
            Properties properties,
            String mineral,
            int referenceValue
    ) {
        super(
                properties
        );

        this.mineral =
                mineral;

        this.referenceValue =
                Math.max(
                        1,
                        referenceValue
                );
    }

    public String mineral() {
        return mineral;
    }

    public int referenceValue() {
        return referenceValue;
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
                        "tooltip.wayaround.coin.reference",
                        referenceValue
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.coin.free_market"
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );
    }
}
