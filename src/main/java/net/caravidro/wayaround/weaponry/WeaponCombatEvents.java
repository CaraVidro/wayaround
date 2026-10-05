package net.caravidro.wayaround.weaponry;

import java.util.Comparator;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class WeaponCombatEvents {

    private WeaponCombatEvents() {
    }

    @SubscribeEvent
    public static void attack(
            AttackEntityEvent event
    ) {
        Player player =
                event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack main =
                player.getMainHandItem();

        if (!(main.getItem()
                instanceof WayWeaponItem weapon)) {
            return;
        }

        int variant =
                Math.floorMod(
                        player.tickCount
                                + event.getTarget()
                                        .getId(),
                        3
                );

        switch (weapon.family()) {
            case DAGGER ->
                    dagger(
                            serverPlayer,
                            event,
                            variant
                    );

            case KATANA ->
                    katanaSound(
                            serverPlayer,
                            variant
                    );

            case SCYTHE ->
                    scythe(
                            serverPlayer,
                            event,
                            variant
                    );
        }
    }

    private static void dagger(
            ServerPlayer player,
            AttackEntityEvent event,
            int variant
    ) {
        float pitch =
                1.25F
                        + variant
                                * 0.10F;

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_WEAK,
                        SoundSource.PLAYERS,
                        0.68F,
                        pitch
                );

        ItemStack offhand =
                player.getOffhandItem();

        if (!(offhand.getItem()
                instanceof WayWeaponItem second)
                || second.family()
                        != WeaponFamily.DAGGER) {
            return;
        }

        /*
         * The offhand item's attribute component supplies the real attack
         * speed bonus. Keep damage vanilla-owned so invulnerability frames,
         * enchantments and crit math are never bypassed or pre-empted.
         */
        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS,
                        0.42F,
                        1.45F
                                + variant
                                        * 0.08F
                );
    }

    private static void katanaSound(
            ServerPlayer player,
            int variant
    ) {
        float pitch =
                switch (variant) {
                    case 1 -> 1.22F;
                    case 2 -> 0.92F;
                    default -> 1.06F;
                };

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS,
                        0.92F,
                        pitch
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.TRIDENT_THROW,
                        SoundSource.PLAYERS,
                        0.26F,
                        1.55F
                                + variant
                                        * 0.08F
                );
    }

    private static void scythe(
            ServerPlayer player,
            AttackEntityEvent event,
            int variant
    ) {
        WeaponFamily family =
                WeaponFamily.SCYTHE;

        double radius =
                family.sweepRadius();

        Vec3 origin =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight()
                                        * 0.45,
                                0.0
                        );

        Vec3 facing =
                player.getLookAngle()
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (facing.lengthSqr()
                > 1.0E-8) {
            facing =
                    facing.normalize();
        }

        final Vec3 sweepFacing =
                facing;

        final LivingEntity mainTarget =
                event.getTarget()
                        instanceof LivingEntity living
                        ? living
                        : null;

        AABB area =
                player.getBoundingBox()
                        .inflate(
                                radius,
                                1.35,
                                radius
                        );

        var targets =
                player.serverLevel()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                area,
                                living ->
                                        living != player
                                                && living != mainTarget
                                                && living.isAlive()
                                                && player.hasLineOfSight(
                                                living
                                        )
                                                && !living.isAlliedTo(
                                                player
                                        )
                        );

        targets.stream()
                .sorted(
                        Comparator.comparingDouble(
                                player::distanceToSqr
                        )
                )
                .limit(
                        6
                )
                .forEach(
                        living -> {
                            Vec3 to =
                                    living.position()
                                            .subtract(
                                                    origin
                                            )
                                            .multiply(
                                                    1.0,
                                                    0.0,
                                                    1.0
                                            );

                            if (to.lengthSqr()
                                    <= 1.0E-8) {
                                return;
                            }

                            /*
                             * Wide ~220 degree harvesting arc. The back cone
                             * remains safe so the scythe is not a full sphere.
                             */
                            double dot =
                                    sweepFacing.dot(
                                            to.normalize()
                                    );

                            if (dot < -0.34) {
                                return;
                            }

                            float damage =
                                    (float) player.getAttributeValue(
                                            Attributes.ATTACK_DAMAGE
                                    )
                                            * (float) family.sweepDamageFactor();

                            living.hurt(
                                    player.damageSources()
                                            .playerAttack(
                                                    player
                                            ),
                                    Math.max(
                                            1.0F,
                                            damage
                                    )
                            );

                            Vec3 push =
                                    to.normalize()
                                            .scale(
                                                    0.22
                                            );

                            living.push(
                                    push.x,
                                    0.08,
                                    push.z
                            );
                        }
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_STRONG,
                        SoundSource.PLAYERS,
                        1.05F,
                        0.72F
                                + variant
                                        * 0.07F
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS,
                        0.72F,
                        0.62F
                                + variant
                                        * 0.06F
                );
    }
}
