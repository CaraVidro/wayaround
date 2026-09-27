package net.caravidro.wayaround.infinity;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    /*
     * Hysteresis: entering the outer field only slows a projectile. Crossing
     * STOP_RADIUS latches it here. Only a latched projectile can later be
     * released, and RELEASE_RADIUS is deliberately much farther out.
     */
    private static final Map<UUID, HeldProjectile>
            HELD_PROJECTILES =
            new HashMap<>();

    private static final double VISUAL_RANGE =
            128.0;

    private static final float MIN_ACTIVE_CONFIDENCE =
            0.08F;

    public static boolean protects(Entity entity) {
        if (entity.level().isClientSide) return entity.isAlive()
                && entity.getPersistentData().getBoolean("WayAroundInfinityActive");
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return false;
        InfinityState state = ACTIVE.get(player.getUUID());
        return state != null && state.confidence >= MIN_ACTIVE_CONFIDENCE
                && state.dimension.equals(player.level().dimension()) && hasSpectrum(player);
    }

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

        playActivationSound(
                player
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

        boolean wasActive =
                state.confidence
                        >= MIN_ACTIVE_CONFIDENCE;

        state.confidence =
                Mth.clamp(
                        state.confidence
                                + cleanEvidence
                                + cleanUrgency * 0.035F,
                        0.0F,
                        1.0F
                );

        if (!wasActive
                && state.confidence
                        >= MIN_ACTIVE_CONFIDENCE) {
            playActivationSound(
                    player
            );
        }

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

    private static void playActivationSound(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                0.62F,
                0.72F
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.92F,
                1.28F
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS,
                0.34F,
                1.72F
        );
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

            state.centerMotion =
                    state.lastCenter == null
                            ? Vec3.ZERO
                            : center.subtract(
                                    state.lastCenter
                            );

            state.lastCenter =
                    center;

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
                    state
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

        tickHeldProjectiles(
                server
        );

        tickDrainedProjectiles(
                server
        );
    }

    private static void applyField(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center,
            float radius,
            float confidence,
            InfinityState state
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

            if (entity instanceof WarProjectileEntity warProjectile
                    && warProjectile.ownedBy(
                            owner.getUUID()
                    )) {
                continue;
            }

            if (protects(entity)) continue;

            boolean projectileLike =
                    entity instanceof Projectile
                            || entity instanceof WarProjectileEntity;

            if (projectileLike
                    && HELD_PROJECTILES.containsKey(
                            entity.getUUID()
                    )) {
                // A latched projectile is governed exclusively by
                // tickHeldProjectiles() until it crosses RELEASE_RADIUS.
                continue;
            }

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

            if (projectileLike) continue;

            /*
             * Non-projectile Infinity behaviour stays as before: living
             * entities/objects are progressively damped and can freeze near
             * the center.
             */
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

    /** Swept entry prevents even rounds faster than the field diameter tunnelling. */
    public static boolean advanceProjectile(ServerLevel level, Entity projectile) {
        if (HELD_PROJECTILES.containsKey(projectile.getUUID())) {
            projectile.setPos(projectile.position().add(projectile.getDeltaMovement()));
            projectile.hurtMarked = true;
            projectile.hasImpulse = true;
            return true;
        }
        // Released rounds have spent their momentum: let them fall without being recaptured.
        if (DRAINED_PROJECTILES.containsKey(projectile.getUUID())
                || projectile instanceof WarProjectileEntity round && round.momentumBroken()) return false;
        Vec3 from = projectile.position();
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 to = from.add(velocity);
        InfinityState nearest = null;
        Vec3 entry = null;
        double best = Double.POSITIVE_INFINITY;
        for (InfinityState state : ACTIVE.values()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(state.owner);
            if (owner == null || !protects(owner) || owner.level() != level
                    || projectile instanceof Projectile p && p.getOwner() == owner
                    || projectile instanceof WarProjectileEntity w && w.ownedBy(state.owner)) continue;
            Vec3 hit = segmentSphereEntry(from, to, owner.getEyePosition(), radiusFor(state.confidence));
            if (hit != null && from.distanceToSqr(hit) < best) {
                best = from.distanceToSqr(hit);
                entry = hit;
                nearest = state;
            }
        }
        if (nearest == null) return false;
        // Respect a wall BEFORE the field, rather than transporting rounds through it.
        var wall = level.clip(new net.minecraft.world.level.ClipContext(from, entry,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, projectile));
        if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                || hitsEntityBefore(level, projectile, from, entry)) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(nearest.owner);
        Vec3 center = owner.getEyePosition();
        double distance = entry.distanceTo(center);
        double gap = Math.max(0, distance - stopRadiusFor(nearest.confidence));
        double speed = velocity.length();
        // Fast rounds shed most of their speed on entry, but retain several visible steps.
        double nextSpeed = InfinityMath.slowedSpeed(speed, gap);
        if (gap < 0.08 || nextSpeed < 0.015) {
            projectile.setPos(entry);
            holdProjectile(level, projectile, nearest);
            projectile.hurtMarked = true;
            projectile.hasImpulse = true;
            return true;
        }
        Vec3 nextVelocity = velocity.normalize().scale(nextSpeed);
        Vec3 next = entry.add(nextVelocity);
        var innerWall = level.clip(new net.minecraft.world.level.ClipContext(entry, next,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, projectile));
        if (innerWall.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                || hitsEntityBefore(level, projectile, entry, next)) {
            // Let the projectile's own collision code process the wall at its reduced speed.
            Vec3 collisionStep = innerWall.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                    ? next : innerWall.getLocation();
            projectile.setDeltaMovement(collisionStep.subtract(from).scale(1.001));
            return false;
        }
        projectile.setPos(next);
        projectile.setDeltaMovement(nextVelocity);
        projectile.hurtMarked = true;
        projectile.hasImpulse = true;
        return true;
    }

    private static boolean hitsEntityBefore(ServerLevel level, Entity projectile, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) < 1.0E-9) return false;
        return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, projectile, from, to, new AABB(from, to).inflate(0.3),
                candidate -> candidate.isAlive() && candidate.isPickable() && !candidate.isSpectator()
                        && !(candidate instanceof Projectile) && !(candidate instanceof WarProjectileEntity)
                        && !(projectile instanceof Projectile shot && shot.getOwner() == candidate)
                        && !(projectile instanceof WarProjectileEntity round && round.ownedBy(candidate.getUUID()))) != null;
    }

    private static void holdProjectile(
            ServerLevel level,
            Entity entity,
            InfinityState state
    ) {
        HELD_PROJECTILES.put(
                entity.getUUID(),
                new HeldProjectile(
                        state.owner,
                        level.dimension()
                )
        );

        DRAINED_PROJECTILES.remove(
                entity.getUUID()
        );

        entity.setDeltaMovement(
                Vec3.ZERO
        );

        if (entity instanceof WarProjectileEntity warProjectile) {
            warProjectile.setInfinityHeld(
                    true
            );
        } else if (entity instanceof Projectile projectile) {
            projectile.setNoGravity(
                    true
            );
        }
    }

    private static void tickHeldProjectiles(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, HeldProjectile>>
                iterator =
                HELD_PROJECTILES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HeldProjectile> entry =
                    iterator.next();

            HeldProjectile held =
                    entry.getValue();

            ServerLevel level =
                    server.getLevel(
                            held.dimension
                    );

            InfinityState state =
                    ACTIVE.get(
                            held.infinityOwner
                    );

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    held.infinityOwner
                            );

            Entity entity =
                    level == null
                            ? null
                            : level.getEntity(
                                    entry.getKey()
                            );

            if (level == null
                    || entity == null
                    || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            if (state == null
                    || owner == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level
                    || state.confidence < MIN_ACTIVE_CONFIDENCE
                    || !hasSpectrum(
                            owner
                    )) {
                releaseProjectile(
                        level,
                        entity
                );
                iterator.remove();
                continue;
            }

            Vec3 center =
                    owner.getEyePosition();

            Vec3 relative =
                    entity.position()
                            .subtract(
                                    center
                            );

            double distance =
                    relative.length();

            if (distance
                    > releaseRadiusFor(
                            state.confidence
                    )) {
                releaseProjectile(
                        level,
                        entity
                );
                iterator.remove();
                continue;
            }

            Vec3 outward =
                    distance < 0.001
                            ? owner.getLookAngle()
                            .scale(
                                    -1.0
                            )
                            .normalize()
                            : relative.scale(
                                    1.0 / distance
                            );

            double ownerClosing =
                    state.centerMotion.dot(
                            outward
                    );

            Vec3 next =
                    Vec3.ZERO;

            if (ownerClosing > 0.012) {
                /*
                 * Actual world-space retreat. The bullet doesn't merely remain
                 * fixed while the player closes distance; it gains outward
                 * velocity and visibly travels backwards.
                 */
                double retreatSpeed =
                        Mth.clamp(
                                0.11
                                        + ownerClosing
                                                * 2.85,
                                0.11,
                                0.90
                        );

                next =
                        outward.scale(
                                retreatSpeed
                        );
            }

            if (entity instanceof WarProjectileEntity warProjectile) {
                warProjectile.setInfinityHeld(
                        true
                );
            } else if (entity instanceof Projectile projectile) {
                projectile.setNoGravity(
                        true
                );
            }

            entity.setDeltaMovement(
                    next
            );

            entity.fallDistance =
                    0.0F;
        }
    }

    private static void releaseProjectile(
            ServerLevel level,
            Entity entity
    ) {
        Vec3 velocity =
                entity.getDeltaMovement();

        /*
         * Whatever outward shove existed is retained only weakly. There is no
         * restoration of pre-Infinity speed; the projectile has spent its
         * momentum and begins falling immediately.
         */
        entity.setDeltaMovement(
                new Vec3(
                        velocity.x * 0.40,
                        Math.min(
                                velocity.y * 0.35,
                                -0.055
                        ),
                        velocity.z * 0.40
                )
        );

        if (entity instanceof WarProjectileEntity warProjectile) {
            warProjectile.setInfinityHeld(
                    false
            );
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

    private static void tickDrainedProjectiles(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, DrainedProjectile>>
                iterator =
                DRAINED_PROJECTILES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DrainedProjectile> entry =
                    iterator.next();

            if (HELD_PROJECTILES.containsKey(
                    entry.getKey()
            )) {
                continue;
            }

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

    private static float stopRadiusFor(
            float confidence
    ) {
        /*
         * Max Infinity: ~1.70 blocks from eye position. This is deliberately
         * well inside the visible/slowdown field so bullets visibly intrude
         * before finally stopping.
         */
        return 0.85F
                + confidence
                        * 0.85F;
    }

    private static float releaseRadiusFor(
            float confidence
    ) {
        /*
         * Hysteresis gap is intentionally huge. Once caught at ~1.7 blocks,
         * a max-confidence projectile must get ~7.7 blocks away before the
         * field releases it.
         */
        return stopRadiusFor(
                confidence
        )
                + 4.75F
                + confidence
                        * 1.25F;
    }

    private static float radiusFor(
            float confidence
    ) {
        return 3.25F
                + confidence
                        * 9.25F;
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
        HELD_PROJECTILES.clear();
    }

    private static final class HeldProjectile {

        private final UUID infinityOwner;
        private final ResourceKey<Level> dimension;

        private HeldProjectile(
                UUID infinityOwner,
                ResourceKey<Level> dimension
        ) {
            this.infinityOwner =
                    infinityOwner;
            this.dimension =
                    dimension;
        }
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
        private Vec3 lastCenter;
        private Vec3 centerMotion =
                Vec3.ZERO;

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
