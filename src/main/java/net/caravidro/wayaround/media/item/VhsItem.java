package net.caravidro.wayaround.media.item;

import java.util.List;
import java.util.Locale;

import net.caravidro.wayaround.media.VhsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class VhsItem
        extends Item {

    public VhsItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        VhsData.read(stack)
                .ifPresent(
                        info -> {
                            double seconds =
                                    info.durationMillis()
                                            / 1000.0;

                            tooltip.add(
                                    Component.translatable(
                                                    "tooltip.wayaround.vhs.duration",
                                                    String.format(
                                                            Locale.ROOT,
                                                            "%.1fs",
                                                            seconds
                                                    )
                                            )
                                            .withStyle(
                                                    ChatFormatting.GRAY
                                            )
                            );

                            tooltip.add(
                                    Component.translatable(
                                                    "tooltip.wayaround.vhs.recording",
                                                    shortId(
                                                            info.recordingId()
                                                    )
                                            )
                                            .withStyle(
                                                    ChatFormatting.DARK_GRAY
                                            )
                            );
                        }
                );
    }

    private static String shortId(
            String id
    ) {
        if (id.length() <= 12) {
            return id;
        }

        return id.substring(
                0,
                12
        );
    }
}
