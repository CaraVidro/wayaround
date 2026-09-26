package net.caravidro.wayaround.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.caravidro.wayaround.infinity.InfinityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Continuous 3D ballistic simulation.
 *
 * These are not arrows and are not instant hitscan. A shot owns a position and
 * velocity, advances through world space every server tick and ray-tests the
 * full segment it crossed so fast rounds cannot tunnel through walls, entities
 * or Infinity.
 */
public final class WarBallistics {

    private WarBallistics() {}

    private static final List<Round> ROUNDS =
            new ArrayList<>();

    private static final Map<FireKey, Long> NEXT_FIRE =
            new HashMap<>();

    public static boolean tryFire(
            ServerPlayer player,
            WarGunItem.Kind kind
    ) {
        if (!player.isAlive()
                || player.isSpectator()) {
            return false;
        }

        long now =
                player.server
                        .getTickCount();

        FireKey key =
                new FireKey(
                        player.getUUID(),
                        kind
                );

        if (now
                < NEXT_FIRE.getOrDefault(
                        key,
                        0L
                )) {
            return false;
        }

        NEXT_FIRE.put(
                key,
                now
                        + kind.intervalTicks
        );

        if (kind.rocket()) {
            spawnRocket(
                    player,
                    kind
            );
        } else {
            for (int i = 0;
                 i < kind.pellets;
                 i++) {
                spawnBullet(
                        player,
                        kind
                );
            }
        }

        muzzle(
                player,
                kind
        );

        return true;
    }

    private static void spawnBullet(
            ServerPlayer player,
            WarGunItem.Kind kind
    ) {
        Vec3 direction =
                spreadDirection(
                        player,
                        kind.spread
                );

        Vec3 start =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        0.55
                                )
                        );

        ROUNDS.add(
                new Round(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        start,
                        direction.scale(
                                kind.speed
                        ),
                        kind.damage,
                        false,
                        0,
                        42
                )
        );
    }

    private static void spawnRocket(
            ServerPlayer player,
            WarGunItem.Kind kind
    ) {
        Vec3 direction =
                spreadDirection(
                        player,
                        kind.spread
                );

        Vec3 start =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        0.90
                                )
                        );

        ROUNDS.add(
                new Round(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        start,
                        direction.scale(
                                kind.speed
                        ),
                        kind.damage,
                        true,
                        0,
                        110
                )
        );
    }

    private static Vec3 spreadDirection(
            ServerPlayer player,
            double spread
    ) {
        Vec3 forward =
                player.getLookAngle()
                        .normalize();

        Vec3 worldUp =
                new Vec3(
                        0.0,
                        1.0,
                        0.0
                );

        Vec3 right =
                forward.cross(
                        worldUp
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

        Vec3 up =
                right.cross(
                        forward
                ).normalize();

        double horizontal =
                player.serverLevel()
                        .random
                        .nextGaussian()
                        * spread;

        double vertical =
                player.serverLevel()
                        .random
                        .nextGaussian()
                        * spread;

        return forward.add(
                        right.scale(
                                horizontal
                        )
                )
                .add(
                        up.scale(
                                vertical
                        )
                )
                .normalize();
    }

    private static void muzzle(
            ServerPlayer player,
            WarGunItem.Kind kind
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        Vec3 muzzle =
                player.getEyePosition()
                        .add(
                                look.scale(
                                        kind.rocket()
                                                ? 1.0
                                                : 0.65
                                )
                        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                muzzle.x,
                muzzle.y,
                muzzle.z,
                kind == WarGunItem.Kind.SHOTGUN
                        ? 10
                        : 4,
                0.06,
                0.06,
                0.06,
                0.015
        );

        level.sendParticles(
                ParticleTypes.FLAME,
                muzzle.x,
                muzzle.y,
                muzzle.z,
                kind.rocket()
                        ? 6
                        : 2,
                0.035,
                0.035,
                0.035,
                0.025
        );

        if (kind == WarGunItem.Kind.SHOTGUN) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS,
                    0.72F,
                    1.52F
            );
        } else if (kind.rocket()) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS,
                    1.1F,
                    0.68F
            );
        } else {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.PLAYERS,
                    kind == WarGunItem.Kind.MACHINE_GUN
                            ? 0.42F
                            : 0.58F,
                    kind == WarGunItem.Kind.MACHINE_GUN
                            ? 1.65F
                            : 1.48F
            );
        }
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        Iterator<Round> iterator =
                ROUNDS.iterator();

        while (iterator.hasNext()) {
            Round round =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            round.dimension
                    );

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    round.owner
                            );

            if (level == null
                    || owner == null
                    || owner.serverLevel() != level
                    || round.age++ >= round.maxAge) {
                iterator.remove();
                continue;
            }

            Vec3 from =
                    round.position;

            Vec3 to =
                    from.add(
                            round.velocity
                    );

            Impact impact =
                    trace(
                            level,
                            owner,
                            from,
                            to
                    );

            if (impact != null) {
                round.position =
                        impact.location;

                if (impact.infinity) {
                    infinityImpact(
                            level,
                            impact.location,
                            round.rocket
                    );
                } else if (round.rocket) {
                    if (impact.entity
                            instanceof LivingEntity living) {
                        living.hurt(
                                level.damageSources()
                                        .playerAttack(
                                                owner
                                        ),
                                round.damage
                        );
                    }

                    explodeRocket(
                            level,
                            owner,
                            impact.location
                    );
                } else {
                    bulletImpact(
                            level,
                            owner,
                            round,
                            impact
                    );
                }

                iterator.remove();
                continue;
            }

            round.position =
                    to;

            trail(
                    level,
                    round,
                    from,
                    to
            );

            if (!round.rocket) {
                round.velocity =
                        round.velocity.scale(
                                0.998
                        ).add(
                                0.0,
                                -0.008,
                                0.0
                        );
            }
        }

        if ((server.getTickCount()
                % 200L) == 0L) {
            long now =
                    server.getTickCount();

            NEXT_FIRE.entrySet()
                    .removeIf(
                            entry ->
                                    entry.getValue()
                                            < now
                    );
        }
    }

    private static Impact trace(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 from,
            Vec3 to
    ) {
        Impact nearest =
                null;

        BlockHitResult block =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                owner
                        )
                );

        if (block.getType()
                != HitResult.Type.MISS) {
            nearest =
                    new Impact(
                            block.getLocation(),
                            null,
                            false
                    );
        }

        Vec3 infinity =
                InfinityManager.clipHardProjectileBarrier(
                        level,
                        owner.getUUID(),
                        from,
                        to
                );

        if (infinity != null
                && closer(
                        from,
                        infinity,
                        nearest
                )) {
            nearest =
                    new Impact(
                            infinity,
                            null,
                            true
                    );
        }

        AABB swept =
                new AABB(
                        from,
                        to
                ).inflate(
                        0.45
                );

        for (Entity candidate :
                level.getEntities(
                        owner,
                        swept,
                        entity ->
                                entity.isAlive()
                                        && entity.isPickable()
                                        && entity != owner
                )) {

            Optional<Vec3> clipped =
                    candidate.getBoundingBox()
                            .inflate(
                                    0.22
                            )
                            .clip(
                                    from,
                                    to
                            );

            if (clipped.isEmpty()) {
                continue;
            }

            Vec3 location =
                    clipped.get();

            if (closer(
                    from,
                    location,
                    nearest
            )) {
                nearest =
                        new Impact(
                                location,
                                candidate,
                                false
                        );
            }
        }

        return nearest;
    }

    private static boolean closer(
            Vec3 from,
            Vec3 candidate,
            Impact current
    ) {
        return current == null
                || from.distanceToSqr(
                        candidate
                )
                < from.distanceToSqr(
                        current.location
                );
    }

    private static void bulletImpact(
            ServerLevel level,
            ServerPlayer owner,
            Round round,
            Impact impact
    ) {
        if (impact.entity
                instanceof LivingEntity living) {
            living.hurt(
                    level.damageSources()
                            .playerAttack(
                                    owner
                            ),
                    round.damage
            );

            Vec3 push =
                    round.velocity.normalize()
                            .scale(
                                    0.34
                            );

            living.push(
                    push.x,
                    0.08,
                    push.z
            );
        }

        level.sendParticles(
                impact.entity == null
                        ? ParticleTypes.SMOKE
                        : ParticleTypes.CRIT,
                impact.location.x,
                impact.location.y,
                impact.location.z,
                impact.entity == null
                        ? 5
                        : 9,
                0.09,
                0.09,
                0.09,
                0.03
        );
    }

    private static void explodeRocket(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 location
    ) {
        level.explode(
                owner,
                location.x,
                location.y,
                location.z,
                5.4F,
                true,
                Level.ExplosionInteraction.TNT
        );
    }

    private static void infinityImpact(
            ServerLevel level,
            Vec3 location,
            boolean rocket
    ) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                location.x,
                location.y,
                location.z,
                rocket
                        ? 30
                        : 10,
                rocket
                        ? 0.42
                        : 0.12,
                rocket
                        ? 0.42
                        : 0.12,
                rocket
                        ? 0.42
                        : 0.12,
                0.025
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                location.x,
                location.y,
                location.z,
                rocket
                        ? 22
                        : 6,
                0.24,
                0.24,
                0.24,
                0.03
        );

        level.playSound(
                null,
                BlockPos.containing(
                        location
                ),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                rocket
                        ? 1.25F
                        : 0.48F,
                rocket
                        ? 0.62F
                        : 1.42F
        );
    }

    private static void trail(
            ServerLevel level,
            Round round,
            Vec3 from,
            Vec3 to
    ) {
        if (round.rocket) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    from.x,
                    from.y,
                    from.z,
                    3,
                    0.05,
                    0.05,
                    0.05,
                    0.005
            );

            level.sendParticles(
                    ParticleTypes.FLAME,
                    from.x,
                    from.y,
                    from.z,
                    2,
                    0.025,
                    0.025,
                    0.025,
                    0.005
            );

            return;
        }

        for (int i = 1;
             i <= 3;
             i++) {
            double t =
                    i / 4.0;

            Vec3 point =
                    from.lerp(
                            to,
                            t
                    );

            level.sendParticles(
                    ParticleTypes.CRIT,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public static void clearAll() {
        ROUNDS.clear();
        NEXT_FIRE.clear();
    }

    private static final class Round {

        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private Vec3 position;
        private Vec3 velocity;
        private final float damage;
        private final boolean rocket;
        private int age;
        private final int maxAge;

        private Round(
                UUID owner,
                ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                float damage,
                boolean rocket,
                int age,
                int maxAge
        ) {
            this.owner =
                    owner;
            this.dimension =
                    dimension;
            this.position =
                    position;
            this.velocity =
                    velocity;
            this.damage =
                    damage;
            this.rocket =
                    rocket;
            this.age =
                    age;
            this.maxAge =
                    maxAge;
        }
    }

    private record Impact(
            Vec3 location,
            Entity entity,
            boolean infinity
    ) {}

    private record FireKey(
            UUID player,
            WarGunItem.Kind kind
    ) {}
}
