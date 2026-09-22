package net.caravidro.wayaround.industrial.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared water drift, anchor and onboard controls for the mod's vessels. */
public abstract class SailingShipEntity extends ChestBoat {
    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(SailingShipEntity.class, EntityDataSerializers.BOOLEAN);
    private double anchorX;
    private double anchorZ;
    private float anchorYaw;
    public SailingShipEntity(EntityType<? extends Boat> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANCHORED, false);
    }
    public boolean isAnchored() { return entityData.get(ANCHORED); }
    public boolean hasSleeper() { return false; }
    public void toggleAnchor(Player player) {
        if (hasSleeper()) {
            player.displayClientMessage(Component.translatable("message.wayaround.ship.sleeping"), true);
            return;
        }
        boolean anchored = !isAnchored();
        anchorX = getX();
        anchorZ = getZ();
        anchorYaw = getYRot();
        entityData.set(ANCHORED, anchored);
        if (anchored) setDeltaMovement(0, getDeltaMovement().y, 0);
        player.displayClientMessage(Component.translatable(anchored
                ? "message.wayaround.ship.anchored" : "message.wayaround.ship.unanchored"), true);
    }
    @Override public void move(MoverType type, Vec3 movement) {
        super.move(type, isAnchored() ? new Vec3(0, movement.y, 0) : movement);
    }
    @Override public void tick() {
        if (!level().isClientSide && getControllingPassenger() == null && !isAnchored()
                && level().getFluidState(BlockPos.containing(getX(), getY() - 0.05, getZ())).is(FluidTags.WATER)) {
            // Weak prevailing current, independent of fuel and consistent across nearby ships.
            double phase = level().getGameTime() / 2400.0;
            setDeltaMovement(getDeltaMovement().add(0.0012 * Math.cos(phase), 0, 0.0012 * Math.sin(phase)));
        }
        double x = getX();
        double z = getZ();
        float yaw = getYRot();
        super.tick();
        if (isAnchored()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            setPos(level().isClientSide ? x : anchorX, getY(), level().isClientSide ? z : anchorZ);
            setYRot(level().isClientSide ? yaw : anchorYaw);
        }
    }
    public boolean canUse(Player player) {
        return isAlive() && player.level() == level() && !player.isSpectator()
                && (hasPassenger(player) || getBoundingBox().inflate(5).contains(player.position()));
    }
    public void openControls(Player player) {
        if (!level().isClientSide && canUse(player)) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ShipMenu(id, inventory, this),
                    Component.translatable("menu.wayaround.ship")));
        }
    }
    @Override public void openCustomInventoryScreen(Player player) { openControls(player); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            openControls(player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player, hand);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Anchored", isAnchored());
        tag.putDouble("AnchorX", anchorX);
        tag.putDouble("AnchorZ", anchorZ);
        tag.putFloat("AnchorYaw", anchorYaw);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ANCHORED, tag.getBoolean("Anchored"));
        anchorX = tag.getDouble("AnchorX");
        anchorZ = tag.getDouble("AnchorZ");
        anchorYaw = tag.getFloat("AnchorYaw");
    }
}
