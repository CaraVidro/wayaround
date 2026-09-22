package net.caravidro.wayaround.dream;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.phys.Vec3;

public final class DreamPlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(DreamPlayerEntity.class,EntityDataSerializers.INT);
    private DreamPlayerProfile profile=DreamPlayerProfile.from(new PlayerBehaviorProfile());
    private BlockPos targetChest;
    private DreamPlayerState originalTask=DreamPlayerState.GO_TO_CHEST;
    private UUID owner,lastAttacker;
    private int stateTime,stareTime;
    private boolean interacting;
    public DreamPlayerEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(STATE,0);}
    public DreamPlayerState state(){return DreamPlayerState.values()[Math.clamp(entityData.get(STATE),0,DreamPlayerState.values().length-1)];}
    private void transition(DreamPlayerState state){entityData.set(STATE,state.ordinal());stateTime=0;}
    public void configure(UUID player,BlockPos chest,DreamPlayerProfile behavior){
        owner=player;targetChest=chest;profile=behavior;originalTask=DreamPlayerState.GO_TO_CHEST;
        transition(chest==null?DreamPlayerState.IDLE:originalTask);
    }
    public UUID owner(){return owner;}
    public BlockPos targetChest(){return targetChest;}
    @Override protected double getDefaultGravity(){return state()==DreamPlayerState.INTERRUPTED?.055:super.getDefaultGravity();}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide)return true;
        if(source.getEntity() instanceof Player attacker){
            if(owner!=null&&level().getPlayerByUUID(owner) instanceof net.minecraft.server.level.ServerPlayer dreamer){
                DreamSession session=DreamManager.session(dreamer);
                if(session!=null)session.director.onActorHit();
            }
            closeChest();getNavigation().stop();lastAttacker=attacker.getUUID();
            Vec3 push=position().subtract(attacker.position()).multiply(1,0,1);
            if(push.lengthSqr()<.001)push=new Vec3(1,0,0);
            push=push.normalize().scale(.55);
            setDeltaMovement(push.x,.3,push.z);hasImpulse=true;hurtMarked=true;
            level().broadcastEntityEvent(this,(byte)2);
            transition(DreamPlayerState.INTERRUPTED);
        }
        return true;
    }
    @Override protected void customServerAiStep(){
        super.customServerAiStep();stateTime++;
        Player attacker=lastAttacker==null?null:level().getPlayerByUUID(lastAttacker);
        switch(state()){
            case IDLE -> {}
            case GO_TO_CHEST,RETURN_TO_CHEST -> {
                if(targetChest==null||!level().hasChunkAt(targetChest)){transition(DreamPlayerState.IDLE);break;}
                if(distanceToSqr(Vec3.atCenterOf(targetChest))<7){
                    getNavigation().stop();transition(DreamPlayerState.OPEN_CHEST);
                }else if(stateTime%20==1){
                    getNavigation().moveTo(targetChest.getX()+.5,targetChest.getY(),targetChest.getZ()+.5,profile.movementSpeed());
                    if(onGround()&&random.nextFloat()<profile.jumpFrequency())getJumpControl().jump();
                }
            }
            case OPEN_CHEST -> {
                if(targetChest==null){transition(DreamPlayerState.IDLE);break;}
                getLookControl().setLookAt(targetChest.getX()+.5,targetChest.getY()+.7,targetChest.getZ()+.5);
                if(stateTime%10==1){openChest();swing(net.minecraft.world.InteractionHand.MAIN_HAND);}
            }
            case INTERRUPTED -> {
                if(stateTime>=12&&(onGround()||stateTime>=40)){
                    stareTime=profile.stareMinTicks()+random.nextInt(profile.stareMaxTicks()-profile.stareMinTicks()+1);
                    transition(DreamPlayerState.STARE_AT_PLAYER);
                }
            }
            case STARE_AT_PLAYER -> {
                if(attacker!=null){
                    getLookControl().setLookAt(attacker,30,30);lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,attacker.getEyePosition());
                    setYBodyRot(getYHeadRot());
                }
                if(stateTime>=stareTime)transition(DreamPlayerState.RETURN_TO_CHEST);
            }
        }
    }
    private void openChest(){setChestOpen(true);interacting=true;}
    private void closeChest(){if(interacting)setChestOpen(false);interacting=false;}
    private void setChestOpen(boolean open){
        if(targetChest==null||!level().hasChunkAt(targetChest))return;
        var state=level().getBlockState(targetChest);
        if(state.getBlock() instanceof BarrelBlock)level().setBlock(targetChest,state.setValue(BarrelBlock.OPEN,open),2);
        else if(level().getBlockEntity(targetChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)
            level().blockEvent(targetChest,state.getBlock(),1,open?1:0);
    }
    @Override public void remove(RemovalReason reason){if(!level().isClientSide)closeChest();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("DreamOwner",owner);
        if(targetChest!=null)tag.putLong("TargetChest",targetChest.asLong());
        tag.putFloat("JumpChance",profile.jumpFrequency());tag.putDouble("SpeedModifier",profile.movementSpeed());
        tag.putBoolean("LikesContainers",profile.likesContainers());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);
        configure(tag.hasUUID("DreamOwner")?tag.getUUID("DreamOwner"):null,tag.contains("TargetChest")?BlockPos.of(tag.getLong("TargetChest")):null,
                new DreamPlayerProfile(tag.getBoolean("LikesContainers"),Math.clamp(tag.getFloat("JumpChance"),.01F,.70F),
                        Math.clamp(tag.getDouble("SpeedModifier"),.85,1.35),14,60));
    }
}
