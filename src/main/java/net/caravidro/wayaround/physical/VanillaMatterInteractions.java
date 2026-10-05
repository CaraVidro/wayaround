package net.caravidro.wayaround.physical;

import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Small vanilla-facing integration probes for the universal matter foundation.
 *
 * <p>This is not the future universal combustion solver. It proves that vanilla
 * blocks can participate in WayAround physics without a bespoke duplicate
 * block: campfire -> ember -> physical material -> vanilla fire -> existing
 * WayAround dynamic-fire layer.</p>
 */
public final class VanillaMatterInteractions {

    private VanillaMatterInteractions() {
    }

    public static void tickLitCampfire(
            ServerLevel level,
            BlockPos campfirePos,
            BlockState campfireState
    ) {
        if (!campfireState.hasProperty(
                CampfireBlock.LIT
        )
                || !campfireState.getValue(
                CampfireBlock.LIT
        )
                || !level.getGameRules()
                .getBoolean(
                        GameRules.RULE_DOFIRETICK
                )) {
            return;
        }

        long phase =
                Math.floorMod(
                        campfirePos.asLong(),
                        12L
                );

        if (Math.floorMod(
                level.getGameTime(),
                12L
        ) != phase
                || level.random.nextFloat()
                > 0.42F) {
            return;
        }

        /*
         * Existing weather wind is only a compatibility source here. Step 5 of
         * the roadmap replaces this with the universal flow field.
         */
        double windAngle =
                BlizzardWind.angle(
                        level.getGameTime()
                );

        double distance =
                2.0
                        + level.random.nextDouble()
                                * 3.5;

        double sideways =
                (
                        level.random.nextDouble()
                                - 0.5
                )
                        * 1.8;

        double dx =
                Math.cos(
                        windAngle
                )
                        * distance
                        - Math.sin(
                        windAngle
                )
                        * sideways;

        double dz =
                Math.sin(
                        windAngle
                )
                        * distance
                        + Math.cos(
                        windAngle
                )
                        * sideways;

        BlockPos approximateContact =
                campfirePos.offset(
                        (int) Math.round(
                                dx
                        ),
                        level.random.nextInt(
                                3
                        ) - 1,
                        (int) Math.round(
                                dz
                        )
                );

        emitEmberTrail(
                level,
                campfirePos,
                approximateContact
        );

        tryIgniteNear(
                level,
                approximateContact
        );
    }

    private static void emitEmberTrail(
            ServerLevel level,
            BlockPos from,
            BlockPos to
    ) {
        Vec3 start =
                Vec3.atCenterOf(
                        from
                ).add(
                        0.0,
                        0.45,
                        0.0
                );

        Vec3 end =
                Vec3.atCenterOf(
                        to
                ).add(
                        0.0,
                        0.28,
                        0.0
                );

        for (int step = 1;
             step <= 5;
             step++) {
            double t =
                    step / 5.0;

            Vec3 point =
                    start.lerp(
                            end,
                            t
                    ).add(
                            0.0,
                            Math.sin(
                                    Math.PI * t
                            )
                                    * 0.85,
                            0.0
                    );

            level.sendParticles(
                    ParticleTypes.SMALL_FLAME,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.015,
                    0.015,
                    0.015,
                    0.005
            );
        }
    }

    private static void tryIgniteNear(
            ServerLevel level,
            BlockPos approximate
    ) {
        int[] verticalOffsets =
                {
                        1,
                        0,
                        -1,
                        -2
                };

        for (int vertical :
                verticalOffsets) {
            BlockPos fuelPos =
                    approximate.offset(
                            0,
                            vertical,
                            0
                    );

            if (!level.hasChunkAt(
                    fuelPos
            )) {
                continue;
            }

            BlockState fuel =
                    level.getBlockState(
                            fuelPos
                    );

            float flammability =
                    BlockMatterResolver.exposedFlammability(
                            fuel
                    );

            if (flammability <= 0.05F) {
                continue;
            }

            if (level.isRainingAt(
                    fuelPos.above()
            )) {
                return;
            }

            float ignitionChance =
                    0.08F
                            + flammability
                                    * 0.28F;

            if (level.random.nextFloat()
                    > ignitionChance) {
                return;
            }

            Direction[] directions =
                    Direction.values();

            int start =
                    level.random.nextInt(
                            directions.length
                    );

            for (int index = 0;
                 index < directions.length;
                 index++) {
                Direction direction =
                        directions[
                                (
                                        start
                                                + index
                                )
                                        % directions.length
                                ];

                BlockPos firePos =
                        fuelPos.relative(
                                direction
                        );

                if (!level.hasChunkAt(
                        firePos
                )
                        || !level.getBlockState(
                        firePos
                ).isAir()
                        || !level.getFluidState(
                        firePos
                ).isEmpty()) {
                    continue;
                }

                BlockState fire =
                        BaseFireBlock.getState(
                                level,
                                firePos
                        );

                if (!fire.canSurvive(
                        level,
                        firePos
                )) {
                    continue;
                }

                level.setBlock(
                        firePos,
                        fire,
                        3
                );

                level.sendParticles(
                        ParticleTypes.FLAME,
                        firePos.getX() + 0.5,
                        firePos.getY() + 0.25,
                        firePos.getZ() + 0.5,
                        5,
                        0.14,
                        0.08,
                        0.14,
                        0.01
                );

                /*
                 * FireBlockMixin observes this vanilla fire placement and
                 * registers it with EnhancedFireVisuals automatically.
                 */
                return;
            }

            return;
        }
    }
}
