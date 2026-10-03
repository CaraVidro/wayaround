package net.caravidro.wayaround.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Three small real birds; distant migration flocks are a separate visual event. */
public final class WoodlandBirdEntity extends PathfinderMob implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> PERCHED=SynchedEntityData.defineId(WoodlandBirdEntity.class,EntityDataSerializers.BOOLEAN);
    public WoodlandBirdEntity(EntityType<? extends WoodlandBirdEntity> t,Level l){super(t,l);moveControl=new FlyingMoveControl(this,12,true);setNoGravity(true);}
    public int species(){return getType()==NatureContent.HUMMINGBIRD.get()?0:getType()==NatureContent.THRUSH.get()?1:2;}
    public boolean perched(){return entityData.get(PERCHED);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PERCHED,false);}
    public static AttributeSupplier.Builder attributes(double hp,double speed){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,hp).add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.FLYING_SPEED,speed).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected PathNavigation createNavigation(Level l){return new FlyingPathNavigation(this,l);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new BirdFlightGoal());}
    @Override public boolean isFlying(){return !perched();}
    @Override public boolean removeWhenFarAway(double d){return !hasCustomName();}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.PARROT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource d){return SoundEvents.PARROT_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.PARROT_DEATH;}
    @Override protected float getSoundVolume(){return species()==0?.18F:.30F;}
    @Override public float getVoicePitch(){return species()==0?1.8F:species()==1?1.3F:.9F;}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<96*96;}
    private final class BirdFlightGoal extends Goal {
        private Vec3 target;
        private int nextMove;
        BirdFlightGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return true;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(isInWaterOrBubble()){setNoGravity(true);entityData.set(PERCHED,false);setDeltaMovement(getDeltaMovement().add(0,.08,0));}
            if(tickCount>=nextMove||target==null){chooseTarget();nextMove=tickCount+100+random.nextInt(140);}
            if(target==null)return;
            double distance=position().distanceToSqr(target);
            if(distance<.12&&species()!=0){entityData.set(PERCHED,true);setNoGravity(false);setDeltaMovement(Vec3.ZERO);return;}
            entityData.set(PERCHED,false);setNoGravity(true);
            moveControl.setWantedPosition(target.x,target.y,target.z,1);
            getLookControl().setLookAt(target.x,target.y,target.z,20,20);
            // Tiny flower visitor hovers; thrush forages low; parrot perches high.
            if(species()==0&&distance<.25)setDeltaMovement(getDeltaMovement().scale(.5));
        }
        private void chooseTarget(){
            for(int tries=0;tries<6;tries++){
                int x=getBlockX()+random.nextInt(17)-8,z=getBlockZ()+random.nextInt(17)-8;
                BlockPos column=new BlockPos(x,0,z);
                if(!level().hasChunkAt(column))continue;
                int y=level().getHeight(species()==1?Heightmap.Types.MOTION_BLOCKING_NO_LEAVES:Heightmap.Types.MOTION_BLOCKING,x,z);
                BlockPos p=new BlockPos(x,y,z);
                if(!level().getBlockState(p).isAir()||!level().getFluidState(p.below()).isEmpty())continue;
                if(species()==2&&!level().getBlockState(p.below()).is(BlockTags.LEAVES))continue;
                target=new Vec3(x+.5,y+(species()==0?.7:0),z+.5);return;
            }
            target=position().add(random.nextDouble()*6-3,random.nextDouble()*2+1,random.nextDouble()*6-3);
        }
    }
}
