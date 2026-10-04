package net.caravidro.wayaround.war.outpost;

import java.util.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Server-driven clockwork drone. Its pilot's physical body never follows the camera. */
public final class OutpostDroneEntity extends Entity {
    private static final EntityDataAccessor<Boolean> IMPACT=SynchedEntityData.defineId(OutpostDroneEntity.class,EntityDataSerializers.BOOLEAN),LAUNCHED=SynchedEntityData.defineId(OutpostDroneEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BATTERY=SynchedEntityData.defineId(OutpostDroneEntity.class,EntityDataSerializers.INT);
    private UUID owner;private int battery=6000,flightAge;private Vec3 input=Vec3.ZERO;private long inputAt;
    public OutpostDroneEntity(EntityType<? extends OutpostDroneEntity> t,Level l){super(t,l);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(IMPACT,false);b.define(LAUNCHED,false);b.define(BATTERY,6000);}
    public static boolean enabled(Level l,boolean impact){return WorldFeatureRuntime.enabled(l,impact?WorldFeature.WAR_WITHOUT_REASON:WorldFeature.MEDIA);}
    public void configure(UUID id,boolean impact,int charge){owner=id;entityData.set(IMPACT,impact);battery=Mth.clamp(charge,0,6000);entityData.set(BATTERY,battery);}
    public boolean impact(){return entityData.get(IMPACT);}public boolean launched(){return entityData.get(LAUNCHED);}public int battery(){return entityData.get(BATTERY);}public UUID owner(){return owner;}
    public boolean owned(Player p){return p.getUUID().equals(owner);}
    public boolean controls(ServerPlayer p,int forward,int side,int vertical,float yaw,float pitch){
        if(!owned(p)||launched()||battery<=0||!Float.isFinite(yaw)||!Float.isFinite(pitch)||!OutpostRemote.controls(p,this)||!enabled(level(),impact()))return false;
        setYRot(getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-getYRot()),-20,20));setXRot(Mth.clamp(pitch,-80,80));
        Vec3 f=Vec3.directionFromRotation(0,getYRot()),r=f.cross(new Vec3(0,1,0));input=f.scale(Mth.clamp(forward,-1,1)).add(r.scale(Mth.clamp(side,-1,1))).add(0,Mth.clamp(vertical,-1,1),0);if(input.lengthSqr()>1)input=input.normalize();inputAt=level().getGameTime();return true;
    }
    public void launch(ServerPlayer p){if(!impact()||!owned(p)||!OutpostRemote.controls(p,this)||battery<=0||!enabled(level(),true))return;entityData.set(LAUNCHED,true);setDeltaMovement(Vec3.directionFromRotation(getXRot(),getYRot()).scale(1.15));flightAge=0;OutpostRemote.stop(p);}
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        var stack=p.getItemInHand(hand);if(!owned(p)||launched())return InteractionResult.FAIL;if(level().isClientSide)return InteractionResult.SUCCESS;
        if(stack.is(OutpostContent.CONTROLLER.get())){RemoteControllerItem.bindDrone(stack,this);p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.wayaround.remote.drone"),true);return InteractionResult.CONSUME;}
        if(stack.is(Items.REDSTONE)&&battery<6000){battery=Math.min(6000,battery+1200);entityData.set(BATTERY,battery);if(!p.getAbilities().instabuild)stack.shrink(1);return InteractionResult.CONSUME;}
        if(p.isShiftKeyDown()&&stack.isEmpty()&&!OutpostRemote.controlled(this)){var item=packed();if(!p.addItem(item))p.drop(item,false);discard();return InteractionResult.CONSUME;}return InteractionResult.PASS;
    }
    public ItemStack packed(){var item=new ItemStack(impact()?OutpostContent.IMPACT_DRONE.get():OutpostContent.CAMERA_DRONE.get());var n=new CompoundTag();n.putInt("Battery",battery);item.set(DataComponents.CUSTOM_DATA,CustomData.of(n));return item;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean hurt(DamageSource source,float amount){if(isInvulnerableTo(source)||level().isClientSide)return false;if(launched())detonate();else {spawnAtLocation(new ItemStack(OutpostContent.CHASSIS.get()));discard();}return true;}
    public void detonate(){if(level().isClientSide||isRemoved())return;var cause=level() instanceof ServerLevel s&&owner!=null?s.getEntity(owner):null;discard();if(enabled(level(),true))level().explode(cause,getX(),getY(),getZ(),3,Level.ExplosionInteraction.TNT);}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(!enabled(level(),impact())){input=Vec3.ZERO;setDeltaMovement(getDeltaMovement().scale(.7));}else if(battery>0){battery--;if(tickCount%20==0)entityData.set(BATTERY,battery);}
        if(launched()){
            if(++flightAge>400||battery<=0||!enabled(level(),true)){entityData.set(LAUNCHED,false);setDeltaMovement(Vec3.ZERO);return;}
            Vec3 from=position(),to=from.add(getDeltaMovement());if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(to))){entityData.set(LAUNCHED,false);setDeltaMovement(Vec3.ZERO);return;}
            var hit=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,this));
            boolean entityHit=false;for(var e:level().getEntities(this,getBoundingBox().expandTowards(getDeltaMovement()).inflate(.12),e->e.isAlive()&&e.isPickable()&&!e.getUUID().equals(owner))){if(e.getBoundingBox().inflate(.2).clip(from,to).isPresent()){entityHit=true;break;}}
            if(hit.getType()!=HitResult.Type.MISS||entityHit){setPos(hit.getType()==HitResult.Type.MISS?to:hit.getLocation());detonate();return;}move(MoverType.SELF,getDeltaMovement());
        }else {
            var desired=battery>0&&level().getGameTime()-inputAt<=8&&enabled(level(),impact())?input.scale(.48):Vec3.ZERO;
            Vec3 velocity=getDeltaMovement().scale(.72).add(desired.scale(.28));if(battery<=0||isInWater())velocity=velocity.add(0,-.025,0);
            if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(position().add(velocity))))velocity=Vec3.ZERO;
            setDeltaMovement(velocity);move(MoverType.SELF,velocity);if(horizontalCollision||verticalCollision)setDeltaMovement(Vec3.ZERO);
        }
        if(level() instanceof ServerLevel s&&tickCount%40==0&&battery>0&&OutpostBudget.effect(s))s.playSound(null,blockPosition(),SoundEvents.BEE_LOOP,SoundSource.NEUTRAL,.22F,impact()?.65F:1.2F);
    }
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);n.putBoolean("Impact",impact());n.putBoolean("Launched",launched());n.putInt("Battery",battery);n.putInt("FlightAge",flightAge);}
    @Override protected void readAdditionalSaveData(CompoundTag n){configure(n.hasUUID("Owner")?n.getUUID("Owner"):null,n.getBoolean("Impact"),n.getInt("Battery"));entityData.set(LAUNCHED,n.getBoolean("Launched"));flightAge=Mth.clamp(n.getInt("FlightAge"),0,400);}
}
