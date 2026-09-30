package net.caravidro.wayaround.industrial.pipework;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class IndustrialPipeItem
        extends BlockItem {

    private final PipeSpec spec;

    public IndustrialPipeItem(
            IndustrialPipeBlock block,
            PipeSpec spec,
            Properties properties
    ) {
        super(
                block,
                properties
        );

        this.spec =
                spec;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        // Material and geometry communicate the use; no numerical specification HUD.
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
