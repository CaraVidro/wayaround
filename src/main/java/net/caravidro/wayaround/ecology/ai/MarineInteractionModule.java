package net.caravidro.wayaround.ecology.ai;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.ecology.AquaticPredator;
import net.caravidro.wayaround.ecology.MantaRayEntity;
import net.caravidro.wayaround.ecology.OarfishEntity;
import net.caravidro.wayaround.ecology.SeagullEntity;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.WhaleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Interactions whose participants belong to different ecological groups.
 *
 * The seagull is intentionally not a fake damage event: the prey remains the
 * same fish entity while being physically carried.
 */
public final class MarineInteractionModule {

    private static final String GULL_CARRY_UNTIL =
            "WayAroundGullCarryUntil";

    private static final String GULL_NEXT_HUNT =
            "WayAroundGullNextHunt";

    private MarineInteractionModule() {
    }

    public static void tickSeagulls(
            ServerLevel level
    ) {
        Set<UUID> touched =
                new HashSet<>();

        int processed =
                0;

        outer:
        for (var player : level.players()) {
            AABB area =
                    player.getBoundingBox()
                            .inflate(
                                    64.0,
                                    32.0,
                                    64.0
                            );

            for (SeagullEntity gull :
                    level.getEntitiesOfClass(
                            SeagullEntity.class,
                            area
                    )) {
                if (!touched.add(
                        gull.getUUID()
                )) {
                    continue;
                }

                tickSeagull(
                        level,
                        gull
                );

                if (++processed >= 96) {
                    break outer;
                }
            }
        }
    }

    private static void tickSeagull(
            ServerLevel level,
            SeagullEntity gull
    ) {
        long now =
                level.getGameTime();

        AbstractFish carried =
                gull.getPassengers()
                        .stream()
                        .filter(
                                AbstractFish.class::isInstance
                        )
                        .map(
                                AbstractFish.class::cast
                        )
                        .findFirst()
                        .orElse(null);

        if (carried != null) {
            carried.setAirSupply(
                    carried.getMaxAirSupply()
            );

            Vec3 forward =
                    gull.getLookAngle();

            double wantedY =
                    Math.max(
                            level.getSeaLevel()
                                    + 5.0,
                            gull.getY()
                                    + 2.0
                    );

            gull.getMoveControl()
                    .setWantedPosition(
                            gull.getX()
                                    + forward.x
                                            * 7.0,
                            wantedY,
                            gull.getZ()
                                    + forward.z
                                            * 7.0,
                            1.24
                    );

            if (now >= gull.getPersistentData()
                    .getLong(
                            GULL_CARRY_UNTIL
                    )) {
                eatCarriedFish(
                        level,
                        gull,
                        carried,
                        now
                );
            }

            return;
        }

        if (now < gull.getPersistentData()
                .getLong(
                        GULL_NEXT_HUNT
                )) {
            return;
        }

        AbstractFish prey =
                level.getEntitiesOfClass(
                                AbstractFish.class,
                                gull.getBoundingBox()
                                        .inflate(
                                                18.0,
                                                12.0,
                                                18.0
                                        ),
                                fish ->
                                        fish.isAlive()
                                                && !fish.isPassenger()
                                                && !(fish instanceof WhaleEntity)
                                                && !(fish instanceof MantaRayEntity)
                                                && !(fish instanceof OarfishEntity)
                                                && !(fish instanceof SunfishEntity)
                                                && !(fish instanceof AquaticPredator)
                                                && LivingFaunaManager.fishSize(
                                                fish
                                        ) <= 1.25F
                                                && nearSurface(
                                                level,
                                                fish.blockPosition()
                                        )
                        )
                        .stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        gull::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (prey == null) {
            gull.getPersistentData()
                    .putLong(
                            GULL_NEXT_HUNT,
                            now
                                    + 80L
                                    + level.random.nextInt(
                                    160
                            )
                    );

            return;
        }

        gull.getNavigation()
                .moveTo(
                        prey.getX(),
                        prey.getY()
                                + 0.7,
                        prey.getZ(),
                        1.58
                );

        if (gull.distanceToSqr(
                prey
        ) <= 2.3 * 2.3
                && prey.startRiding(
                gull,
                true
        )) {
            prey.setAirSupply(
                    prey.getMaxAirSupply()
            );

            gull.getPersistentData()
                    .putLong(
                            GULL_CARRY_UNTIL,
                            now
                                    + 80L
                                    + level.random.nextInt(
                                    121
                            )
                    );

            level.playSound(
                    null,
                    gull.blockPosition(),
                    SoundEvents.CHICKEN_AMBIENT,
                    SoundSource.NEUTRAL,
                    0.9F,
                    1.28F
            );

            level.sendParticles(
                    ParticleTypes.SPLASH,
                    prey.getX(),
                    prey.getY(),
                    prey.getZ(),
                    10,
                    0.24,
                    0.12,
                    0.24,
                    0.08
            );
        }
    }

    private static void eatCarriedFish(
            ServerLevel level,
            SeagullEntity gull,
            AbstractFish prey,
            long now
    ) {
        prey.stopRiding();

        level.playSound(
                null,
                gull.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.95F,
                1.15F
                        + level.random.nextFloat()
                                * 0.18F
        );

        level.sendParticles(
                ParticleTypes.POOF,
                gull.getX(),
                gull.getY()
                        - 0.38,
                gull.getZ(),
                9,
                0.20,
                0.14,
                0.20,
                0.02
        );

        prey.discard();

        gull.getPersistentData()
                .putLong(
                        GULL_NEXT_HUNT,
                        now
                                + 420L
                                + level.random.nextInt(
                                500
                        )
                );
    }

    private static boolean nearSurface(
            ServerLevel level,
            BlockPos origin
    ) {
        for (int dy = 1;
             dy <= 5;
             dy++) {
            if (!level.getFluidState(
                    origin.above(
                            dy
                    )
            ).is(
                    FluidTags.WATER
            )) {
                return true;
            }
        }

        return false;
    }
}
