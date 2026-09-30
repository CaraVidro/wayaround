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
        String media =
                spec.media()
                        .stream()
                        .map(
                                medium -> Component.translatable(
                                        "pipe_medium.wayaround."
                                                + medium.name()
                                                .toLowerCase(
                                                        java.util.Locale.ROOT
                                                )
                                ).getString()
                        )
                        .collect(
                                Collectors.joining(
                                        " / "
                                )
                        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipework.media",
                        media
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipework.flow",
                        spec.flowPerTick()
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipework.pressure",
                        spec.maxPressureBar()
                ).withStyle(
                        ChatFormatting.BLUE
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipework.temperature",
                        spec.maxTemperatureC()
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );
    }
}
