package net.caravidro.wayaround.spectrum;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared launch/tunnel routine for Spectrum melee finishers and high Black
 * Flash charges. One implementation means every "launch them through the wall"
 * move obeys the same world rules.
 */
public final class SpectrumImpact {

    private SpectrumImpact() {}

    public static void launchAndBreak(
            ServerPlayer attacker,
            LivingEntity target,
            Vec3 velocity,
            double pathDistance,
            int radius
    ) {
        if (!(target.level()
                instanceof ServerLevel level)) {
            return;
        }

        Vec3 direction =
                velocity.lengthSqr()
                        < 1.0E-6
                        ? attacker.getLookAngle()
                        : velocity.normalize();

        breakPath(
                level,
                attacker,
                target.position()
                        .add(
                                0.0,
                                target.getBbHeight() * 0.55,
                                0.0
                        ),
                direction,
                pathDistance,
                radius
        );

        target.setDeltaMovement(
                velocity
        );

        target.fallDistance =
                0.0F;

        level.sendParticles(
                ParticleTypes.CLOUD,
                target.getX(),
                target.getY()
                        + target.getBbHeight() * 0.55,
                target.getZ(),
                26 + radius * 12,
                0.35 + radius * 0.18,
                0.28 + radius * 0.12,
                0.35 + radius * 0.18,
                0.12
        );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.52F + radius * 0.18F,
                1.42F - radius * 0.10F
        );
    }

    private static void breakPath(
            ServerLevel level,
            ServerPlayer attacker,
            Vec3 start,
            Vec3 direction,
            double distance,
            int radius
    ) {
        Set<BlockPos> visited =
                new HashSet<>();

        int samples =
                Math.max(
                        1,
                        (int)Math.ceil(
                                distance / 0.42
                        )
                );

        for (int index = 1;
             index <= samples;
             index++) {
            double t =
                    distance
                            * index
                            / samples;

            Vec3 center =
                    start.add(
                            direction.scale(
                                    t
                            )
                    );

            for (int x = -radius;
                 x <= radius;
                 x++) {
                for (int y = -radius;
                     y <= radius;
                     y++) {
                    for (int z = -radius;
                         z <= radius;
                         z++) {
                        if (x * x + y * y + z * z
                                > radius * radius + 1) {
                            continue;
                        }

                        BlockPos pos =
                                BlockPos.containing(
                                        center.x + x,
                                        center.y + y,
                                        center.z + z
                                );

                        if (!visited.add(
                                pos
                        )) {
                            continue;
                        }

                        BlockState state =
                                level.getBlockState(
                                        pos
                                );

                        if (state.isAir()
                                || state.getDestroySpeed(
                                level,
                                pos
                        ) < 0.0F) {
                            continue;
                        }

                        level.destroyBlock(
                                pos,
                                false,
                                attacker
                        );
                    }
                }
            }
        }
    }
}
