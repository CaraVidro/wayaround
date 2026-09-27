package net.caravidro.wayaround.infinity;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.cursed.ImmortalWheelManager;
import net.caravidro.wayaround.network.InfinityVisualPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Experimental "Infinity" field.
 *
 * Natural speech accumulates confidence progressively. Explicit speech such
 * as "infinidade ativar" is the deliberate override and sets the field to
 * maximum immediately. Confidence drives radius, distortion and how
 * aggressively motion converges toward zero.
 */
public final class InfinityManager {

    private InfinityManager() {
    }

    private static final Map<UUID, InfinityState> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, FrozenOrientation>
            FROZEN_ORIENTATION =
            new HashMap<>();

    private static final Map<UUID, ProjectileBrake>
            PROJECTILE_BRAKES =
            new HashMap<>();

    private static final double VISUAL_RANGE =
            128.0;

    private static final float MIN_ACTIVE_CONFIDENCE =
            0.08F;

    public static boolean activateMax(
            ServerPlayer player
    ) {
        if (!hasSpectrum(
                player
        )) {
            return false;
        }

        InfinityState state =
                ACTIVE.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new InfinityState(
                                        player.getUUID(),
                                        player.serverLevel()
                                                .dimension()
                                )
                );

        state.dimension =
                player.serverLevel()
                        .dimension();

        state.confidence =
                1.0F;

        state.lastReinforcedTick =
                player.server
                        .getTickCount();

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.82F,
                        1.34F
                );

        WayAround.LOGGER.info(
                "[Infinity] owner={} ATIVADA NO MAXIMO",
                player.getGameProfile()
                        .getName()
        );

        return true;
    }

    public static boolean reinforce(
            ServerPlayer player,
            float evidence,
            float urgency
    ) {
        if (!hasSpectrum(
                player
        )) {
            return false;
        }

        float cleanEvidence =
                Mth.clamp(
                        evidence,
                        0.0F,
                        1.0F
                );

        float cleanUrgency =
                Mth.clamp(
                        urgency,
                        0.0F,
                        1.0F
                );

        if (cleanEvidence
                <= 0.001F) {

            return false;
        }

        InfinityState state =
                ACTIVE.computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new InfinityState(
                                        player.getUUID(),
                                        player.serverLevel()
                                                .dimension()
                                )
                );

        state.dimension =
                player.serverLevel()
                        .dimension();

        state.confidence =
                Mth.clamp(
                        state.confidence
                                + cleanEvidence
                                + cleanUrgency * 0.035F,
                        0.0F,
                        1.0F
                );

        state.lastReinforcedTick =
                player.server
                        .getTickCount();

        WayAround.LOGGER.info(
                "[Infinity] owner={} +evidence={} urgency={} -> confidence={}",
                player.getGameProfile()
                        .getName(),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        cleanEvidence
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        cleanUrgency
                ),
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        state.confidence
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS,
                        0.22F
                                + state.confidence
                                        * 0.34F,
                        1.82F
                                - state.confidence
                                        * 0.28F
                );

        return true;
    }

    public static boolean deactivate(
            ServerPlayer player
    ) {
        InfinityState removed =
                ACTIVE.remove(
                        player.getUUID()
                );

        if (removed == null) {
            return false;
        }

        sendVisual(
                player.serverLevel(),
                removed,
                player.position(),
                0.0F,
                0.0F
        );

        WayAround.LOGGER.info(
                "[Infinity] owner={} desativada",
                player.getGameProfile()
                        .getName()
        );

        return true;
    }

    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer defender)
                || !isInfinityActive(
                        defender
                )) {
            return;
        }

        DamageSource source =
                event.getSource();

        Entity attackerEntity =
                source.getEntity();

        Entity directEntity =
                source.getDirectEntity();

        /*
         * Infinity is a barrier against external attacks. Environmental/self
         * damage is intentionally left alone; entity-caused melee, projectiles,
         * explosions and similar attacks are denied here.
         */
        if (attackerEntity == null
                && directEntity == null) {
            return;
        }

        if (attackerEntity
                instanceof ServerPlayer attacker
                && attacker != defender
                && isTrueMelee(
                        source,
                        attacker,
                        defender
                )
                && ImmortalWheelManager.hasWheel(
                        attacker
                )) {

            float penetration =
                    ImmortalWheelManager
                            .adaptToInfinityMelee(
                                    attacker
                            );

            event.setAmount(
                    Math.max(
                            0.0F,
                            event.getAmount()
                                    * penetration
                    )
            );

            return;
        }

        event.setAmount(
                0.0F
        );
    }

    public static void onKnockBack(
            LivingKnockBackEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player
                && isInfinityActive(
                        player
                )) {

            /*
             * Damage and motion are separate vanilla systems. Blocking only
             * damage still lets explosions/punches shove the owner around.
             */
            event.setCanceled(
                    true
            );
        }
    }

    public static boolean isInfinityActive(
            ServerPlayer player
    ) {
        InfinityState state =
                ACTIVE.get(
                        player.getUUID()
                );

        return state != null
                && state.confidence
                        >= MIN_ACTIVE_CONFIDENCE
                && hasSpectrum(
                        player
                );
    }

    private static boolean isTrueMelee(
            DamageSource source,
            ServerPlayer attacker,
            ServerPlayer defender
    ) {
        if (source.is(
                DamageTypeTags.IS_PROJECTILE
        )) {
            return false;
        }

        Entity direct =
                source.getDirectEntity();

        if (direct != attacker
                || source.getEntity()
                        != attacker) {
            return false;
        }

        return attacker.distanceToSqr(
                defender
        ) <= 20.25;
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        PROJECTILE_BRAKES.values()
                .removeIf(
                        brake ->
                                tick
                                        - brake.lastSeenTick
                                        > 40L
                );

        Iterator<Map.Entry<UUID, InfinityState>>
                iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            InfinityState state =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    state.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            state.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || !hasSpectrum(
                            owner
                    )) {

                iterator.remove();
                continue;
            }

            if (state.confidence
                    < MIN_ACTIVE_CONFIDENCE) {

                continue;
            }

            Vec3 center =
                    owner.getEyePosition();

            float radius =
                    radiusFor(
                            state.confidence
                    );

            applyField(
                    level,
                    owner,
                    center,
                    radius,
                    state.confidence,
                    tick
            );

            if ((server.getTickCount()
                    & 1) == 0) {

                sendVisual(
                        level,
                        state,
                        center,
                        state.confidence,
                        radius
                );
            }
        }
    }

    private static void applyField(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center,
            float radius,
            float confidence,
            long tick
    ) {
        AABB area =
                new AABB(
                        center.x - radius,
                        center.y - radius,
                        center.z - radius,
                        center.x + radius,
                        center.y + radius,
                        center.z + radius
                );

        for (Entity entity :
                level.getEntities(
                        owner,
                        area,
                        candidate ->
                                candidate.isAlive()
                                        && candidate != owner
                )) {

            if (entity instanceof Projectile projectile
                    && projectile.getOwner()
                    == owner) {

                continue;
            }

            if (entity instanceof Projectile projectile) {
                slowProjectile(
                        level,
                        projectile,
                        center,
                        radius,
                        confidence,
                        tick
                );
                continue;
            }

            Vec3 relative =
                    entity.position()
                            .subtract(
                                    center
                            );

            double distance =
                    relative.length();

            double interactionRadius =
                    entity instanceof LivingEntity
                            ? Math.min(
                                    radius,
                                    2.75
                            )
                            : radius;

            if (distance < 0.08
                    || distance >= interactionRadius) {

                continue;
            }

            Vec3 outward =
                    relative.scale(
                            1.0
                                    / distance
                    );

            double proximity =
                    1.0
                            - distance
                                    / interactionRadius;

            double influence =
                    Mth.clamp(
                            confidence
                                    * (
                                    0.22
                                            + proximity
                                                    * proximity
                                                    * 1.12
                            ),
                            0.0,
                            1.0
                    );

            Vec3 velocity =
                    entity.getDeltaMovement();

            double damping =
                    Math.max(
                            0.0,
                            1.0
                                    - influence
                                            * influence
                                            * 0.985
                    );

            Vec3 next =
                    velocity.scale(
                            damping
                    );

            if (influence > 0.965) {
                next =
                        Vec3.ZERO;

                FrozenOrientation orientation =
                        FROZEN_ORIENTATION.computeIfAbsent(
                                entity.getUUID(),
                                ignored ->
                                        new FrozenOrientation(
                                                entity.getYRot(),
                                                entity.getXRot(),
                                                entity instanceof LivingEntity living
                                                        ? living.getYHeadRot()
                                                        : entity.getYRot(),
                                                entity instanceof LivingEntity living
                                                        ? living.yBodyRot
                                                        : entity.getYRot()
                                        )
                        );

                entity.setYRot(
                        orientation.yaw
                );

                entity.setXRot(
                        orientation.pitch
                );

                entity.yRotO =
                        orientation.yaw;

                entity.xRotO =
                        orientation.pitch;

                if (entity instanceof LivingEntity living) {
                    living.setYHeadRot(
                            orientation.headYaw
                    );

                    living.yHeadRotO =
                            orientation.headYaw;

                    living.yBodyRot =
                            orientation.bodyYaw;

                    living.yBodyRotO =
                            orientation.bodyYaw;
                }

            } else {
                FROZEN_ORIENTATION.remove(
                        entity.getUUID()
                );
            }

            entity.setDeltaMovement(
                    next
            );

            entity.fallDistance =
                    0.0F;
        }
    }

    private static void slowProjectile(
            ServerLevel level,
            Projectile projectile,
            Vec3 center,
            float radius,
            float confidence,
            long tick
    ) {
        Vec3 offset =
                projectile.position()
                        .subtract(
                                center
                        );

        double distance =
                offset.length();

        if (distance >= radius) {
            return;
        }

        Vec3 velocity =
                projectile.getDeltaMovement();

        double speed =
                velocity.length();

        ProjectileBrake brake =
                PROJECTILE_BRAKES.computeIfAbsent(
                        projectile.getUUID(),
                        ignored ->
                                new ProjectileBrake(
                                        Math.max(
                                                0.001,
                                                speed
                                        ),
                                        speed > 0.001
                                                ? velocity.normalize()
                                                : new Vec3(
                                                        0.0,
                                                        0.0,
                                                        1.0
                                                ),
                                        tick
                                )
                );

        brake.lastSeenTick =
                tick;

        /*
         * Faster projectiles get fewer braking ticks. A normal arrow eases
         * down visibly; a bullet sheds most speed in a handful of ticks, but
         * never teleports straight from full speed to zero.
         */
        double stopTicks =
                Mth.clamp(
                        24.0
                                - brake.entrySpeed
                                        * 2.25,
                        5.0,
                        18.0
                );

        double age =
                tick
                        - brake.enteredTick
                        + 1.0;

        double confidenceScale =
                0.60
                        + confidence
                                * 0.40;

        double progress =
                Mth.clamp(
                        age
                                / stopTicks
                                * confidenceScale,
                        0.0,
                        1.0
                );

        double factor =
                Math.pow(
                        1.0 - progress,
                        1.65
                );

        double targetSpeed =
                brake.entrySpeed
                        * factor;

        double nextSpeed =
                Math.min(
                        speed,
                        targetSpeed
                );

        Vec3 direction =
                speed > 0.001
                        ? velocity.normalize()
                        : brake.entryDirection;

        double hardStopDistance =
                Math.max(
                        1.75,
                        Math.min(
                                2.65,
                                radius * 0.20
                        )
                );

        if (distance
                <= hardStopDistance
                || progress >= 0.995) {

            nextSpeed =
                    0.0;
        }

        projectile.setDeltaMovement(
                direction.scale(
                        nextSpeed
                )
        );

        projectile.fallDistance =
                0.0F;

        /*
         * Fast rounds get a sparse neutral trail while braking. This is debug
         * feedback for bullets and also makes their deceleration readable.
         */
        if (brake.entrySpeed >= 5.0
                && (tick & 1L) == 0L) {

            level.sendParticles(
                    ParticleTypes.CRIT,
                    projectile.getX(),
                    projectile.getY(),
                    projectile.getZ(),
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.0
            );
        }
    }

    private static float radiusFor(
            float confidence
    ) {
        return 3.25F
                + confidence
                        * 9.50F;
    }

    private static void sendVisual(
            ServerLevel level,
            InfinityState state,
            Vec3 center,
            float confidence,
            float radius
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                VISUAL_RANGE,
                new InfinityVisualPayload(
                        state.owner,
                        center.x,
                        center.y,
                        center.z,
                        confidence,
                        radius
                )
        );
    }

    private static boolean hasSpectrum(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(player, SpectrumType.VOID);
    }

    public static void clearAll() {
        ACTIVE.clear();
        FROZEN_ORIENTATION.clear();
        PROJECTILE_BRAKES.clear();
    }

    private static final class ProjectileBrake {
        private final double entrySpeed;
        private final Vec3 entryDirection;
        private final long enteredTick;
        private long lastSeenTick;

        private ProjectileBrake(
                double entrySpeed,
                Vec3 entryDirection,
                long enteredTick
        ) {
            this.entrySpeed =
                    entrySpeed;

            this.entryDirection =
                    entryDirection;

            this.enteredTick =
                    enteredTick;

            this.lastSeenTick =
                    enteredTick;
        }
    }

    private static final class FrozenOrientation {

        private final float yaw;
        private final float pitch;
        private final float headYaw;
        private final float bodyYaw;

        private FrozenOrientation(
                float yaw,
                float pitch,
                float headYaw,
                float bodyYaw
        ) {
            this.yaw =
                    yaw;

            this.pitch =
                    pitch;

            this.headYaw =
                    headYaw;

            this.bodyYaw =
                    bodyYaw;
        }
    }

    private static final class InfinityState {

        private final UUID owner;
        private ResourceKey<Level> dimension;
        private float confidence;
        private long lastReinforcedTick;

        private InfinityState(
                UUID owner,
                ResourceKey<Level> dimension
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;
        }
    }
}
