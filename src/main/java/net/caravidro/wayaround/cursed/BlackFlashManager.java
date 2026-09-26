package net.caravidro.wayaround.cursed;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumImpact;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Experimental Black Flash combat rhythm.
 *
 * - Consecutive melee hits on the same target raise a growing chance to enter
 *   the "timing window".
 * - When primed, both hands leak red particles.
 * - More grounded attacks while primed increase stored charge.
 * - The next airborne melee attack consumes the timing window and becomes a
 *   Black Flash. More stored charge means a stronger multiplier.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class BlackFlashManager {

    private BlackFlashManager() {
    }

    private static final int MIN_COMBO_FOR_PRIME = 3;
    private static final int COMBO_TIMEOUT_TICKS = 48;
    private static final int PRIME_TIMEOUT_TICKS = 120;
    private static final int MAX_CHARGE = 5;

    private static final Map<UUID, State> STATES =
            new HashMap<>();

    private static final Map<UUID, Long> ULTIMATE_TUKUNA =
            new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getSource().getEntity()
                instanceof ServerPlayer attacker)
                || event.getAmount() <= 0.0F) {

            return;
        }

        LivingEntity target =
                event.getEntity();

        if (target == attacker) {
            return;
        }

        /*
         * Black Flash is a direct melee rhythm, not a projectile / Blue /
         * explosion side effect from the same player.
         */
        Entity direct =
                event.getSource()
                        .getDirectEntity();

        if (direct != attacker) {
            return;
        }

        long tick =
                attacker.server
                        .getTickCount();

        State state =
                STATES.computeIfAbsent(
                        attacker.getUUID(),
                        ignored -> new State()
                );

        UUID targetId =
                target.getUUID();

        if (!targetId.equals(
                state.target
        )
                || tick - state.lastHitTick
                        > COMBO_TIMEOUT_TICKS) {

            state.resetFor(
                    targetId,
                    tick
            );
        }

        state.lastHitTick =
                tick;

        state.comboHits++;

        if (state.primed) {
            if (isAirAttack(
                    attacker
            )) {

                trigger(
                        attacker,
                        target,
                        event,
                        state
                );

                STATES.remove(
                        attacker.getUUID()
                );

                return;
            }

            state.charge =
                    Math.min(
                            MAX_CHARGE,
                            state.charge + 1
                    );

            state.primedTick =
                    tick;

            emitHands(
                    attacker,
                    state.charge,
                    true
            );

            emitChargeEscalation(
                    attacker,
                    state.charge
            );

            attacker.displayClientMessage(
                    Component.translatable(
                                    "message.wayaround.black_flash.charge",
                                    state.charge,
                                    MAX_CHARGE
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            ),
                    true
            );

            return;
        }

        if (state.comboHits
                < MIN_COMBO_FOR_PRIME) {

            return;
        }

        float chance =
                Mth.clamp(
                        0.08F
                                + (
                                state.comboHits
                                        - MIN_COMBO_FOR_PRIME
                        )
                                * 0.075F,
                        0.08F,
                        0.64F
                );

        if (attacker.getRandom()
                .nextFloat()
                >= chance) {

            return;
        }

        state.primed =
                true;

        state.charge =
                0;

        state.primedTick =
                tick;

        emitHands(
                attacker,
                0,
                true
        );

        attacker.displayClientMessage(
                Component.translatable(
                                "message.wayaround.black_flash.primed"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD
                        ),
                true
        );
    }

    @SubscribeEvent
    public static void onTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        var iterator =
                STATES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry =
                    iterator.next();

            State state =
                    entry.getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null
                    || !player.isAlive()) {

                iterator.remove();
                continue;
            }

            if (!state.primed) {
                if (tick - state.lastHitTick
                        > COMBO_TIMEOUT_TICKS) {

                    iterator.remove();
                }

                continue;
            }

            if (tick - state.primedTick
                    > PRIME_TIMEOUT_TICKS) {

                iterator.remove();
                continue;
            }

            if (tick % 2L == 0L) {
                emitHands(
                        player,
                        state.charge,
                        false
                );
            }
        }

        ULTIMATE_TUKUNA.entrySet()
                .removeIf(
                        entry -> {
                            if (tick
                                    < entry.getValue()) {
                                return false;
                            }

                            ServerPlayer target =
                                    server.getPlayerList()
                                            .getPlayer(
                                                    entry.getKey()
                                            );

                            if (target != null
                                    && target.isAlive()
                                    && SpectrumAccess.has(
                                    target,
                                    SpectrumType.TUKUNA
                            )) {
                                float nearDeath =
                                        Math.max(
                                                1.0F,
                                                target.getMaxHealth()
                                                        * 0.05F
                                        );

                                target.setHealth(
                                        Math.min(
                                                target.getHealth(),
                                                nearDeath
                                        )
                                );
                            }

                            return true;
                        }
                );
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        STATES.clear();
        ULTIMATE_TUKUNA.clear();
    }

    private static void trigger(
            ServerPlayer attacker,
            LivingEntity target,
            LivingIncomingDamageEvent event,
            State state
    ) {
        float multiplier =
                1.90F
                        + state.charge
                                * 0.24F;

        event.setAmount(
                event.getAmount()
                        * multiplier
        );

        ServerLevel level =
                attacker.serverLevel();

        Vec3 center =
                target.position()
                        .add(
                                0.0,
                                target.getBbHeight() * 0.58,
                                0.0
                        );

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                center.x,
                center.y,
                center.z,
                58 + state.charge * 9,
                0.72,
                0.56,
                0.72,
                0.28
        );

        level.sendParticles(
                ParticleTypes.CRIT,
                center.x,
                center.y,
                center.z,
                34 + state.charge * 4,
                0.84,
                0.64,
                0.84,
                0.34
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                20 + state.charge * 3,
                0.66,
                0.50,
                0.66,
                0.28
        );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                1.8F,
                0.58F
                        + state.charge
                                * 0.035F
        );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                0.55F,
                0.46F
        );

        attacker.displayClientMessage(
                Component.translatable(
                                "message.wayaround.black_flash.trigger",
                                String.format(
                                        java.util.Locale.ROOT,
                                        "%.2f",
                                        multiplier
                                )
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD
                        ),
                true
        );

        applyChargeImpact(
                attacker,
                target,
                state.charge
        );

        if (target instanceof ServerPlayer bearer
                && state.charge >= 5
                && SpectrumAccess.has(
                bearer,
                SpectrumType.TUKUNA
        )) {
            ULTIMATE_TUKUNA.put(
                    bearer.getUUID(),
                    attacker.server
                            .getTickCount()
                            + 1L
            );
        }

        if (target instanceof ServerPlayer bearer
                && ImmortalWheelManager.hasWheel(
                        bearer
                )) {

            if (state.charge >= 5) {
                ImmortalWheelManager.breakByUltimateBlackFlash(
                        bearer,
                        attacker,
                        state.charge
                );
            } else {
                ImmortalWheelManager.damageByBlackFlash(
                        bearer,
                        attacker,
                        state.charge
                );
            }
        }
    }

    private static void emitChargeEscalation(
            ServerPlayer player,
            int charge
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 center =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight() * 0.62,
                                0.0
                        );

        int count =
                6
                        + charge
                                * charge
                                * 4;

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                center.x,
                center.y,
                center.z,
                count,
                0.18 + charge * 0.055,
                0.24 + charge * 0.045,
                0.18 + charge * 0.055,
                0.03 + charge * 0.012
        );

        if (charge >= 2) {
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    center.x,
                    center.y,
                    center.z,
                    4 + charge * 5,
                    0.24 + charge * 0.07,
                    0.28 + charge * 0.05,
                    0.24 + charge * 0.07,
                    0.08
            );
        }

        if (charge >= 4) {
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    center.x,
                    center.y,
                    center.z,
                    22 + charge * 8,
                    0.54,
                    0.72,
                    0.54,
                    0.12
            );
        }

        level.playSound(
                null,
                player.blockPosition(),
                charge >= 4
                        ? SoundEvents.WARDEN_SONIC_BOOM
                        : SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                0.38F + charge * 0.17F,
                Math.max(
                        0.46F,
                        1.30F - charge * 0.14F
                )
        );
    }

    private static void applyChargeImpact(
            ServerPlayer attacker,
            LivingEntity target,
            int charge
    ) {
        if (charge < 3) {
            return;
        }

        Vec3 direction =
                target.position()
                        .subtract(
                                attacker.position()
                        );

        direction =
                new Vec3(
                        direction.x,
                        0.0,
                        direction.z
                );

        if (direction.lengthSqr()
                < 1.0E-6) {
            Vec3 look =
                    attacker.getLookAngle();

            direction =
                    new Vec3(
                            look.x,
                            0.0,
                            look.z
                    );
        }

        direction =
                direction.lengthSqr()
                        < 1.0E-6
                        ? new Vec3(
                        0.0,
                        0.0,
                        1.0
                )
                        : direction.normalize();

        if (charge == 3) {
            SpectrumImpact.launchAndBreak(
                    attacker,
                    target,
                    direction.scale(
                            2.85
                    ).add(
                            0.0,
                            0.42,
                            0.0
                    ),
                    13.0,
                    1
            );

            return;
        }

        if (charge == 4) {
            cinematic(
                    attacker,
                    PlayerCinematicPayload.BLACK_FLASH_HEAVY,
                    22,
                    0.62F
            );

            ServerLevel level =
                    attacker.serverLevel();

            Vec3 center =
                    target.position()
                            .add(
                                    0.0,
                                    target.getBbHeight() * 0.55,
                                    0.0
                            );

            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    center.x,
                    center.y,
                    center.z,
                    8,
                    0.82,
                    0.72,
                    0.82,
                    0.02
            );

            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    center.x,
                    center.y,
                    center.z,
                    86,
                    1.15,
                    0.85,
                    1.15,
                    0.28
            );

            SpectrumImpact.launchAndBreak(
                    attacker,
                    target,
                    direction.scale(
                            4.25
                    ).add(
                            0.0,
                            0.72,
                            0.0
                    ),
                    20.0,
                    1
            );

            return;
        }

        cinematic(
                attacker,
                PlayerCinematicPayload.BLACK_FLASH_ULTIMATE,
                30,
                1.05F
        );

        ServerLevel level =
                attacker.serverLevel();

        Vec3 center =
                target.position()
                        .add(
                                0.0,
                                target.getBbHeight() * 0.55,
                                0.0
                        );

        level.sendParticles(
                ParticleTypes.EXPLOSION,
                center.x,
                center.y,
                center.z,
                18,
                1.45,
                1.0,
                1.45,
                0.05
        );

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                center.x,
                center.y,
                center.z,
                190,
                1.65,
                1.15,
                1.65,
                0.55
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                120,
                1.35,
                0.92,
                1.35,
                0.65
        );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                2.4F,
                0.46F
        );

        SpectrumImpact.launchAndBreak(
                attacker,
                target,
                direction.scale(
                        7.25
                ).add(
                        0.0,
                        1.05,
                        0.0
                ),
                34.0,
                1
        );
    }

    private static void cinematic(
            ServerPlayer player,
            byte animation,
            int duration,
            float shake
    ) {
        PlayerCinematicPayload payload =
                new PlayerCinematicPayload(
                        player.getUUID(),
                        animation,
                        duration,
                        true,
                        shake
                );

        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                160.0,
                payload
        );

        PacketDistributor.sendToPlayer(
                player,
                payload
        );
    }

    private static boolean isAirAttack(
            ServerPlayer attacker
    ) {
        return !attacker.onGround()
                && !attacker.isPassenger()
                && !attacker.isInWaterOrBubble();
    }

    private static void emitHands(
            ServerPlayer player,
            int charge,
            boolean burst
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 look =
                player.getLookAngle();

        Vec3 right =
                new Vec3(
                        -look.z,
                        0.0,
                        look.x
                );

        if (right.lengthSqr()
                < 0.0001) {

            right =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            right =
                    right.normalize();
        }

        Vec3 forward =
                new Vec3(
                        look.x,
                        0.0,
                        look.z
                );

        if (forward.lengthSqr()
                < 0.0001) {

            forward =
                    new Vec3(
                            0.0,
                            0.0,
                            1.0
                    );
        } else {
            forward =
                    forward.normalize();
        }

        Vec3 center =
                player.position()
                        .add(
                                0.0,
                                player.getBbHeight() * 0.68,
                                0.0
                        )
                        .add(
                                forward.scale(
                                        0.28
                                )
                        );

        Vec3 leftHand =
                center.add(
                        right.scale(
                                -0.27
                        )
                );

        Vec3 rightHand =
                center.add(
                        right.scale(
                                0.27
                        )
                );

        int count =
                (burst ? 8 : 2)
                        + charge;

        double spread =
                0.035
                        + charge
                                * 0.006;

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                leftHand.x,
                leftHand.y,
                leftHand.z,
                count,
                spread,
                spread,
                spread,
                0.015 + charge * 0.004
        );

        level.sendParticles(
                DustParticleOptions.REDSTONE,
                rightHand.x,
                rightHand.y,
                rightHand.z,
                count,
                spread,
                spread,
                spread,
                0.015 + charge * 0.004
        );
    }

    private static final class State {
        private UUID target;
        private long lastHitTick;
        private long primedTick;
        private int comboHits;
        private int charge;
        private boolean primed;

        private void resetFor(
                UUID target,
                long tick
        ) {
            this.target =
                    target;
            this.lastHitTick =
                    tick;
            this.primedTick =
                    tick;
            this.comboHits =
                    0;
            this.charge =
                    0;
            this.primed =
                    false;
        }
    }
}
