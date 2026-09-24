package net.caravidro.wayaround.media.item;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.caravidro.wayaround.media.PhotoData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class PhotoItem
        extends Item {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    public PhotoItem(
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
        PhotoData.read(stack)
                .ifPresent(
                        info -> tooltip.add(
                                Component.translatable(
                                                "tooltip.wayaround.photo.taken",
                                                FORMAT.format(
                                                        Instant.ofEpochMilli(
                                                                        info.takenAt()
                                                                )
                                                                .atZone(
                                                                        ZoneId.systemDefault()
                                                                )
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.GRAY
                                        )
                        )
                );
    }
}
