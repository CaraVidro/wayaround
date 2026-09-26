package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.TUKUNA_SYSTEM
        )) {
            return InteractionResultHolder.pass(
                    player.getItemInHand(
                            hand
                    )
            );
        }

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
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!WorldFeatureRuntime.enabled(
                player.level(),
                WorldFeature.TUKUNA_SYSTEM
        )) {
            return InteractionResult.PASS;
        }

        if (!(target instanceof Player)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide
                && player instanceof ServerPlayer actor
                && target instanceof ServerPlayer receptacle) {
            if (!TukunaManager.beginForcedFeed(actor, receptacle, stack)) {
                return InteractionResult.PASS;
            }
        }

        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity living,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.TUKUNA_SYSTEM
        )) {
            return;
        }

        if (!level.isClientSide
                && living instanceof ServerPlayer actor) {
            TukunaManager.tickForcedFeed(actor, stack);
        }
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity living,
            int timeCharged
    ) {
        if (!level.isClientSide
                && living instanceof ServerPlayer actor) {
            TukunaManager.cancelForcedFeed(actor);
        }
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
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.TUKUNA_SYSTEM
        )) {
            return stack;
        }

        if (!level.isClientSide
                && living instanceof ServerPlayer player) {

            boolean wasForceFeeding =
                    TukunaManager.isForceFeeding(
                            player
                    );

            boolean forced =
                    TukunaManager.finishForcedFeed(
                            player,
                            stack
                    );

            if (forced
                    || (!wasForceFeeding
                    && TukunaManager.consumeFinger(
                            player,
                            stack
                    ))) {
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
        tooltip.add(
                Component.translatable(
                                "tooltip.wayaround.tukuna_finger"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        )
        );

        tooltip.add(
                Component.literal(
                                "Há alguma coisa presa aqui."
                        )
                        .withStyle(
                                ChatFormatting.DARK_GRAY,
                                ChatFormatting.ITALIC
                        )
        );
    }
}
