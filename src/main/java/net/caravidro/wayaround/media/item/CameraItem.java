package net.caravidro.wayaround.media.item;

import net.caravidro.wayaround.media.MediaClientBridge;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.media.PlacedCameraBlock;
import net.caravidro.wayaround.media.PlacedCameraBlockEntity;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
    public InteractionResult useOn(
            UseOnContext context
    ) {
        if (!WorldFeatureRuntime.enabled(context.getLevel(), WorldFeature.MEDIA)) {
            return InteractionResult.PASS;
        }
        Player player =
                context.getPlayer();

        if (player == null
                || !player.isShiftKeyDown()) {

            return super.useOn(
                    context
            );
        }

        Level level =
                context.getLevel();

        BlockPos target =
                context.getClickedPos()
                        .relative(
                                context.getClickedFace()
                        );

        if (!level.getBlockState(
                target
        )
                .canBeReplaced()) {

            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction facing =
                player.getDirection();

        level.setBlock(
                target,
                MediaContent.PLACED_CAMERA
                        .get()
                        .defaultBlockState()
                        .setValue(
                                PlacedCameraBlock.FACING,
                                facing
                        ),
                Block.UPDATE_ALL
        );

        if (level.getBlockEntity(
                target
        )
                instanceof PlacedCameraBlockEntity camera) {

            camera.arm(
                    player.getUUID(),
                    facing
            );

            ItemStack offhand =
                    player.getOffhandItem();

            if (offhand.is(
                    MediaContent.BROADCAST_ANTENNA_ITEM.get()
            )
                    && camera.installIntegratedAntenna()
                    && !player.getAbilities()
                    .instabuild) {

                offhand.shrink(
                        1
                );
            }
        }

        if (!player.getAbilities()
                .instabuild) {

            context.getItemInHand()
                    .shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        if (!WorldFeatureRuntime.enabled(level, WorldFeature.MEDIA)) {
            return InteractionResultHolder.pass(stack);
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
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity living,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.MEDIA)
                || !level.isClientSide) {
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
        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.MEDIA)
                || !level.isClientSide) {
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
