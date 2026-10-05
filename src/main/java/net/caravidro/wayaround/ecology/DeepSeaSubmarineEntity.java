package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.flow.UniversalFlow;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Powered exploration submarine with saved physical cargo. */
public final class DeepSeaSubmarineEntity extends AbyssVehicleEntity implements HasCustomInventoryScreen {
    private float throttle, steering, vertical, turnSpeed;
    private long lastInputTick;
    private final SimpleContainer cargo = new SimpleContainer(27) {
        @Override public boolean stillValid(Player player) { return isAlive() && player.distanceToSqr(DeepSeaSubmarineEntity.this)<64; }
    };
    public DeepSeaSubmarineEntity(EntityType<? extends DeepSeaSubmarineEntity> type, Level level) { super(type,level); }
    @Override protected boolean capsule() { return false; }
    public SimpleContainer cargo() { return cargo; }
    public void setControls(float throttle,float steering,float vertical) {
        this.throttle=Mth.clamp(throttle,-1,1);this.steering=Mth.clamp(steering,-1,1);this.vertical=Mth.clamp(vertical,-1,1);lastInputTick=level().getGameTime();
    }
    @Override public void tick() {
        super.tick();if(level().isClientSide || !isAlive())return;
        Entity pilot=getFirstPassenger();
        if(pilot instanceof Player player) player.setAirSupply(player.getMaxAirSupply());
        if(pilot==null || level().getGameTime()-lastInputTick>8) throttle=steering=vertical=0;
        if(!inWaterColumn()) {
            Vec3 motion=getDeltaMovement().add(0,-.055,0);move(MoverType.SELF,motion);setDeltaMovement(motion.multiply(.82,.88,.82));return;
        }
        turnSpeed=Mth.lerp(.12F,turnSpeed,steering*2.1F);setYRot(getYRot()+turnSpeed);
        if(pilot!=null)setXRot(Mth.lerp(.15F,getXRot(),Mth.clamp(pilot.getXRot(),-70,70)));
        Vec3 forward=Vec3.directionFromRotation(0,getYRot());
        Vec3 target=forward.scale(throttle*.20).add(0,vertical*.14,0).add(UniversalFlow.waterAround(level(),blockPosition()).velocityPerTick().scale(.035));
        Vec3 motion=constrainAscent(getDeltaMovement().scale(.86).add(target.scale(.14)));
        move(MoverType.SELF,motion);impact(motion);setDeltaMovement(new Vec3(horizontalCollision?0:motion.x,verticalCollision?0:motion.y,horizontalCollision?0:motion.z).scale(.96));
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return super.getPassengerRidingPosition(passenger).add(0,-1.18,0); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if(player.isSecondaryUseActive()) {
            if(!level().isClientSide) {
                if(player.getItemInHand(hand).is(Items.STICK) && !isVehicle() && cargo.isEmpty() && pressureExposure()==0) { spawnAtLocation(EcologyContent.DEEP_SEA_SUBMARINE_ITEM.get());discard(); }
                else openCustomInventoryScreen(player);
            }
        } else if(!level().isClientSide && !player.isPassenger()) player.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void openCustomInventoryScreen(Player player) {
        if(!level().isClientSide && cargo.stillValid(player)) player.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.threeRows(id,inventory,cargo),Component.translatable("container.wayaround.submarine_cargo")));
    }
    @Override protected void dropCargo() { Containers.dropContents(level(),this,cargo);cargo.clearContent(); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);setYRot(tag.getFloat("SubmarineYaw"));
        NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registryAccess());
        for(int i=0;i<27;i++)cargo.setItem(i,items.get(i));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putFloat("SubmarineYaw",getYRot());
        NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)items.set(i,cargo.getItem(i));ContainerHelper.saveAllItems(tag,items,registryAccess());
    }
}
