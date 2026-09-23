package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.blue.BlueManager;
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

public final class BlueItem extends Item {

    public static final int USE_DURATION = 72000;

    public BlueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        player.startUsingItem(hand);

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            BlueManager.begin(serverPlayer);
        }

        return InteractionResultHolder.consume(
                player.getItemInHand(hand)
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity livingEntity,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (!level.isClientSide
                && livingEntity instanceof ServerPlayer player) {
            BlueManager.tickHeld(player);
        }
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity livingEntity,
            int timeLeft
    ) {
        if (!level.isClientSide
                && livingEntity instanceof ServerPlayer player) {
            BlueManager.release(player);
        }
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
                        "tooltip.wayaround.blue.control"
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.blue.sling"
                )
        );
    }
}
