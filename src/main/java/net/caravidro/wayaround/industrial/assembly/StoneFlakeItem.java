package net.caravidro.wayaround.industrial.assembly;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class StoneFlakeItem extends Item {
    public StoneFlakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AssemblyPartProfile part = AssemblyItemData.readPart(stack);
        if (part != null) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.wayaround.stone_flake.quality",
                    percent(part.quality())
            ).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.wayaround.stone_flake.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int percent(float value) {
        return Math.round(value * 100.0F);
    }
}
