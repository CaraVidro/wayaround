package net.caravidro.wayaround.media.item;

import net.caravidro.wayaround.media.MediaClientBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class CameraItem
        extends Item {

    private static final int HOLD_TICKS =
            14;

    private static final int USE_DURATION =
            72_000;

    public CameraItem(
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
        ItemStack stack =
                player.getItemInHand(hand);

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
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity living,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (!level.isClientSide) {
            return;
        }

        int usedTicks =
                USE_DURATION
                        - remainingUseDuration;

        if (usedTicks == HOLD_TICKS) {
            MediaClientBridge
                    .cameraLongPress();
        }
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity living,
            int timeLeft
    ) {
        if (!level.isClientSide) {
            return;
        }

        int usedTicks =
                USE_DURATION
                        - timeLeft;

        if (usedTicks < HOLD_TICKS) {
            MediaClientBridge
                    .cameraShortPress();
        }
    }
}
