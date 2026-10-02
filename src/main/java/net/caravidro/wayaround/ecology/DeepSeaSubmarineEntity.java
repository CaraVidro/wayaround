package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Small powered abyss submarine. Unlike the descent capsule it can actually
 * translate through the water column: W/S drive, A/D steer, jump rises and
 * sprint descends.
 */
public final class DeepSeaSubmarineEntity extends Entity {

    private float throttle;
    private float steering;
    private float vertical;
    private long lastInputTick;

    /*
     * These are real vanilla LIGHT blocks, moved with the submarine. This is
     * deliberately server-owned: blocks, mobs, fish and terrain receive actual
     * block-light values instead of a client-only fullbright illusion.
     */
    private Set<BlockPos> headlightBlocks =
            new HashSet<>();

    private Set<BlockPos> nextHeadlightBlocks =
            new HashSet<>();

    private static final int HEADLIGHT_REFRESH_TICKS =
            3;

    private static final double HEADLIGHT_MAX_DISTANCE =
            42.0;

    private static final double[] HEADLIGHT_SIDES = {
            -0.48,
            0.48
    };

    private static final double[] HEADLIGHT_DISTANCES = {
            0.5,
            4.0,
            8.0,
            12.0,
            17.0,
            22.0,
            28.0,
            35.0,
            41.0
    };

    private static final double[] HEADLIGHT_WINGS = {
            -1.0,
            1.0
    };

    private double lastHeadlightX =
            Double.NaN;

    private double lastHeadlightY =
            Double.NaN;

    private double lastHeadlightZ =
            Double.NaN;

    private float lastHeadlightYaw =
            Float.NaN;

    private float lastHeadlightVertical =
            Float.NaN;

    public DeepSeaSubmarineEntity(
            EntityType<? extends DeepSeaSubmarineEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        blocksBuilding =
                true;
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
    }

    public void setControls(
            float throttle,
            float steering,
            float vertical
    ) {
        this.throttle =
                clamp(
                        throttle
                );

        this.steering =
                clamp(
                        steering
                );

        this.vertical =
                clamp(
                        vertical
                );

        lastInputTick =
                level()
                        .getGameTime();
    }

    @Override
    public void tick() {
        super.tick();

        Entity pilot =
                getFirstPassenger();

        if (pilot != null
                && !level().isClientSide
                && pilot instanceof Player player) {
            player.setAirSupply(
                    player.getMaxAirSupply()
            );
        }

        if (level().isClientSide) {
            return;
        }

        if (pilot == null
                || level().getGameTime()
                - lastInputTick > 8L) {
            throttle =
                    0.0F;

            steering =
                    0.0F;

            vertical =
                    0.0F;
        }

        boolean water =
                isInWater()
                        || level()
                        .getFluidState(
                                blockPosition()
                        )
                        .is(
                                FluidTags.WATER
                        );

        if (!water) {
            clearHeadlights();

            Vec3 motion =
                    getDeltaMovement()
                            .add(
                                    0.0,
                                    -0.055,
                                    0.0
                            );

            move(
                    MoverType.SELF,
                    motion
            );

            setDeltaMovement(
                    motion.multiply(
                            0.82,
                            0.88,
                            0.82
                    )
            );

            return;
        }

        setYRot(
                getYRot()
                        + steering
                        * 2.65F
        );

        Vec3 forward =
                Vec3.directionFromRotation(
                        0.0F,
                        getYRot()
                );

        Vec3 current =
                WaterDynamics.currentAround(
                        level(),
                        blockPosition()
                );

        Vec3 target =
                forward.scale(
                        throttle
                                * 0.20
                )
                        .add(
                                0.0,
                                vertical
                                        * 0.14,
                                0.0
                        )
                        .add(
                                current.scale(
                                        0.035
                                )
                        );

        Vec3 motion =
                getDeltaMovement()
                        .scale(
                                0.72
                        )
                        .add(
                                target.scale(
                                        0.28
                                )
                        );

        if (pilot == null) {
            motion =
                    motion.add(
                            0.0,
                            0.008,
                            0.0
                    );
        }

        move(
                MoverType.SELF,
                motion
        );

        setDeltaMovement(
                motion.scale(
                        0.94
                )
        );

        if (tickCount
                % HEADLIGHT_REFRESH_TICKS
                == 0) {
            updateHeadlights();
        }
    }

    /**
     * Places a sparse chain of invisible vanilla light blocks through the
     * water in front of both physical lamps. Because every node is an actual
     * light source, Minecraft's light engine illuminates blocks/entities too.
     */
    private void updateHeadlights() {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.SUBMARINE_LIGHTING
                );

        try {
        if (level().isClientSide) {
            return;
        }

        double dx =
                getX()
                        - lastHeadlightX;

        double dy =
                getY()
                        - lastHeadlightY;

        double dz =
                getZ()
                        - lastHeadlightZ;

        float yawDelta =
                Float.isFinite(
                        lastHeadlightYaw
                )
                        ? Math.abs(
                        net.minecraft.util.Mth.wrapDegrees(
                                getYRot()
                                        - lastHeadlightYaw
                        )
                )
                        : Float.POSITIVE_INFINITY;

        /*
         * A stationary submarine used to rewrite ~20-50 LIGHT blocks every
         * three ticks, forcing repeated light-engine updates even though the
         * beam had not moved. Preserve the real light nodes until position,
         * yaw or pitch control actually changes enough to matter.
         */
        if (Double.isFinite(
                lastHeadlightX
        )
                && dx * dx
                        + dy * dy
                        + dz * dz
                        < 0.12 * 0.12
                && yawDelta < 1.4F
                && Math.abs(
                vertical
                        - lastHeadlightVertical
        ) < 0.08F) {
            return;
        }

        lastHeadlightX =
                getX();

        lastHeadlightY =
                getY();

        lastHeadlightZ =
                getZ();

        lastHeadlightYaw =
                getYRot();

        lastHeadlightVertical =
                vertical;

        Vec3 forward =
                Vec3.directionFromRotation(
                        0.0F,
                        getYRot()
                )
                        .normalize();

        Vec3 right =
                new Vec3(
                        forward.z,
                        0.0,
                        -forward.x
                )
                        .normalize();

        /*
         * Descending nudges the beams downward; rising lifts them slightly.
         * The hull itself does not visually pitch yet, so keep this subtle.
         */
        Vec3 beam =
                new Vec3(
                        forward.x,
                        vertical * 0.12
                                - 0.025,
                        forward.z
                )
                        .normalize();

        Set<BlockPos> next =
                nextHeadlightBlocks;

        next.clear();

        for (double side :
                HEADLIGHT_SIDES) {

            Vec3 origin =
                    position()
                            .add(
                                    0.0,
                                    1.10,
                                    0.0
                            )
                            .add(
                                    forward.scale(
                                            1.35
                                    )
                            )
                            .add(
                                    right.scale(
                                            side
                                    )
                            );

            double clearDistance =
                    clearBeamDistance(
                            origin,
                            beam
                    );

            /*
             * Spacing stays below vanilla's 15-block falloff radius, so the
             * nodes merge into one continuous shaft of real illumination.
             */
            for (int index = 0;
                 index < HEADLIGHT_DISTANCES.length;
                 index++) {

                double distance =
                        HEADLIGHT_DISTANCES[index];

                if (distance
                        > clearDistance) {
                    break;
                }

                Vec3 point =
                        origin.add(
                                beam.scale(
                                        distance
                                )
                        );

                BlockPos pos =
                        BlockPos.containing(
                                point
                        );

                int lightLevel =
                        Math.max(
                                10,
                                15
                                        - index
                                        / 2
                        );

                if (placeHeadlightBlock(
                        pos,
                        lightLevel
                )) {
                    next.add(
                            pos.immutable()
                    );
                }

                /*
                 * Widen the far part of the beam into a modest cone rather
                 * than a single laser line.
                 */
                if (distance >= 17.0) {
                    double spread =
                            Math.min(
                                    1.75,
                                    (
                                            distance - 12.0
                                    )
                                            * 0.055
                            );

                    for (double wingSign :
                            HEADLIGHT_WINGS) {

                        BlockPos wingPos =
                                BlockPos.containing(
                                        point.add(
                                                right.scale(
                                                        wingSign
                                                                * spread
                                                )
                                        )
                                );

                        if (placeHeadlightBlock(
                                wingPos,
                                Math.max(
                                        9,
                                        lightLevel - 1
                                )
                        )) {
                            next.add(
                                    wingPos.immutable()
                            );
                        }
                    }
                }
            }
        }

        for (BlockPos old :
                headlightBlocks) {

            if (!next.contains(
                    old
            )) {
                removeHeadlightBlock(
                        old
                );
            }
        }

        /*
         * Swap the two reusable sets. No per-refresh HashSet, no Set.copyOf,
         * and no second addAll pass.
         */
        Set<BlockPos> previous =
                headlightBlocks;

        headlightBlocks =
                next;

        nextHeadlightBlocks =
                previous;
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.SUBMARINE_LIGHTING,
                    wayperfStartedAt
            );
        }
    }

    private double clearBeamDistance(
            Vec3 origin,
            Vec3 beam
    ) {
        Vec3 end =
                origin.add(
                        beam.scale(
                                HEADLIGHT_MAX_DISTANCE
                        )
                );

        BlockHitResult hit =
                level().clip(
                        new ClipContext(
                                origin,
                                end,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                this
                        )
                );

        if (hit.getType()
                == HitResult.Type.MISS) {
            return HEADLIGHT_MAX_DISTANCE;
        }

        return Math.max(
                0.0,
                origin.distanceTo(
                        hit.getLocation()
                )
                        - 0.75
        );
    }

    private boolean placeHeadlightBlock(
            BlockPos pos,
            int lightLevel
    ) {
        if (!level().isLoaded(
                pos
        )) {
            return false;
        }

        BlockState current =
                level().getBlockState(
                        pos
                );

        boolean existingLight =
                current.is(
                        Blocks.LIGHT
                );

        boolean water =
                current.is(
                        Blocks.WATER
                )
                        || (
                        existingLight
                                && current.hasProperty(
                                LightBlock.WATERLOGGED
                        )
                                && current.getValue(
                                LightBlock.WATERLOGGED
                        )
                );

        if (!existingLight
                && !current.isAir()
                && !water) {
            return false;
        }

        BlockState light =
                Blocks.LIGHT
                        .defaultBlockState()
                        .setValue(
                                LightBlock.LEVEL,
                                Math.max(
                                        0,
                                        Math.min(
                                                15,
                                                lightLevel
                                        )
                                )
                        );

        if (light.hasProperty(
                LightBlock.WATERLOGGED
        )) {
            light =
                    light.setValue(
                            LightBlock.WATERLOGGED,
                            water
                    );
        }

        if (current == light) {
            return true;
        }

        return level().setBlock(
                pos,
                light,
                3
        );
    }

    private void clearHeadlights() {
        if (level().isClientSide
                || headlightBlocks.isEmpty()) {
            return;
        }

        for (BlockPos pos :
                headlightBlocks) {
            removeHeadlightBlock(
                    pos
            );
        }

        headlightBlocks.clear();

        nextHeadlightBlocks.clear();

        lastHeadlightX =
                Double.NaN;

        lastHeadlightY =
                Double.NaN;

        lastHeadlightZ =
                Double.NaN;

        lastHeadlightYaw =
                Float.NaN;

        lastHeadlightVertical =
                Float.NaN;
    }

    private void removeHeadlightBlock(
            BlockPos pos
    ) {
        if (!level().isLoaded(
                pos
        )) {
            return;
        }

        BlockState state =
                level().getBlockState(
                        pos
                );

        if (!state.is(
                Blocks.LIGHT
        )) {
            return;
        }

        boolean waterlogged =
                state.hasProperty(
                        LightBlock.WATERLOGGED
                )
                        && state.getValue(
                        LightBlock.WATERLOGGED
                );

        level().setBlock(
                pos,
                waterlogged
                        ? Blocks.WATER
                        .defaultBlockState()
                        : Blocks.AIR
                        .defaultBlockState(),
                3
        );
    }

    @Override
    public void remove(
            RemovalReason reason
    ) {
        clearHeadlights();

        super.remove(
                reason
        );
    }

    @Override
    public Vec3 getPassengerRidingPosition(
            Entity passenger
    ) {
        return super.getPassengerRidingPosition(
                passenger
        ).add(
                0.0,
                -1.18,
                0.0
        );
    }

    @Override
    protected boolean canAddPassenger(
            Entity passenger
    ) {
        return getPassengers()
                .isEmpty()
                && passenger
                instanceof Player;
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide
                    && !isVehicle()) {
                spawnAtLocation(
                        EcologyContent.DEEP_SEA_SUBMARINE_ITEM.get()
                );

                discard();
            }

            return InteractionResult.sidedSuccess(
                    level().isClientSide
            );
        }

        if (!level().isClientSide
                && !player.isPassenger()) {
            player.startRiding(
                    this
            );
        }

        return InteractionResult.sidedSuccess(
                level().isClientSide
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        setYRot(
                tag.getFloat(
                        "SubmarineYaw"
                )
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putFloat(
                "SubmarineYaw",
                getYRot()
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    private static float clamp(
            float value
    ) {
        return Math.max(
                -1.0F,
                Math.min(
                        1.0F,
                        value
                )
        );
    }
}
