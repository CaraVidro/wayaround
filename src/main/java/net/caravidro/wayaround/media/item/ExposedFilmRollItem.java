package net.caravidro.wayaround.media.item;

import java.util.List;

import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.media.VhsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ExposedFilmRollItem
        extends Item {

    public ExposedFilmRollItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack roll =
                player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(
                    roll,
                    true
            );
        }

        if (VhsData.read(roll)
                .isEmpty()) {

            return InteractionResultHolder.fail(
                    roll
            );
        }

        if (!MediaInventory.consumeOne(
                player,
                MediaContent.BLANK_VHS.get()
        )) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.media.need_blank_vhs"
                    ),
                    true
            );

            return InteractionResultHolder.fail(
                    roll
            );
        }

        ItemStack tape =
                new ItemStack(
                        MediaContent.VHS.get()
                );

        VhsData.copy(
                roll,
                tape
        );

        if (!player.getAbilities()
                .instabuild) {

            roll.shrink(1);
        }

        MediaInventory.giveOrDrop(
                player,
                tape
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.media.film_transferred"
                ),
                true
        );

        return InteractionResultHolder.success(
                roll
        );
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
                                "tooltip.wayaround.film.exposed"
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }
}
