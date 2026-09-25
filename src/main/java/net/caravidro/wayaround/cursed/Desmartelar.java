package net.caravidro.wayaround.cursed;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
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
        cast(
                caster,
                fingers,
                false
        );
    }

    public static void cast(
            ServerPlayer caster,
            int fingers,
            boolean fire
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
                8.5
                        + power
                                * 0.34;

        double halfSpan =
                1.3
                        + power
                                * 0.075;

        float damage =
                7.5F
                        + power
                                * 0.68F;

        Vec3 forward =
                caster.getLookAngle()
                        .normalize();

        Vec3 right =
                forward.cross(
                        new Vec3(
                                0.0,
                                1.0,
                                0.0
                        )
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
                right.cross(
                        forward
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

        Vec3 start =
                caster.getEyePosition()
                        .add(
                                forward.scale(
                                        0.9
                                )
                        );

        /*
         * Instead of one ray from caster -> target, Desmartelar is now a set
         * of travelling '-' shaped cutting planes. Each plane slices across
         * the forward path from a different angle.
         */
        double[] angles = {
                0.0,
                Math.toRadians(38.0),
                Math.toRadians(-42.0),
                Math.toRadians(78.0)
        };

        for (int band = 0;
             band < angles.length;
             band++) {

            double angle =
                    angles[band];

            Vec3 lateral =
                    right.scale(
                            Math.cos(
                                    angle
                            )
                    )
                            .add(
                                    up.scale(
                                            Math.sin(
                                                    angle
                                            )
                                    )
                            )
                            .normalize();

            sweepBand(
                    level,
                    caster,
                    start,
                    forward,
                    lateral,
                    length,
                    halfSpan
                            * (
                            0.82
                                    + band
                                            * 0.07
                    ),
                    damage,
                    fire
            );

            spawnSlash(
                    level,
                    caster,
                    start,
                    forward,
                    length,
                    halfSpan,
                    band,
                    angle
            );
        }

        Vec3 end =
                start.add(
                        forward.scale(
                                length
                        )
                );

        level.sendParticles(
                fire
                        ? ParticleTypes.FLAME
                        : ParticleTypes.SWEEP_ATTACK,
                end.x,
                end.y,
                end.z,
                fire
                        ? 44
                        : 14,
                halfSpan,
                halfSpan * 0.7,
                halfSpan,
                fire
                        ? 0.08
                        : 0.0
        );

        level.playSound(
                null,
                caster.blockPosition(),
                fire
                        ? SoundEvents.FIRECHARGE_USE
                        : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                fire
                        ? 1.45F
                        : 1.25F,
                fire
                        ? 0.72F
                        : 0.55F
        );
    }

    private static void sweepBand(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 forward,
            Vec3 lateral,
            double length,
            double halfSpan,
            float damage,
            boolean fire
    ) {
        Set<BlockPos> touched =
                new HashSet<>();

        AABB affected =
                new AABB(
                        start,
                        start.add(
                                forward.scale(
                                        length
                                )
                        )
                ).inflate(
                        halfSpan + 1.8
                );

        for (double distance = 0.0;
             distance <= length;
             distance += 0.48) {

            Vec3 center =
                    start.add(
                            forward.scale(
                                    distance
                            )
                    );

            for (double across = -halfSpan;
                 across <= halfSpan;
                 across += 0.46) {

                Vec3 sample =
                        center.add(
                                lateral.scale(
                                        across
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

                if (!state.isAir()
                        && state.getDestroySpeed(
                        level,
                        pos
                ) >= 0.0F) {

                    level.destroyBlock(
                            pos,
                            true,
                            caster
                    );

                    if (fire) {
                        igniteAround(
                                level,
                                pos
                        );
                    }
                }
            }
        }

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        affected,
                        entity ->
                                entity != caster
                                        && entity.isAlive()
                )) {

            Vec3 relative =
                    living.getBoundingBox()
                            .getCenter()
                            .subtract(
                                    start
                            );

            double along =
                    relative.dot(
                            forward
                    );

            if (along < -0.6
                    || along > length + 0.8) {
                continue;
            }

            Vec3 planeCenter =
                    start.add(
                            forward.scale(
                                    Mth.clamp(
                                            along,
                                            0.0,
                                            length
                                    )
                            )
                    );

            double across =
                    Math.abs(
                            living.getBoundingBox()
                                    .getCenter()
                                    .subtract(
                                            planeCenter
                                    )
                                    .dot(
                                            lateral
                                    )
                    );

            if (across
                    > halfSpan
                            + living.getBbWidth()) {
                continue;
            }

            living.hurt(
                    caster.damageSources()
                            .playerAttack(
                                    caster
                            ),
                    damage
            );

            if (fire) {
                living.igniteForSeconds(
                        9.0F
                );
            }

            living.push(
                    forward.x * 0.42,
                    0.10,
                    forward.z * 0.42
            );
        }
    }

    private static void igniteAround(
            ServerLevel level,
            BlockPos origin
    ) {
        for (Direction direction :
                Direction.values()) {

            BlockPos firePos =
                    origin.relative(
                            direction
                    );

            if (!level.getBlockState(
                    firePos
            ).isAir()) {
                continue;
            }

            BlockState fire =
                    Blocks.FIRE
                            .defaultBlockState();

            if (fire.canSurvive(
                    level,
                    firePos
            )) {
                level.setBlock(
                        firePos,
                        fire,
                        3
                );
            }
        }
    }

    private static void spawnSlash(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 forward,
            double length,
            double halfSpan,
            int band,
            double angle
    ) {
        DesmartelarSlashEntity slash =
                new DesmartelarSlashEntity(
                        WayAroundContent.DESMARTELAR_SLASH.get(),
                        level
                );

        Vec3 middle =
                start.add(
                        forward.scale(
                                length
                                        * (
                                        0.38
                                                + band
                                                        * 0.10
                                )
                        )
                );

        slash.setPos(
                middle.x,
                middle.y,
                middle.z
        );

        slash.setYRot(
                caster.getYRot()
                        + (float) Math.toDegrees(
                        angle
                )
        );

        slash.setXRot(
                caster.getXRot()
                        + (
                        band % 2 == 0
                                ? 0.0F
                                : 24.0F
                )
        );

        slash.configure(
                (float) (
                        halfSpan
                                * 2.0
                ),
                0.24F
                        + band
                                * 0.035F
        );

        level.addFreshEntity(
                slash
        );
    }
}
