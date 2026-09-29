package net.caravidro.wayaround.war;

import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Fire-control layer only.
 *
 * Every shot is now a real WarProjectileEntity. Physics, collision, Infinity
 * interaction and falling after momentum loss live on that entity itself.
 */
public final class WarBallistics {

    private WarBallistics() {}

    private static final Map<FireKey, Long> NEXT_FIRE =
            new HashMap<>();

    public static boolean tryFire(
            ServerPlayer player,
            WarGunItem.Kind kind
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.WAR_WITHOUT_REASON
        )) {
            return false;
        }

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
            spawn(
                    player,
                    kind,
                    0.90
            );
        } else {
            for (int index = 0;
                 index < kind.pellets;
                 index++) {
                spawn(
                        player,
                        kind,
                        0.58
                );
            }
        }

        muzzle(
                player,
                kind
        );

        WayAroundAdvancements.warShot(
                player
        );

        if (kind.rocket()) {
            WayAroundAdvancements.warRocket(
                    player
            );
        }

        return true;
    }

    private static void spawn(
            ServerPlayer player,
            WarGunItem.Kind kind,
            double muzzleOffset
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
                                        muzzleOffset
                                )
                        );

        WarProjectileEntity projectile =
                new WarProjectileEntity(
                        WarContent.WAR_PROJECTILE.get(),
                        player.serverLevel()
                );

        projectile.setPos(
                start.x,
                start.y,
                start.z
        );

        projectile.configure(
                player,
                kind,
                direction.scale(
                        kind.speed
                )
        );

        player.serverLevel()
                .addFreshEntity(
                        projectile
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
                                                : 0.68
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
        if ((event.getServer()
                .getTickCount()
                % 200L) != 0L) {
            return;
        }

        long now =
                event.getServer()
                        .getTickCount();

        NEXT_FIRE.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        < now
                );
    }

    public static void clearAll() {
        NEXT_FIRE.clear();
    }

    private record FireKey(
            UUID player,
            WarGunItem.Kind kind
    ) {}
}
