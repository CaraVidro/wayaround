package net.caravidro.wayaround.infinity;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.InfinityVisualPayload;
import net.caravidro.wayaround.war.WarProjectileEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    /*
     * Vanilla/mod Projectile instances that have had their momentum drained.
     * Keeping this after they LEAVE Infinity is intentional: they do not get
     * their old velocity back; they continue losing horizontal momentum and
     * fall under gravity.
     */
    private static final Map<UUID, DrainedProjectile>
            DRAINED_PROJECTILES =
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

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        Set<UUID> influencedProjectiles =
                new HashSet<>();

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
                    influencedProjectiles
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

        tickDrainedProjectiles(
                server,
                influencedProjectiles
        );
    }

    private static void applyField(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center,
            float radius,
            float confidence,
            Set<UUID> influencedProjectiles
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

        Vec3 ownerMotion =
                owner.getDeltaMovement();

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

            if (entity instanceof WarProjectileEntity warProjectile
                    && warProjectile.ownedBy(
                            owner.getUUID()
                    )) {
                continue;
            }

            boolean projectileLike =
                    entity instanceof Projectile
                            || entity instanceof WarProjectileEntity;

            Vec3 relative =
                    entity.position()
                            .subtract(
                                    center
                            );

            double distance =
                    relative.length();

            if (distance < 0.08
                    || distance >= radius) {

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
                                    / radius;

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

            if (projectileLike
                    && influence > 0.06) {
                influencedProjectiles.add(
                        entity.getUUID()
                );

                if (entity instanceof WarProjectileEntity warProjectile) {
                    warProjectile.markInfinityAffected();
                } else if (entity instanceof Projectile projectile) {
                    projectile.setNoGravity(
                            false
                    );

                    DRAINED_PROJECTILES.put(
                            entity.getUUID(),
                            new DrainedProjectile(
                                    level.dimension()
                            )
                    );
                }
            }

            Vec3 velocity =
                    entity.getDeltaMovement();

            double approaching =
                    -velocity.dot(
                            outward
                    );

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

            /*
             * Walking toward an incoming object effectively makes the
             * remaining distance disappear faster. At high confidence this
             * becomes a visible recoil: arrows start travelling backwards.
             */
            double ownerClosing =
                    ownerMotion.dot(
                            outward
                    );

            if (entity instanceof Projectile
                    && ownerClosing > 0.0) {

                next =
                        next.add(
                                outward.scale(
                                        ownerClosing
                                                * influence
                                                * (
                                                0.85
                                                        + proximity
                                                                * 2.40
                                        )
                                )
                        );
            }

            if (entity instanceof Projectile
                    && approaching > 0.0
                    && influence > 0.74) {

                next =
                        next.add(
                                outward.scale(
                                        approaching
                                                * (
                                                influence
                                                        - 0.70
                                        )
                                                * 1.55
                                )
                        );
            }

            if (influence > 0.965) {
                /*
                 * Full Infinity does not glue projectiles to one absolute
                 * coordinate. If the owner walks toward a stopped projectile,
                 * the field pushes it outward so from the owner's perspective
                 * the round visibly retreats instead of clipping through.
                 */
                if (projectileLike
                        && ownerClosing > 0.0) {
                    next =
                            outward.scale(
                                    ownerClosing
                                            * (
                                            1.05
                                                    + proximity
                                                            * 1.85
                                    )
                            );
                } else {
                    next =
                            Vec3.ZERO;
                }

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

    private static void tickDrainedProjectiles(
            MinecraftServer server,
            Set<UUID> influencedProjectiles
    ) {
        Iterator<Map.Entry<UUID, DrainedProjectile>>
                iterator =
                DRAINED_PROJECTILES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DrainedProjectile> entry =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            entry.getValue()
                                    .dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            Entity entity =
                    level.getEntity(
                            entry.getKey()
                    );

            if (!(entity instanceof Projectile projectile)
                    || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            projectile.setNoGravity(
                    false
            );

            if (influencedProjectiles.contains(
                    entity.getUUID()
            )) {
                continue;
            }

            /*
             * The projectile is no longer inside any Infinity field, but the
             * lost momentum stays lost. This also defeats self-propelled
             * projectiles that try to rebuild forward speed after release.
             */
            Vec3 velocity =
                    entity.getDeltaMovement();

            Vec3 drained =
                    new Vec3(
                            velocity.x * 0.52,
                            Math.min(
                                    velocity.y * 0.82,
                                    -0.055
                            ),
                            velocity.z * 0.52
                    );

            entity.setDeltaMovement(
                    drained
            );
        }
    }

    /**
     * Continuous segment-vs-Infinity test for very fast physical projectiles.
     *
     * The normal field already damps Projectile entities each tick. Bullets in
     * WarBallistics can cross several blocks per tick, so they also ray-test
     * the 3D "hard core" where the existing influence formula reaches the same
     * 0.965 full-stop threshold used by applyField().
     */
    public static Vec3 clipHardProjectileBarrier(
            ServerLevel level,
            UUID projectileOwner,
            Vec3 from,
            Vec3 to
    ) {
        Vec3 nearest =
                null;

        double nearestDistance =
                Double.POSITIVE_INFINITY;

        for (InfinityState state :
                ACTIVE.values()) {
            if (state.confidence
                    < MIN_ACTIVE_CONFIDENCE
                    || state.owner.equals(
                            projectileOwner
                    )
                    || !state.dimension.equals(
                            level.dimension()
                    )) {
                continue;
            }

            ServerPlayer owner =
                    level.getServer()
                            .getPlayerList()
                            .getPlayer(
                                    state.owner
                            );

            if (owner == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || !hasSpectrum(
                            owner
                    )) {
                continue;
            }

            double requiredProximitySquared =
                    (
                            0.965
                                    / state.confidence
                                    - 0.22
                    )
                            / 1.12;

            if (requiredProximitySquared
                    >= 1.0) {
                continue;
            }

            double proximity =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    requiredProximitySquared
                            )
                    );

            double hardRadius =
                    radiusFor(
                            state.confidence
                    )
                            * (
                            1.0
                                    - proximity
                    );

            if (hardRadius
                    <= 0.05) {
                continue;
            }

            Vec3 hit =
                    segmentSphereEntry(
                            from,
                            to,
                            owner.getEyePosition(),
                            hardRadius
                    );

            if (hit == null) {
                continue;
            }

            double distance =
                    from.distanceToSqr(
                            hit
                    );

            if (distance
                    < nearestDistance) {
                nearestDistance =
                        distance;
                nearest =
                        hit;
            }
        }

        return nearest;
    }

    private static Vec3 segmentSphereEntry(
            Vec3 from,
            Vec3 to,
            Vec3 center,
            double radius
    ) {
        Vec3 delta =
                to.subtract(
                        from
                );

        Vec3 offset =
                from.subtract(
                        center
                );

        double a =
                delta.dot(
                        delta
                );

        if (a
                < 1.0E-9) {
            return null;
        }

        double c =
                offset.dot(
                        offset
                )
                        - radius
                                * radius;

        if (c <= 0.0) {
            return from;
        }

        double b =
                2.0
                        * offset.dot(
                                delta
                        );

        double discriminant =
                b * b
                        - 4.0
                                * a
                                * c;

        if (discriminant
                < 0.0) {
            return null;
        }

        double sqrt =
                Math.sqrt(
                        discriminant
                );

        double t =
                (
                        -b
                                - sqrt
                )
                        / (
                        2.0
                                * a
                );

        if (t < 0.0
                || t > 1.0) {
            return null;
        }

        return from.add(
                delta.scale(
                        t
                )
        );
    }

    private static float radiusFor(
            float confidence
    ) {
        return 2.75F
                + confidence
                        * 7.75F;
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
        DRAINED_PROJECTILES.clear();
    }

    private static final class DrainedProjectile {

        private final ResourceKey<Level> dimension;

        private DrainedProjectile(
                ResourceKey<Level> dimension
        ) {
            this.dimension =
                    dimension;
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
