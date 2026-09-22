package net.caravidro.wayaround.industrial.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CoalShipEntity extends SailingShipEntity {
    private static final EntityDataAccessor<Integer> FUEL =
            SynchedEntityData.defineId(CoalShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(CoalShipEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean forwardInput;

    public CoalShipEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.SPRUCE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUEL, 0);
        builder.define(RUNNING, false);
    }

    @Override
    public Item getDropItem() {
        return CoalShipContent.COAL_SHIP_ITEM.get();
    }
    public boolean isEngineRunning() {
        return entityData.get(RUNNING);
    }

    @Override
    public void setInput(boolean left, boolean right, boolean forward, boolean backward) {
        super.setInput(left, right, forward, backward);
        forwardInput = forward && !backward;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            // Vanilla's passenger input packet already validates and updates the pilot's forward input.
            boolean forward = getControllingPassenger() instanceof Player pilot && pilot.zza > 0.0F;
            int coalSlot = findCoal();
            CoalShipEngine.Step step = CoalShipEngine.tick(entityData.get(FUEL), coalSlot >= 0,
                    forward && !isAnchored(), hasWaterUnderHull());
            if (step.consumeCoal()) {
                removeItem(coalSlot, 1);
                setChanged();
            }
            entityData.set(FUEL, step.remainingTicks());
            entityData.set(RUNNING, step.running());
        }

        // Preserve vanilla boat control, collision and networking. Only the controlling client simulates
        // steering; the server alone approves the engine and removes coal from the saved cargo inventory.
        if (level().isClientSide && isControlledByLocalInstance() && forwardInput
                && isEngineRunning() && hasWaterUnderHull()) {
            float angle = getYRot() * Mth.DEG_TO_RAD;
            Vec3 velocity = getDeltaMovement().add(-Mth.sin(angle) * CoalShipEngine.ACCELERATION,
                    0.0, Mth.cos(angle) * CoalShipEngine.ACCELERATION);
            double speed = velocity.horizontalDistance();
            if (speed > CoalShipEngine.MAX_HORIZONTAL_SPEED) {
                double factor = CoalShipEngine.MAX_HORIZONTAL_SPEED / speed;
                velocity = new Vec3(velocity.x * factor, velocity.y, velocity.z * factor);
            }
            setDeltaMovement(velocity);
        }

        super.tick();
        if (isEngineRunning()) {
            if (level().isClientSide && tickCount % 4 == 0) {
                Vec3 exhaust = new Vec3(0.0, 1.06, -1.03).yRot(-getYRot() * Mth.DEG_TO_RAD).add(position());
                level().addParticle(ParticleTypes.SMOKE, exhaust.x, exhaust.y, exhaust.z, 0.0, 0.045, 0.0);
            } else if (!level().isClientSide && tickCount % 20 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE,
                        SoundSource.NEUTRAL, 0.35F, 0.7F);
            }
        }
    }

    private boolean hasWaterUnderHull() {
        if (isUnderWater()) {
            return false;
        }
        BlockPos sample = BlockPos.containing(getX(), getBoundingBox().minY - 0.05, getZ());
        return level().getFluidState(sample).is(FluidTags.WATER);
    }

    private int findCoal() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (isCoal(getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isCoal(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isSpectator() && player.isSecondaryUseActive() && isCoal(held)) {
            if (!level().isClientSide) {
                boolean loaded = loadOneCoal(held);
                if (loaded) {
                    held.consume(1, player);
                }
                player.displayClientMessage(Component.translatable(loaded
                        ? "message.wayaround.coal_ship.loaded" : "message.wayaround.coal_ship.full"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player, hand);
    }

    private boolean loadOneCoal(ItemStack held) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack cargo = getItem(slot);
            if (cargo.isEmpty()) {
                setItem(slot, held.copyWithCount(1));
                setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(cargo, held) && cargo.getCount() < cargo.getMaxStackSize()) {
                cargo.grow(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EngineFuel", entityData.get(FUEL));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FUEL, Mth.clamp(tag.getInt("EngineFuel"), 0, CoalShipEngine.TICKS_PER_COAL));
        entityData.set(RUNNING, false);
    }
}
