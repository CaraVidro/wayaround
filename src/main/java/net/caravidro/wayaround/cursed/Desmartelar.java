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

    /**
     * Desmartelar is a moving cutting plane rather than a ray emitted directly
     * from the player's face. Three slightly different planes cross the target
     * corridor, producing the "— moving through space" behavior.
     */
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

        double travel =
                9.0
                        + power
                                * 0.36;

        double span =
                3.6
                        + power
                                * 0.16;

        double thickness =
                0.30
                        + power
                                * 0.014;

        float damage =
                6.0F
                        + power
                                * 0.58F;

        Vec3 look =
                caster.getLookAngle()
                        .normalize();

        Vec3 right =
                horizontalRight(
                        look
                );

        double[] yawOffsets = {
                -0.11,
                0.0,
                0.13
        };

        double[] verticalOffsets = {
                0.72,
                0.05,
                -0.58
        };

        double[] lateralOffsets = {
                -0.70,
                0.35,
                0.85
        };

        float[] rolls = {
                -18.0F,
                4.0F,
                24.0F
        };

        for (int index = 1;
             index < 2;
             index++) {

            Vec3 direction =
                    rotateYaw(
                            look,
                            yawOffsets[index]
                    )
                            .normalize();

            Vec3 start =
                    caster.getEyePosition()
                            .add(
                                    direction.scale(
                                            2.0
                                    )
                            )
                            .add(
                                    right.scale(
                                            lateralOffsets[index]
                                    )
                            )
                            .add(
                                    0.0,
                                    verticalOffsets[index],
                                    0.0
                            );

            if (fire) {
                for (int heatStep = 0; heatStep < 4; heatStep++) {
                    net.caravidro.wayaround.thermal.RegionalTemperature.pulse(level,
                            start.add(direction.scale(heatStep * travel / 3)), 7, 2600);
                }
            }
            cutCorridor(
                    level,
                    caster,
                    start,
                    direction,
                    travel,
                    span,
                    thickness,
                    damage,
                    fire
            );

            spawnMovingSlash(
                    level,
                    caster,
                    start,
                    direction,
                    span,
                    thickness,
                    rolls[index],
                    fire
            );
        }

        Vec3 far =
                caster.getEyePosition()
                        .add(
                                look.scale(
                                        travel + 2.0
                                )
                        );

        level.sendParticles(
                fire
                        ? ParticleTypes.FLAME
                        : ParticleTypes.SWEEP_ATTACK,
                far.x,
                far.y,
                far.z,
                fire
                        ? 36
                        : 14,
                span * 0.32,
                0.8,
                span * 0.32,
                fire
                        ? 0.10
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
                        ? 1.6F
                        : 1.25F,
                fire
                        ? 0.48F
                        : 0.55F
        );
    }

    private static void spawnMovingSlash(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 direction,
            double span,
            double thickness,
            float roll,
            boolean fire
    ) {
        DesmartelarSlashEntity slash =
                new DesmartelarSlashEntity(
                        WayAroundContent.DESMARTELAR_SLASH.get(),
                        level
                );

        slash.setPos(
                start.x,
                start.y,
                start.z
        );

        slash.setYRot(
                caster.getYRot()
        );

        slash.setXRot(
                caster.getXRot()
                        * 0.22F
        );

        slash.configure(
                (float) span,
                (float) (
                        thickness
                                * 2.2
                ),
                roll,
                fire,
                direction.scale(
                        1.35
                )
        );

        level.addFreshEntity(
                slash
        );
    }

    private static void cutCorridor(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 start,
            Vec3 direction,
            double travel,
            double span,
            double thickness,
            float damage,
            boolean fire
    ) {
        Vec3 right =
                horizontalRight(
                        direction
                );

        Vec3 up =
                direction.cross(
                        right
                );

        if (up.lengthSqr()
                < 0.0001) {
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

        for (double distance = 0.0;
             distance <= travel;
             distance += 0.45) {

            Vec3 planeCenter =
                    start.add(
                            direction.scale(
                                    distance
                            )
                    );

            for (double across = -span * 0.5;
                 across <= span * 0.5;
                 across += 0.55) {

                for (double vertical = -thickness;
                     vertical <= thickness;
                     vertical += 0.42) {

                    Vec3 sample =
                            planeCenter
                                    .add(
                                            right.scale(
                                                    across
                                            )
                                    )
                                    .add(
                                            up.scale(
                                                    vertical
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

                    if (state.isAir()
                            || state.getDestroySpeed(
                            level,
                            pos
                    ) < 0.0F) {
                        continue;
                    }

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

        AABB search =
                new AABB(
                        start,
                        start.add(
                                direction.scale(
                                        travel
                                )
                        )
                ).inflate(
                        span * 0.65,
                        1.55,
                        span * 0.65
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
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
                            direction
                    );

            if (along < -1.0
                    || along > travel + 1.0) {
                continue;
            }

            Vec3 planeCenter =
                    start.add(
                            direction.scale(
                                    Mth.clamp(
                                            along,
                                            0.0,
                                            travel
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
                                            right
                                    )
                    );

            if (across
                    > span * 0.62
                            + living.getBbWidth()
                                    * 0.55) {
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
                        12.0F
                );
            }

            living.push(
                    direction.x * 0.34,
                    0.10,
                    direction.z * 0.34
            );
        }
    }

    private static void igniteAround(
            ServerLevel level,
            BlockPos origin
    ) {
        BlockPos[] candidates = {
                origin,
                origin.above(),
                origin.north(),
                origin.south(),
                origin.east(),
                origin.west()
        };

        for (BlockPos pos :
                candidates) {

            if (!level.getBlockState(
                    pos
            ).isAir()) {
                continue;
            }

            BlockState fire =
                    Blocks.FIRE
                            .defaultBlockState();

            if (fire.canSurvive(
                    level,
                    pos
            )) {
                level.setBlock(
                        pos,
                        fire,
                        3
                );
                return;
            }
        }
    }

    private static Vec3 horizontalRight(
            Vec3 direction
    ) {
        Vec3 right =
                new Vec3(
                        -direction.z,
                        0.0,
                        direction.x
                );

        return right.lengthSqr()
                < 0.0001
                ? new Vec3(
                1.0,
                0.0,
                0.0
        )
                : right.normalize();
    }

    private static Vec3 rotateYaw(
            Vec3 vector,
            double radians
    ) {
        double sin =
                Math.sin(
                        radians
                );

        double cos =
                Math.cos(
                        radians
                );

        return new Vec3(
                vector.x * cos
                        - vector.z * sin,
                vector.y,
                vector.x * sin
                        + vector.z * cos
        );
    }
}

