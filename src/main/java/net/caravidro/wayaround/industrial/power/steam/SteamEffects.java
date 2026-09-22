package net.caravidro.wayaround.industrial.power.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SteamEffects {
    private SteamEffects() {}

    public static void leak(ServerLevel level, BlockPos pos, int particles, double speed) {
        level.sendParticles(ParticleTypes.CLOUD,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            Math.max(1, particles), 0.18, 0.18, 0.18, speed);
    }

    public static void hiss(ServerLevel level, BlockPos pos, float volume) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
            SoundSource.BLOCKS, volume, 1.25F + level.random.nextFloat() * 0.2F);
    }

    public static void rupture(ServerLevel level, BlockPos pos, int releasedSteam,
            double radius, float maxDamage) {
        int particles = Math.max(18, Math.min(80, releasedSteam / 20));
        level.sendParticles(ParticleTypes.CLOUD,
            pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
            particles, 0.45, 0.45, 0.45, 0.12);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE,
            SoundSource.BLOCKS, 0.9F, 1.25F);

        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, new AABB(pos).inflate(radius))) {
            Vec3 delta = entity.position().subtract(center);
            double distance = Math.max(0.25, delta.length());
            double scale = Math.max(0.0, 1.0 - distance / radius);
            if (scale <= 0.0) continue;

            entity.hurt(level.damageSources().hotFloor(), Math.max(0.5F, maxDamage * (float) scale));
            Vec3 push = delta.lengthSqr() < 0.0001
                ? new Vec3(0.0, 0.45, 0.0)
                : delta.normalize().scale(0.9 * scale).add(0.0, 0.25 * scale, 0.0);
            entity.push(push.x, push.y, push.z);
        }
    }
}
