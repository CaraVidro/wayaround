package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
