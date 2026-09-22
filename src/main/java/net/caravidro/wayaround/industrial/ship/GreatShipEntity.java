package net.caravidro.wayaround.industrial.ship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GreatShipEntity extends SailingShipEntity {
    private static final List<EntityDataAccessor<Optional<UUID>>> SEATS = java.util.stream.IntStream.range(0, 6)
            .mapToObj(i -> SynchedEntityData.defineId(GreatShipEntity.class, EntityDataSerializers.OPTIONAL_UUID)).toList();
    private static final Vec3[] POSITIONS = {
        new Vec3(0, 1.2, -2.7), new Vec3(-1.2, .85, -1.5), new Vec3(1.2, .85, -1.5),
        new Vec3(-1.2, .85, .7), new Vec3(1.2, .85, .7), new Vec3(0, .85, 2.5)
    };
    private NonNullList<ItemStack> cargo = NonNullList.withSize(54, ItemStack.EMPTY);
    private UUID sleeper;
    public GreatShipEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.SPRUCE);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        SEATS.forEach(key -> builder.define(key, Optional.empty()));
    }
    @Override public Item getDropItem() { return CoalShipContent.GREAT_SHIP_ITEM.get(); }
    @Override protected int getMaxPassengers() { return 6; }
    @Override protected boolean canAddPassenger(Entity entity) {
        return entity instanceof Player && super.canAddPassenger(entity)
                && (sleeper == null || getPassengers().size() < 5 || entity.getUUID().equals(sleeper));
    }
    private int seatOf(Entity entity) {
        for (int i = 0; i < 6; i++) if (entityData.get(SEATS.get(i)).filter(entity.getUUID()::equals).isPresent()) return i;
        return -1;
    }
    @Override protected void addPassenger(Entity entity) {
        super.addPassenger(entity);
        if (!level().isClientSide && seatOf(entity) < 0) {
            for (var key : SEATS) if (entityData.get(key).isEmpty()) {
                entityData.set(key, Optional.of(entity.getUUID()));
                break;
            }
        }
    }
    @Override protected void removePassenger(Entity entity) {
        super.removePassenger(entity);
        if (!level().isClientSide) for (var key : SEATS) {
            if (entityData.get(key).filter(entity.getUUID()::equals).isPresent()) entityData.set(key, Optional.empty());
        }
    }
    public void board(Player player, int seat) {
        if (!canUse(player) || seat < 0 || seat >= 6) return;
        if (entityData.get(SEATS.get(seat)).filter(id -> !id.equals(player.getUUID())).isPresent()) {
            player.displayClientMessage(Component.translatable("message.wayaround.ship.seat_taken"), true);
            return;
        }
        if (player.getVehicle() != this && !player.startRiding(this)) return;
        for (var key : SEATS) if (entityData.get(key).filter(player.getUUID()::equals).isPresent()) {
            entityData.set(key, Optional.empty());
        }
        entityData.set(SEATS.get(seat), Optional.of(player.getUUID()));
        player.closeContainer();
    }
    @Override public LivingEntity getControllingPassenger() {
        return getPassengers().stream().filter(p -> seatOf(p) == 0 && p instanceof LivingEntity)
                .map(p -> (LivingEntity) p).findFirst().orElse(null);
    }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int seat = seatOf(passenger);
        return POSITIONS[Math.max(0, seat)].yRot(-getYRot() * (float) Math.PI / 180.0F);
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        openControls(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean hurt(DamageSource source, float amount) { return super.hurt(source, amount * .08F); }
    @Override public void setDeltaMovement(Vec3 velocity) {
        double speed = velocity.horizontalDistance();
        if (speed > .1) velocity = new Vec3(velocity.x * .1 / speed, velocity.y, velocity.z * .1 / speed);
        super.setDeltaMovement(velocity);
    }
    @Override public int getContainerSize() { return 54; }
    @Override public NonNullList<ItemStack> getItemStacks() { return cargo; }
    @Override public void clearItemStacks() { cargo = NonNullList.withSize(54, ItemStack.EMPTY); }
    @Override public boolean stillValid(Player player) { return canUse(player); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        unpackLootTable(player);
        return ChestMenu.sixRows(id, inventory, this);
    }
    public void openWorkbench(Player player) {
        if (!canUse(player)) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                new CraftingMenu(id, inventory, ContainerLevelAccess.create(level(), blockPosition())) {
                    @Override public boolean stillValid(Player p) { return canUse(p); }
                }, Component.translatable("container.crafting")));
    }
    public Vec3 berthPosition() {
        return new Vec3(.8, 1.1, -2.4).yRot(-getYRot() * (float) Math.PI / 180.0F).add(position());
    }
    public boolean ownsSleeper(Player player) { return player.getUUID().equals(sleeper); }
    @Override public boolean hasSleeper() { return sleeper != null; }
    public void sleep(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canUse(player)) return;
        if (!isAnchored() || sleeper != null || (getPassengers().size() >= 6 && !hasPassenger(player))) {
            player.displayClientMessage(Component.translatable("message.wayaround.ship.bed_anchor"), true);
            return;
        }
        if (!level().dimensionType().natural() || level().isDay()) {
            player.displayClientMessage(Component.translatable("sleep.not_possible"), true);
            return;
        }
        if (!level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(8, 5, 8),
                monster -> monster.isPreventingPlayerRest(player)).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.not_safe"), true);
            return;
        }
        BlockPos bed = BlockPos.containing(berthPosition());
        if (!level().getBlockState(bed).isAir() || !level().getBlockState(bed.above()).isAir()) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.obstructed"), true);
            return;
        }
        player.closeContainer();
        sleeper = player.getUUID();
        player.getPersistentData().putUUID("WayAroundShipBerth", getUUID());
        var result = serverPlayer.startSleepInBed(bed);
        result.ifLeft(problem -> {
            sleeper = null;
            player.getPersistentData().remove("WayAroundShipBerth");
            if (problem.getMessage() != null) player.displayClientMessage(problem.getMessage(), true);
        });
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 20) {
            for (var key : SEATS) {
                Optional<UUID> occupant = entityData.get(key);
                if (occupant.isPresent() && getPassengers().stream().noneMatch(p -> p.getUUID().equals(occupant.get()))) {
                    entityData.set(key, Optional.empty());
                }
            }
        }
        if (!level().isClientSide && sleeper != null) {
            Player player = level().getPlayerByUUID(sleeper);
            if (player == null || !player.isSleeping()) {
                sleeper = null;
                if (player != null) {
                    player.getPersistentData().remove("WayAroundShipBerth");
                    if (canUse(player)) player.startRiding(this);
                }
            }
        }
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && sleeper != null) {
            Player player = level().getPlayerByUUID(sleeper);
            if (player != null) {
                player.stopSleeping();
                player.getPersistentData().remove("WayAroundShipBerth");
            }
            sleeper = null;
        }
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (sleeper != null) tag.putUUID("Sleeper", sleeper);
        for (int i = 0; i < 6; i++) {
            Optional<UUID> occupant = entityData.get(SEATS.get(i));
            if (occupant.isPresent()) tag.putUUID("Seat" + i, occupant.get());
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sleeper = tag.hasUUID("Sleeper") ? tag.getUUID("Sleeper") : null;
        for (int i = 0; i < 6; i++) entityData.set(SEATS.get(i),
                tag.hasUUID("Seat" + i) ? Optional.of(tag.getUUID("Seat" + i)) : Optional.empty());
    }
}
