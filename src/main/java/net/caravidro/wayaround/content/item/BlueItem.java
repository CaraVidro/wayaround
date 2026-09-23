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
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {

            /*
             * Second click dismisses an already summoned Blue. It does not
             * enter item-use/charge again.
             */
            if (BlueManager.hasControllableBlue(
                    serverPlayer
            )) {
                BlueManager.releaseActive(
                        serverPlayer
                );

                return InteractionResultHolder.success(
                        stack
                );
            }

            if (!BlueManager.beginCharge(
                    serverPlayer
            )) {
                return InteractionResultHolder.fail(
                        stack
                );
            }
        }

        player.startUsingItem(
                hand
        );

        return InteractionResultHolder.consume(
                stack
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
        /*
         * The item itself is invisible and should not force the bow/block
         * pose. Hand motion is driven by explicit vanilla swing gestures.
         */
        return UseAnim.NONE;
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
            BlueManager.tickCharge(
                    player
            );
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
            BlueManager.finishCharge(
                    player
            );
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
