package net.caravidro.wayaround.industrial.pipework;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class PipeBlockItem
        extends BlockItem {

    private final PipeProfile profile;

    public PipeBlockItem(
            PipeBlock block,
            PipeProfile profile,
            Properties properties
    ) {
        super(
                block,
                properties
        );

        this.profile =
                profile;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipe.class",
                        Component.translatable(
                                profile.pipeClass()
                                        == PipeProfile.PipeClass.LIQUID
                                        ? "tooltip.wayaround.pipe.liquid"
                                        : "tooltip.wayaround.pipe.gas"
                        )
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipe.capacity",
                        profile.capacity()
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipe.throughput",
                        profile.throughputPerTick()
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.pipe.pressure",
                        Math.round(
                                profile.maxPressureKpa()
                        )
                ).withStyle(
                        ChatFormatting.DARK_GRAY
                )
        );

        if (profile.hotSteamRated()) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.pipe.steam_rated"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );
        }
    }
}
