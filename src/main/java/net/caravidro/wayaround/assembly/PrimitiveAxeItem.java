package net.caravidro.wayaround.assembly;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

public final class PrimitiveAxeItem extends AxeItem {
    public PrimitiveAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AssemblyState state = AssemblyStackData.readAssembly(stack);
        if (state == null) return;

        tooltipComponents.add(Component.translatable("tooltip.wayaround.assembly.manufactured")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable(
                "tooltip.wayaround.assembly.quality", percent(state.overallQuality())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "tooltip.wayaround.assembly.alignment", percent(state.averageAlignment())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "tooltip.wayaround.assembly.durability", percent(state.durabilityScore())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "tooltip.wayaround.assembly.fatigue", percent(state.averageFatigue())
        ).withStyle(ChatFormatting.GRAY));
    }

    private static int percent(float value) {
        return Math.round(value * 100.0F);
    }
}
