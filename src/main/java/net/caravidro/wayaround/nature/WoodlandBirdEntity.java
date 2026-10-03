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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.level.pathfinder.Path;

/** Small real birds; distant migration flocks are a separate visual event. */
public final class WoodlandBirdEntity extends PathfinderMob implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> PERCHED=SynchedEntityData.defineId(WoodlandBirdEntity.class,EntityDataSerializers.BOOLEAN);
    public WoodlandBirdEntity(EntityType<? extends WoodlandBirdEntity> t,Level l){super(t,l);moveControl=new FlyingMoveControl(this,20,true);setNoGravity(true);}
    public int species(){return getType()==NatureContent.HUMMINGBIRD.get()?0:getType()==NatureContent.THRUSH.get()?1:getType()==NatureContent.PARROT.get()?2:3;}
    public boolean perched(){return entityData.get(PERCHED);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PERCHED,false);}
    public static AttributeSupplier.Builder attributes(double hp,double speed){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,hp).add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.FLYING_SPEED,speed).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected PathNavigation createNavigation(Level l){FlyingPathNavigation nav = new FlyingPathNavigation(this,l); nav.setCanFloat(true); nav.setCanPassDoors(false); nav.setCanOpenDoors(false); return nav;}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new PanicGoal(this,1.4)); if(getType()==NatureContent.CROW.get())goalSelector.addGoal(1,new BirdFoodTheftGoal(this)); goalSelector.addGoal(2,new NectarGoal()); goalSelector.addGoal(4,new BirdFlightGoal());}
    @Override public boolean isFlying(){return !perched();}
    @Override public boolean removeWhenFarAway(double d){return !hasCustomName();}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.PARROT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource d){return SoundEvents.PARROT_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.PARROT_DEATH;}
    @Override protected float getSoundVolume(){return species()==0?.18F:.30F;}
    @Override public float getVoicePitch(){return species()==0?1.8F:species()==1?1.3F:species()==3?.65F:.9F;}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<96*96;}
    public static boolean nectarOffer(ItemStack stack) { return stack.is(ItemTags.FLOWERS) || stack.is(Items.SUGAR); }
    private void airborne() { entityData.set(PERCHED,false); setNoGravity(true); }
    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isInWaterOrBubble()) {
            airborne();setDeltaMovement(getDeltaMovement().add(0,.08,0));
        }
    }
    private final class NectarGoal extends Goal {
        private Player admirer;
        private BlockPos flower;
        private int searchAt, repathAt;
        NectarGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if(species()!=0 || tickCount<searchAt)return false;
            searchAt=tickCount+60;
            admirer=level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(10),p->!p.isSpectator()&&(nectarOffer(p.getMainHandItem())||nectarOffer(p.getOffhandItem())))
                    .stream().limit(8).min(java.util.Comparator.comparingDouble(WoodlandBirdEntity.this::distanceToSqr)).orElse(null);
            flower=null;
            if(admirer!=null)return true;
            double best=Double.MAX_VALUE;
            // At most 968 cells per search, only in chunks already present.
            for(BlockPos p:BlockPos.betweenClosed(blockPosition().offset(-5,-6,-5),blockPosition().offset(5,1,5))) {
                if(!level().hasChunkAt(p)||!level().getBlockState(p).is(BlockTags.FLOWERS))continue;
                if(!level().getBlockState(p.above()).isAir())continue;
                double d=position().distanceToSqr(Vec3.atCenterOf(p));
                if(d<best){best=d;flower=p.immutable();}
            }
            return flower!=null;
        }
        @Override public void start(){airborne();repathAt=0;}
        @Override public boolean canContinueToUse(){return species()==0&&(admirer!=null?admirer.isAlive()&&distanceToSqr(admirer)<144&&(nectarOffer(admirer.getMainHandItem())||nectarOffer(admirer.getOffhandItem())):flower!=null&&level().getBlockState(flower).is(BlockTags.FLOWERS)&&tickCount<searchAt+140);}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            Vec3 target=admirer!=null?admirer.position().add(0,1.5,0):Vec3.atCenterOf(flower).add(0,.7,0);
            if(position().distanceToSqr(target)<1){getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(.65));return;}
            if(tickCount>=repathAt){getNavigation().moveTo(target.x,target.y,target.z,1.05);repathAt=tickCount+20;}
        }
        @Override public void stop(){getNavigation().stop();admirer=null;flower=null;searchAt=tickCount+40;}
    }
    private final class BirdFlightGoal extends Goal {
        private Vec3 target;
        private int nextMove, repathAt, stuck, perchUntil;
        private Vec3 last;
        BirdFlightGoal(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return true;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){airborne();target=null;last=position();stuck=0;nextMove=0;}
        @Override public void stop(){getNavigation().stop();airborne();target=null;}
        @Override public void tick(){
            if(perched()&&tickCount<perchUntil&&level().getBlockState(blockPosition().below()).isCollisionShapeFullBlock(level(),blockPosition().below()))return;
            airborne();
            if(tickCount>=nextMove||(tickCount>=repathAt&&(target==null||horizontalCollision||stuck>30))){chooseTarget();nextMove=tickCount+100+random.nextInt(80);repathAt=tickCount+30;stuck=0;}
            if(target==null)return;
            double distance=position().distanceToSqr(target);
            if(distance<.5){
                getNavigation().stop();setDeltaMovement(getDeltaMovement().scale(.5));
                BlockPos below=BlockPos.containing(target).below();
                if(species()!=0&&level().noCollision(WoodlandBirdEntity.this)&&level().getBlockState(below).isCollisionShapeFullBlock(level(),below)){
                    entityData.set(PERCHED,true);setNoGravity(false);perchUntil=tickCount+60+random.nextInt(100);nextMove=perchUntil;
                }
                return;
            }
            if(last!=null&&position().distanceToSqr(last)<.0004)stuck++;else stuck=0;
            last=position();
            if(tickCount>=repathAt){
                Path path=getNavigation().createPath(target.x,target.y,target.z,1);
                if(path!=null&&path.canReach())getNavigation().moveTo(path,1);else nextMove=tickCount+10;
                repathAt=tickCount+30;
            }
        }
        private void chooseTarget(){
            target=null;
            for(int tries=0;tries<8;tries++){
                int x=getBlockX()+random.nextInt(17)-8,z=getBlockZ()+random.nextInt(17)-8;
                if(!level().hasChunkAt(new BlockPos(x,getBlockY(),z)))continue;
                int y=level().getHeight(species()==1?Heightmap.Types.MOTION_BLOCKING_NO_LEAVES:Heightmap.Types.MOTION_BLOCKING,x,z);
                if(Math.abs(y-getY())>10)y=getBlockY()+random.nextInt(7)-3;
                BlockPos p=new BlockPos(x,y,z);
                if(!level().getBlockState(p).isAir()||!level().getFluidState(p).isEmpty())continue;
                Vec3 candidate=new Vec3(x+.5,y+(species()==0?.7:0),z+.5);
                if(!level().noCollision(WoodlandBirdEntity.this,getBoundingBox().move(candidate.subtract(position()))))continue;
                Path path=getNavigation().createPath(candidate.x,candidate.y,candidate.z,1);
                if(path==null||!path.canReach())continue;
                target=candidate;getNavigation().moveTo(path,1);return;
            }
            getNavigation().stop();
        }
    }
}
