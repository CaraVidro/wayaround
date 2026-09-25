package net.caravidro.wayaround.cursed;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Desmartelar {

    private Desmartelar() {
    }

    public static void cast(
            ServerPlayer caster,
            int fingers
    ) {
        ServerLevel level =
                caster.serverLevel();

        int power =
                Mth.clamp(
                        fingers,
                        1,
                        20
                );

        double length =
                7.5
                        + power
                                * 0.27;

        double width =
                0.42
                        + power
                                * 0.028;

        float damage =
                7.0F
                        + power
                                * 0.62F;

        Vec3 direction =
                caster.getLookAngle()
                        .normalize();

        Vec3 start =
                caster.getEyePosition()
                        .add(
                                direction.scale(
                                        0.65
                                )
                        );

        Vec3 end =
                start.add(
                        direction.scale(
                                length
                        )
                );

        destroyBlocks(
                level,
                caster,
                start,
                direction,
                length,
                width
        );

        damageEntities(
                level,
                caster,
                start,
                end,
                width,
                damage
        );

        DesmartelarSlashEntity slash =
                new DesmartelarSlashEntity(
                        WayAroundContent.DESMARTELAR_SLASH.get(),
                        level
                );

        Vec3 middle =
                start.lerp(
                        end,
                        0.5
                );

        slash.setPos(
                middle.x,
                middle.y,
                middle.z
        );

        slash.setYRot(
                caster.getYRot()
        );

        slash.setXRot(
                caster.getXRot()
        );

        slash.configure(
                (float) length,
                (float) width
        );

        level.addFreshEntity(
                slash
        );

        level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                end.x,
                end.y,
                end.z,
                10,
                width,
                width * 0.6,
                width,
                0.0
        );

        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                1.25F,
                0.55F
        );
    }

    private static void destroyBlocks(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 direction,
            double length,
            double width
    ) {
        Vec3 right =
                new Vec3(
                        -direction.z,
                        0.0,
                        direction.x
                );

        if (right.lengthSqr() < 0.0001) {
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
                direction.cross(
                        right
                );

        if (up.lengthSqr() < 0.0001) {
            up =
                    new Vec3(
                            0.0,
                            1.0,
                            0.0
                    );
        } else {
            up =
                    up.normalize();
        }

        Set<BlockPos> touched =
                new HashSet<>();

        int radial =
                Math.max(
                        0,
                        Mth.ceil(
                                width
                        )
                );

        for (double distance = 0.0;
             distance <= length;
             distance += 0.42) {

            Vec3 center =
                    start.add(
                            direction.scale(
                                    distance
                            )
                    );

            for (int x = -radial;
                 x <= radial;
                 x++) {

                for (int y = -radial;
                     y <= radial;
                     y++) {

                    Vec3 sample =
                            center.add(
                                    right.scale(
                                            x * 0.55
                                    )
                            ).add(
                                    up.scale(
                                            y * 0.55
                                    )
                            );

                    BlockPos pos =
                            BlockPos.containing(
                                    sample
                            );

                    if (!touched.add(
                            pos
                    )) {
                        continue;
                    }

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    if (state.isAir()) {
                        continue;
                    }

                    float hardness =
                            state.getDestroySpeed(
                                    level,
                                    pos
                            );

                    if (hardness < 0.0F) {
                        continue;
                    }

                    level.destroyBlock(
                            pos,
                            true,
                            caster
                    );
                }
            }
        }
    }

    private static void damageEntities(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 end,
            double width,
            float damage
    ) {
        AABB search =
                new AABB(
                        start,
                        end
                ).inflate(
                        width + 1.0
                );

        Vec3 segment =
                end.subtract(
                        start
                );

        double segmentLengthSq =
                segment.lengthSqr();

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        entity ->
                                entity != caster
                                        && entity.isAlive()
                )) {

            Vec3 point =
                    living.getBoundingBox()
                            .getCenter();

            double t =
                    segmentLengthSq <= 0.0001
                            ? 0.0
                            : point.subtract(start)
                                    .dot(segment)
                                    / segmentLengthSq;

            t =
                    Mth.clamp(
                            t,
                            0.0,
                            1.0
                    );

            Vec3 closest =
                    start.add(
                            segment.scale(
                                    t
                            )
                    );

            double hitRadius =
                    width
                            + living.getBbWidth()
                                    * 0.55;

            if (point.distanceToSqr(
                    closest
            )
                    > hitRadius
                            * hitRadius) {
                continue;
            }

            living.hurt(
                    caster.damageSources()
                            .playerAttack(
                                    caster
                            ),
                    damage
            );

            Vec3 push =
                    segment.normalize()
                            .scale(
                                    0.38
                            );

            living.push(
                    push.x,
                    0.12,
                    push.z
            );
        }
    }
}
