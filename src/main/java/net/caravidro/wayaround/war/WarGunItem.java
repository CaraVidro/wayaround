package net.caravidro.wayaround.war;

import java.util.List;

import net.caravidro.wayaround.advancement.WayAroundAdvancements;

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

/**
 * Input surface only. WarBallistics owns cooldowns, trajectories, impacts and
 * Infinity interaction so every firearm speaks the same projectile language.
 */
public final class WarGunItem extends Item {

    private static final int USE_DURATION =
            72_000;

    private final Kind kind;

    public WarGunItem(
            Kind kind,
            Properties properties
    ) {
        super(
                properties
        );
        this.kind =
                kind;
    }

    public Kind kind() {
        return kind;
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
            if (WarBallistics.tryFire(
                    serverPlayer,
                    kind
            )) {
                WayAroundAdvancements.warShot(
                        serverPlayer
                );

            }
        }

        if (kind.automatic) {
            player.startUsingItem(
                    hand
            );

            return InteractionResultHolder.consume(
                    stack
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return kind.automatic
                ? USE_DURATION
                : 0;
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
        if (!level.isClientSide
                && kind.automatic
                && living instanceof ServerPlayer player
                && WarBallistics.tryFire(
                player,
                kind
        )) {
            WayAroundAdvancements.warShot(
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
                        kind.tooltip
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.war.infinity"
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );
    }

    public enum Kind {
        GLOCK(
                "tooltip.wayaround.glock",
                false,
                5,
                1,
                14.0F,
                7.5,
                0.0065
        ),
        SHOTGUN(
                "tooltip.wayaround.shotgun",
                false,
                18,
                9,
                5.25F,
                6.0,
                0.085
        ),
        MACHINE_GUN(
                "tooltip.wayaround.machine_gun",
                true,
                2,
                1,
                8.0F,
                8.0,
                0.025
        ),
        ROCKET_LAUNCHER(
                "tooltip.wayaround.rocket_launcher",
                false,
                13,
                1,
                18.0F,
                1.65,
                0.004
        );

        public final String tooltip;
        public final boolean automatic;
        public final int intervalTicks;
        public final int pellets;
        public final float damage;
        public final double speed;
        public final double spread;

        Kind(
                String tooltip,
                boolean automatic,
                int intervalTicks,
                int pellets,
                float damage,
                double speed,
                double spread
        ) {
            this.tooltip =
                    tooltip;
            this.automatic =
                    automatic;
            this.intervalTicks =
                    intervalTicks;
            this.pellets =
                    pellets;
            this.damage =
                    damage;
            this.speed =
                    speed;
            this.spread =
                    spread;
        }

        public boolean rocket() {
            return this
                    == ROCKET_LAUNCHER;
        }
    }
}
