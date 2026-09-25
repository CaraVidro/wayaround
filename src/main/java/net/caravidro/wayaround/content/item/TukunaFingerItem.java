package net.caravidro.wayaround.content.item;

import java.util.List;
import java.util.UUID;

import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class TukunaFingerItem extends Item {

    public TukunaFingerItem(
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
        player.startUsingItem(
                hand
        );

        return InteractionResultHolder.consume(
                player.getItemInHand(
                        hand
                )
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity living
    ) {
        if (!level.isClientSide
                && living instanceof ServerPlayer player) {

            if (TukunaManager.consumeFinger(
                    player,
                    stack
            )) {
                stack.shrink(
                        1
                );
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        UUID owner =
                TukunaManager.fingerOwner(
                        stack
                );

        tooltip.add(
                Component.translatable(
                                "tooltip.wayaround.tukuna_finger"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        )
        );

        if (owner != null) {
            tooltip.add(
                    Component.literal(
                                    "Soul: "
                                            + owner.toString()
                                                    .substring(
                                                            0,
                                                            8
                                                    )
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }
    }
}
