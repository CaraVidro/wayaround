package net.caravidro.wayaround.ecology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tiny pressure capsule for abyss exploration.
 *
 * It deliberately has no horizontal drive: the pilot can rotate the capsule and
 * its lamp, while W/S command vertical ascent/descent. This keeps the abyss about
 * looking and descending rather than turning it into a submarine racing game.
 */
public final class DeepSeaCapsuleEntity extends AbyssVehicleEntity {
    private float verticalInput;
    private long lastInputTick;

    public DeepSeaCapsuleEntity(EntityType<? extends DeepSeaCapsuleEntity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    @Override protected boolean capsule() { return true; }

    public void setVerticalInput(float input) {
        verticalInput = Math.max(-1.0F, Math.min(1.0F, input));
        lastInputTick = level().getGameTime();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide || !isAlive()) return;
        Entity pilot = getFirstPassenger();
        if (pilot != null) {
            setYRot(net.minecraft.util.Mth.rotLerp(.18F, getYRot(), pilot.getYRot()));
            setXRot(net.minecraft.util.Mth.lerp(.18F, getXRot(), pilot.getXRot()));

            if (!level().isClientSide && pilot instanceof Player player) {
                player.setAirSupply(player.getMaxAirSupply());
            }
        }

        if (!level().isClientSide) {
            if (pilot == null || level().getGameTime() - lastInputTick > 8L) {
                verticalInput = 0.0F;
            }

            Vec3 motion = inWaterColumn()
                    ? constrainAscent(getDeltaMovement().scale(.82).add(0, verticalInput*.16*.18, 0))
                    : getDeltaMovement().add(0,-.055,0).multiply(.8,.88,.8);
            move(MoverType.SELF, motion); impact(motion);
            setDeltaMovement(verticalCollision ? Vec3.ZERO : motion);
        }
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        /*
         * 1.21.1 does not expose the newer EntityType.Builder passenger
         * attachment helper. Move the rider down from the vanilla top-of-hitbox
         * mount point into the pressure hull here instead.
         */
        return super.getPassengerRidingPosition(passenger)
                .add(0.0, -1.02, 0.0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide && !isVehicle()) {
                spawnAtLocation(EcologyContent.DEEP_SEA_CAPSULE_ITEM.get());
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && !player.isPassenger()) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
