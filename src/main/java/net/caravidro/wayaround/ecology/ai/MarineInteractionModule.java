package net.caravidro.wayaround.ecology.ai;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.Entity;

import net.caravidro.wayaround.ecology.AquaticPredator;
import net.caravidro.wayaround.ecology.MantaRayEntity;
import net.caravidro.wayaround.ecology.OarfishEntity;
import net.caravidro.wayaround.ecology.SeagullEntity;
import net.caravidro.wayaround.ecology.SunfishEntity;
import net.caravidro.wayaround.ecology.WhaleCarcassEntity;
import net.caravidro.wayaround.ecology.WhaleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cross-species marine interactions.
 *
 * Seagulls now perform a real fishing sequence:
 * cruise -> dive -> physically carry the fish -> climb -> land -> eat.
 */
public final class MarineInteractionModule {

    private static final String GULL_PHASE =
            "WayAroundGullPhase";
    private static final String GULL_DIVE_UNTIL =
            "WayAroundGullDiveUntil";
    private static final String GULL_NEXT_HUNT =
            "WayAroundGullNextHunt";
    private static final String GULL_LANDING =
            "WayAroundGullLanding";
    private static final String GULL_EAT_AT =
            "WayAroundGullEatAt";

    private static final String GULL_SUNFISH_TARGET =
            "WayAroundGullSunfishTarget";

    private static final String GULL_SUNFISH_PECK_AT =
            "WayAroundGullSunfishPeckAt";

    private static final String GULL_SUNFISH_LEAVE_AT =
            "WayAroundGullSunfishLeaveAt";

    private static final int PHASE_CRUISE = 0;
    private static final int PHASE_DIVE = 1;
    private static final int PHASE_ASCEND = 2;
    private static final int PHASE_LAND = 3;
    private static final int PHASE_EAT = 4;

    /*
     * A gull can lift sardines, tropical fish, clownfish and similar prey,
     * but not an absurd player-sized survivor. Large flying fish can also
     * eventually grow beyond this threshold and become safe from gulls.
     */
    private static final float MAX_GULL_PREY_SIZE =
            1.10F;

    private MarineInteractionModule() {
    }

    public static void tickSeagulls(
            ServerLevel level
    ) {
        IntOpenHashSet touched =
                new IntOpenHashSet(
                        192
                );

        int processed =
                0;

        outer:
        for (var player : level.players()) {
            AABB area =
                    player.getBoundingBox()
                            .inflate(
                                    72.0,
                                    40.0,
                                    72.0
                            );

            for (SeagullEntity gull :
                    level.getEntitiesOfClass(
                            SeagullEntity.class,
                            area
                    )) {
                if (!touched.add(
                        gull.getId()
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

    private static <T extends Entity> T nearest(
            Entity origin,
            List<T> candidates
    ) {
        T nearest =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (T candidate :
                candidates) {

            double distance =
                    origin.distanceToSqr(
                            candidate
                    );

            if (distance < bestDistance) {
                bestDistance =
                        distance;

                nearest =
                        candidate;
            }
        }

        return nearest;
    }

    private static void tickSeagull(
            ServerLevel level,
            SeagullEntity gull
    ) {
        long now =
                level.getGameTime();

        var data =
                gull.getPersistentData();

        int phase =
                data.getInt(
                        GULL_PHASE
                );

        AbstractFish carried =
                null;

        for (Entity passenger :
                gull.getPassengers()) {

            if (passenger
                    instanceof AbstractFish fish) {
                carried =
                        fish;

                break;
            }
        }

        if (carried != null) {
            carried.setAirSupply(
                    carried.getMaxAirSupply()
            );

            handleCarriedFish(
                    level,
                    gull,
                    carried,
                    phase,
                    now
            );

            return;
        }

        if (handleSunfishPerch(
                level,
                gull,
                now
        )) {
            return;
        }

        if (phase == PHASE_ASCEND
                || phase == PHASE_LAND
                || phase == PHASE_EAT) {
            resetFlight(
                    gull
            );

            phase =
                    PHASE_CRUISE;
        }

        if (gull.isInWaterOrBubble()
                && phase != PHASE_DIVE) {
            escapeWater(
                    level,
                    gull
            );

            return;
        }

        if (scavengeWhaleCarcass(
                level,
                gull,
                now
        )) {
            return;
        }

        if (now < data.getLong(
                GULL_NEXT_HUNT
        )) {
            return;
        }

        AbstractFish prey =
                nearestSurfacePrey(
                        level,
                        gull
                );

        if (prey == null) {
            data.putInt(
                    GULL_PHASE,
                    PHASE_CRUISE
            );

            data.putLong(
                    GULL_NEXT_HUNT,
                    now
                            + 60L
                            + level.random.nextInt(
                            120
                    )
            );

            return;
        }

        data.putInt(
                GULL_PHASE,
                PHASE_DIVE
        );

        data.putLong(
                GULL_DIVE_UNTIL,
                now + 60L
        );

        gull.setNoGravity(
                true
        );

        Vec3 dive =
                new Vec3(
                        prey.getX(),
                        prey.getY()
                                + 0.18,
                        prey.getZ()
                );

        gull.getMoveControl()
                .setWantedPosition(
                        dive.x,
                        dive.y,
                        dive.z,
                        1.75
                );

        gull.getNavigation()
                .moveTo(
                        dive.x,
                        dive.y,
                        dive.z,
                        1.55
                );

        if (gull.distanceToSqr(
                prey
        ) <= 2.15 * 2.15) {
            capture(
                    level,
                    gull,
                    prey,
                    now
            );

            return;
        }

        if (now >= data.getLong(
                GULL_DIVE_UNTIL
        )) {
            data.putInt(
                    GULL_PHASE,
                    PHASE_CRUISE
            );

            data.putLong(
                    GULL_NEXT_HUNT,
                    now + 100L
            );

            escapeWater(
                    level,
                    gull
            );
        }
    }

    private static boolean handleSunfishPerch(
            ServerLevel level,
            SeagullEntity gull,
            long now
    ) {
        var data =
                gull.getPersistentData();

        if (data.hasUUID(
                GULL_SUNFISH_TARGET
        )) {
            var target =
                    level.getEntity(
                            data.getUUID(
                                    GULL_SUNFISH_TARGET
                            )
                    );

            if (!(target instanceof SunfishEntity sunfish)
                    || !sunfish.isAlive()
                    || !sunfish.isBasking()) {
                clearSunfishPerch(
                        gull,
                        now
                );

                return false;
            }

            if (now >= data.getLong(
                    GULL_SUNFISH_LEAVE_AT
            )) {
                clearSunfishPerch(
                        gull,
                        now
                );

                gull.setDeltaMovement(
                        gull.getDeltaMovement()
                                .add(
                                        0.0,
                                        0.24,
                                        0.0
                                )
                );

                return true;
            }

            double tx =
                    sunfish.getX();

            double ty =
                    sunfish.baskingBackY()
                            + 0.03;

            double tz =
                    sunfish.getZ();

            Vec3 delta =
                    new Vec3(
                            tx - gull.getX(),
                            ty - gull.getY(),
                            tz - gull.getZ()
                    );

            if (delta.lengthSqr()
                    > 0.75 * 0.75) {
                gull.setNoGravity(
                        true
                );

                gull.getMoveControl()
                        .setWantedPosition(
                                tx,
                                ty,
                                tz,
                                1.05
                        );

                gull.getNavigation()
                        .moveTo(
                                tx,
                                ty,
                                tz,
                                0.92
                        );

                if (delta.lengthSqr()
                        < 3.0 * 3.0) {
                    gull.setDeltaMovement(
                            gull.getDeltaMovement()
                                    .scale(
                                            0.72
                                    )
                                    .add(
                                            delta.normalize()
                                                    .scale(
                                                            0.08
                                                    )
                                    )
                    );
                }

                return true;
            }

            /*
             * Treat the sideways sunfish like a tiny moving island. Locking
             * the gull's feet to the back means it never shoves the fish while
             * perched, even though side collisions remain physical.
             */
            gull.setNoGravity(
                    true
            );

            gull.getNavigation()
                    .stop();

            gull.setPos(
                    tx,
                    ty,
                    tz
            );

            gull.setDeltaMovement(
                    Vec3.ZERO
            );

            if (now >= data.getLong(
                    GULL_SUNFISH_PECK_AT
            )) {
                ItemStack snack =
                        new ItemStack(
                                net.caravidro.wayaround.ecology.EcologyContent.RAW_SUNFISH_MEAT.get()
                        );

                level.playSound(
                        null,
                        gull.blockPosition(),
                        SoundEvents.GENERIC_EAT,
                        SoundSource.NEUTRAL,
                        0.72F,
                        1.35F
                                + level.random.nextFloat()
                                        * 0.20F
                );

                level.sendParticles(
                        new ItemParticleOption(
                                ParticleTypes.ITEM,
                                snack
                        ),
                        gull.getX(),
                        gull.getY()
                                + 0.05,
                        gull.getZ(),
                        5,
                        0.10,
                        0.05,
                        0.10,
                        0.015
                );

                if (level.random.nextFloat()
                        < 0.34F) {
                    sunfish.addScarFromPeck();

                    level.playSound(
                            null,
                            sunfish.blockPosition(),
                            SoundEvents.FOX_BITE,
                            SoundSource.NEUTRAL,
                            0.42F,
                            1.42F
                    );
                }

                /*
                 * One little feeding/pecking visit is enough. The gull lingers
                 * for a moment afterward and then leaves.
                 */
                data.putLong(
                        GULL_SUNFISH_PECK_AT,
                        Long.MAX_VALUE
                );

                data.putLong(
                        GULL_SUNFISH_LEAVE_AT,
                        Math.min(
                                data.getLong(
                                        GULL_SUNFISH_LEAVE_AT
                                ),
                                now
                                        + 45L
                                        + level.random.nextInt(
                                        55
                                )
                        )
                );
            }

            return true;
        }

        if (now < data.getLong(
                GULL_NEXT_HUNT
        )
                || level.random.nextFloat()
                        > 0.012F) {
            return false;
        }

        SunfishEntity sunfish =
                nearest(
                        gull,
                        level.getEntitiesOfClass(
                                SunfishEntity.class,
                                gull.getBoundingBox()
                                        .inflate(
                                                28.0,
                                                14.0,
                                                28.0
                                        ),
                                fish ->
                                        fish.isAlive()
                                                && fish.isBasking()
                                                && fish.isInWaterOrBubble()
                        )
                );

        if (sunfish == null) {
            return false;
        }

        data.putUUID(
                GULL_SUNFISH_TARGET,
                sunfish.getUUID()
        );

        data.putLong(
                GULL_SUNFISH_PECK_AT,
                now
                        + 45L
                        + level.random.nextInt(
                        80
                )
        );

        data.putLong(
                GULL_SUNFISH_LEAVE_AT,
                now
                        + 150L
                        + level.random.nextInt(
                        190
                )
        );

        gull.setNoGravity(
                true
        );

        return true;
    }

    private static void clearSunfishPerch(
            SeagullEntity gull,
            long now
    ) {
        var data =
                gull.getPersistentData();

        data.remove(
                GULL_SUNFISH_TARGET
        );

        data.remove(
                GULL_SUNFISH_PECK_AT
        );

        data.remove(
                GULL_SUNFISH_LEAVE_AT
        );

        data.putLong(
                GULL_NEXT_HUNT,
                now
                        + 220L
                        + gull.getRandom()
                                .nextInt(
                                        360
                                )
        );

        resetFlight(
                gull
        );
    }

    private static void capture(
            ServerLevel level,
            SeagullEntity gull,
            AbstractFish prey,
            long now
    ) {
        if (!prey.startRiding(
                gull,
                true
        )) {
            return;
        }

        prey.setAirSupply(
                prey.getMaxAirSupply()
        );

        gull.getPersistentData()
                .putInt(
                        GULL_PHASE,
                        PHASE_ASCEND
                );

        gull.getPersistentData()
                .putLong(
                        GULL_EAT_AT,
                        now + 260L
                );

        gull.setNoGravity(
                true
        );

        gull.setDeltaMovement(
                gull.getDeltaMovement()
                        .add(
                                0.0,
                                0.32,
                                0.0
                        )
        );

        level.playSound(
                null,
                gull.blockPosition(),
                SoundEvents.CHICKEN_AMBIENT,
                SoundSource.NEUTRAL,
                0.92F,
                1.34F
        );

        level.sendParticles(
                ParticleTypes.SPLASH,
                prey.getX(),
                prey.getY(),
                prey.getZ(),
                16,
                0.30,
                0.18,
                0.30,
                0.10
        );
    }

    private static void handleCarriedFish(
            ServerLevel level,
            SeagullEntity gull,
            AbstractFish prey,
            int phase,
            long now
    ) {
        var data =
                gull.getPersistentData();

        if (phase == PHASE_ASCEND) {
            gull.setNoGravity(
                    true
            );

            double comfortY =
                    Math.max(
                            level.getSeaLevel()
                                    + 8.0,
                            gull.getY()
                                    + 4.0
                    );

            gull.getMoveControl()
                    .setWantedPosition(
                            gull.getX()
                                    + gull.getLookAngle().x
                                            * 8.0,
                            comfortY,
                            gull.getZ()
                                    + gull.getLookAngle().z
                                            * 8.0,
                            1.42
                    );

            if (gull.getY()
                    >= level.getSeaLevel()
                            + 6.5) {
                BlockPos landing =
                        findLandingSpot(
                                level,
                                gull.blockPosition()
                        );

                if (landing != null) {
                    data.putLong(
                            GULL_LANDING,
                            landing.asLong()
                    );

                    data.putInt(
                            GULL_PHASE,
                            PHASE_LAND
                    );

                    return;
                }

                /*
                 * Open ocean can genuinely have nowhere sensible to land.
                 * After carrying the prey long enough, hover at a comfortable
                 * altitude and eat it in the air instead of circling forever.
                 */
                if (now >= data.getLong(
                        GULL_EAT_AT
                )) {
                    eatCarriedFish(
                            level,
                            gull,
                            prey,
                            now
                    );

                    return;
                }
            }

            return;
        }

        if (phase == PHASE_LAND) {
            BlockPos landing =
                    BlockPos.of(
                            data.getLong(
                                    GULL_LANDING
                            )
                    );

            double tx =
                    landing.getX()
                            + 0.5;
            double ty =
                    landing.getY()
                            + 0.85;
            double tz =
                    landing.getZ()
                            + 0.5;

            double horizontal =
                    Math.hypot(
                            gull.getX() - tx,
                            gull.getZ() - tz
                    );

            if (horizontal > 1.5
                    || gull.getY()
                    > ty + 1.8) {
                gull.setNoGravity(
                        true
                );

                gull.getMoveControl()
                        .setWantedPosition(
                                tx,
                                ty,
                                tz,
                                1.12
                        );

                gull.getNavigation()
                        .moveTo(
                                tx,
                                ty,
                                tz,
                                1.02
                        );

                return;
            }

            gull.setNoGravity(
                    false
            );

            gull.getNavigation()
                    .stop();

            gull.setDeltaMovement(
                    0.0,
                    -0.10,
                    0.0
            );

            if (gull.onGround()
                    || gull.getY()
                    <= ty + 0.25) {
                data.putInt(
                        GULL_PHASE,
                        PHASE_EAT
                );

                data.putLong(
                        GULL_EAT_AT,
                        now + 40L
                );
            }

            return;
        }

        if (phase == PHASE_EAT) {
            gull.setNoGravity(
                    false
            );

            gull.getNavigation()
                    .stop();

            if (now >= data.getLong(
                    GULL_EAT_AT
            )) {
                eatCarriedFish(
                        level,
                        gull,
                        prey,
                        now
                );
            }

            return;
        }

        data.putInt(
                GULL_PHASE,
                PHASE_ASCEND
        );
    }

    private static boolean scavengeWhaleCarcass(
            ServerLevel level,
            SeagullEntity gull,
            long now
    ) {
        WhaleCarcassEntity carcass =
                nearest(
                        gull,
                        level.getEntitiesOfClass(
                                WhaleCarcassEntity.class,
                                gull.getBoundingBox()
                                        .inflate(
                                                30.0,
                                                18.0,
                                                30.0
                                        ),
                                body ->
                                        body.isAlive()
                                                && !body.isSkeleton()
                        )
                );

        if (carcass == null) {
            return false;
        }

        gull.setNoGravity(
                true
        );

        /*
         * Do not aim at the carcass center. The rendered body is intentionally
         * much larger than its small collision core so it can beach partly
         * inside terrain. Gulls pick an accessible edge based on their current
         * approach direction.
         */
        Vec3 side =
                gull.position()
                        .subtract(
                                carcass.position()
                        )
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (side.lengthSqr()
                < 0.001) {
            side =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            side =
                    side.normalize();
        }

        double edge =
                carcass.getType()
                        == net.caravidro.wayaround.ecology.EcologyContent.SPERM_WHALE_CARCASS.get()
                        ? 1.85
                        : 1.25;

        Vec3 feedingPoint =
                carcass.position()
                        .add(
                                side.scale(
                                        edge
                                )
                        )
                        .add(
                                0.0,
                                0.72,
                                0.0
                        );

        gull.getMoveControl()
                .setWantedPosition(
                        feedingPoint.x,
                        feedingPoint.y,
                        feedingPoint.z,
                        1.18
                );

        if (gull.position()
                .distanceToSqr(
                        feedingPoint
                )
                > 1.35 * 1.35) {
            return true;
        }

        if (now < gull.getPersistentData()
                .getLong(
                        GULL_NEXT_HUNT
                )) {
            return true;
        }

        int consumed =
                carcass.consumeFlesh(
                        2
                                + level.random.nextInt(
                                3
                        )
                );

        if (consumed <= 0) {
            return false;
        }

        ItemStack flesh =
                new ItemStack(
                        net.caravidro.wayaround.ecology.EcologyContent.RAW_WHALE_MEAT.get()
                );

        level.playSound(
                null,
                gull.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.72F,
                1.22F
                        + level.random.nextFloat()
                                * 0.16F
        );

        level.sendParticles(
                new ItemParticleOption(
                        ParticleTypes.ITEM,
                        flesh
                ),
                carcass.getX(),
                carcass.getY()
                        + carcass.getBbHeight()
                                * 0.70,
                carcass.getZ(),
                8
                        + consumed * 2,
                0.42,
                0.18,
                0.42,
                0.04
        );

        level.sendParticles(
                ParticleTypes.POOF,
                carcass.getX(),
                carcass.getY()
                        + carcass.getBbHeight()
                                * 0.68,
                carcass.getZ(),
                5,
                0.35,
                0.14,
                0.35,
                0.015
        );

        gull.getPersistentData()
                .putLong(
                        GULL_NEXT_HUNT,
                        now
                                + 80L
                                + level.random.nextInt(
                                100
                        )
                );

        return true;
    }

    private static AbstractFish nearestSurfacePrey(
            ServerLevel level,
            SeagullEntity gull
    ) {
        return nearest(
                gull,
                level.getEntitiesOfClass(
                        AbstractFish.class,
                        gull.getBoundingBox()
                                .inflate(
                                        20.0,
                                        14.0,
                                        20.0
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
                                ) <= MAX_GULL_PREY_SIZE
                                        && nearSurface(
                                        level,
                                        fish.blockPosition()
                                )
                )
        );
    }

    private static void eatCarriedFish(
            ServerLevel level,
            SeagullEntity gull,
            AbstractFish prey,
            long now
    ) {
        ItemStack meat =
                LivingFaunaManager.meatForFish(
                        prey
                );

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
                new ItemParticleOption(
                        ParticleTypes.ITEM,
                        meat
                ),
                gull.getX(),
                gull.getY()
                        + 0.12,
                gull.getZ(),
                12,
                0.22,
                0.16,
                0.22,
                0.045
        );

        level.sendParticles(
                ParticleTypes.POOF,
                gull.getX(),
                gull.getY(),
                gull.getZ(),
                7,
                0.22,
                0.14,
                0.22,
                0.02
        );

        prey.discard();

        resetFlight(
                gull
        );

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

    private static void resetFlight(
            SeagullEntity gull
    ) {
        gull.getPersistentData()
                .putInt(
                        GULL_PHASE,
                        PHASE_CRUISE
                );

        gull.setNoGravity(
                true
        );
    }

    private static void escapeWater(
            ServerLevel level,
            SeagullEntity gull
    ) {
        gull.setNoGravity(
                true
        );

        gull.setAirSupply(
                gull.getMaxAirSupply()
        );

        double targetY =
                Math.max(
                        level.getSeaLevel()
                                + 5.0,
                        gull.getY()
                                + 5.0
                );

        gull.getMoveControl()
                .setWantedPosition(
                        gull.getX()
                                + gull.getLookAngle().x
                                        * 4.0,
                        targetY,
                        gull.getZ()
                                + gull.getLookAngle().z
                                        * 4.0,
                        1.45
                );

        gull.setDeltaMovement(
                gull.getDeltaMovement()
                        .add(
                                0.0,
                                0.18,
                                0.0
                        )
        );
    }

    private static BlockPos findLandingSpot(
            ServerLevel level,
            BlockPos origin
    ) {
        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int radius = 4;
             radius <= 36;
             radius += 4) {
            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {
                for (int side = -1;
                     side <= 1;
                     side++) {
                    Direction sideDirection =
                            direction.getClockWise();

                    int x =
                            origin.getX()
                                    + direction.getStepX()
                                            * radius
                                    + sideDirection.getStepX()
                                            * side
                                            * 3;

                    int z =
                            origin.getZ()
                                    + direction.getStepZ()
                                            * radius
                                    + sideDirection.getStepZ()
                                            * side
                                            * 3;

                    int y =
                            level.getHeight(
                                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    x,
                                    z
                            );

                    BlockPos spot =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    if (!level.getFluidState(
                            spot
                    ).isEmpty()
                            || !level.getFluidState(
                            spot.above()
                    ).isEmpty()) {
                        continue;
                    }

                    if (level.getBlockState(
                            spot.below()
                    ).is(
                            Blocks.WATER
                    )) {
                        continue;
                    }

                    if (!level.getBlockState(
                            spot.below()
                    ).isFaceSturdy(
                            level,
                            spot.below(),
                            Direction.UP
                    )) {
                        continue;
                    }

                    double distance =
                            spot.distSqr(
                                    origin
                            );

                    if (distance
                            < bestDistance) {
                        bestDistance =
                                distance;
                        best =
                                spot;
                    }
                }
            }

            if (best != null) {
                break;
            }
        }

        return best;
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
