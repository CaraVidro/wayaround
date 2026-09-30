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

/**
 * Tiny pressure capsule for abyss exploration.
 *
 * It deliberately has no horizontal drive: the pilot can rotate the capsule and
 * its lamp, while W/S command vertical ascent/descent. This keeps the abyss about
 * looking and descending rather than turning it into a submarine racing game.
 */
public final class DeepSeaCapsuleEntity extends Entity {
    private float verticalInput;
    private long lastInputTick;

    public DeepSeaCapsuleEntity(EntityType<? extends DeepSeaCapsuleEntity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public void setVerticalInput(float input) {
        verticalInput = Math.max(-1.0F, Math.min(1.0F, input));
        lastInputTick = level().getGameTime();
    }

    @Override
    public void tick() {
        super.tick();

        Entity pilot = getFirstPassenger();
        if (pilot != null) {
            setYRot(pilot.getYRot());
            setXRot(pilot.getXRot());

            if (!level().isClientSide && pilot instanceof Player player) {
                player.setAirSupply(player.getMaxAirSupply());
            }
        }

        if (!level().isClientSide) {
            if (pilot == null || level().getGameTime() - lastInputTick > 8L) {
                verticalInput = 0.0F;
            }

            double speed = verticalInput * 0.16;
            setDeltaMovement(0.0, speed, 0.0);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(0.0, 0.0, 0.0);
        }
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
