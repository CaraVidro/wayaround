package net.caravidro.wayaround.littleleaf;

import java.util.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Tiny leaf cutters with shared food routes, soldiers and stationary queens. */
public final class ColonyInsectEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CASTE=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CARRY=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.BOOLEAN),CLIMB=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockState>> MATERIAL=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    private ColonyGoal workerGoal;
    private BlockPos buildSite;private boolean invading,routeClimbing;
    public String workStatus(){return "scale="+getScale()+", width="+getBbWidth()+", site="+buildSite+", destination="+(workerGoal==null?null:workerGoal.destination)+", route="+(workerGoal==null?null:workerGoal.route.status());}
    public boolean carryingMaterial(){return entityData.get(MATERIAL).isPresent();}
    public BlockState material(){return entityData.get(MATERIAL).orElse(null);}
    public void material(BlockState state){entityData.set(MATERIAL,Optional.ofNullable(state));}
    public void routeClimbing(boolean b){routeClimbing=b;entityData.set(CLIMB,b||horizontalCollision);}
    public boolean climbing(){return entityData.get(CLIMB);}
    public void invade(){invading=true;}
    private BlockPos home;private boolean inside;private int unboundTicks;
    public ColonyInsectEntity(EntityType<? extends ColonyInsectEntity> type,Level level){super(type,level);}
    public int species(){return getType()==LittleLeafContent.RED_ANT.get()?1:getType()==LittleLeafContent.HONEY_ANT.get()?2:getType()==LittleLeafContent.TERMITE.get()?3:0;}
    public int caste(){return entityData.get(CASTE);}
    public boolean carrying(){return entityData.get(CARRY);}
    public void carry(boolean b){entityData.set(CARRY,b);}
    public BlockPos home(){return home;}
    public boolean inside(){return inside;}
    public boolean enlarged(){return getScale()>.5;}
    public void bind(BlockPos p,int role,boolean interior,boolean giant){home=p.immutable();inside=interior;entityData.set(CASTE,Math.max(0,Math.min(2,role)));getAttribute(Attributes.SCALE).setBaseValue((interior||giant?ColonyRules.GIANT:ColonyRules.TINY)*(role==2?2:role==1?1.2:1));getAttribute(Attributes.MAX_HEALTH).setBaseValue(role==2?40:role==1?16:8);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(role==1?4:2);setHealth(getMaxHealth());refreshDimensions();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CASTE,0);b.define(CARRY,false);b.define(CLIMB,false);b.define(MATERIAL,Optional.empty());}
    @Override protected PathNavigation createNavigation(Level l){return new WallClimberNavigation(this,l);}
    @Override public boolean onClimbable(){return entityData.get(CLIMB);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,false));workerGoal=new ColonyGoal();goalSelector.addGoal(3,workerGoal);}
    @Override public void aiStep(){
        super.aiStep();if(!level().isClientSide){
            entityData.set(CLIMB,horizontalCollision||routeClimbing);
            var target=getTarget();if(target!=null&&(!target.isAlive()||target.level()!=level()||distanceToSqr(target)>(inside?48*48:24*24)||target instanceof Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);
            if(home==null&&++unboundTicks>24000&&!hasCustomName())discard();
        }
    }
    public ColonyCoreBlockEntity colony(){if(home==null||!(level() instanceof ServerLevel l)||!ColonyCoreBlockEntity.loaded(l,home))return null;return l.getBlockEntity(home) instanceof ColonyCoreBlockEntity c?c:null;}
    @Override public boolean hurt(DamageSource source,float amount){if(source.getEntity() instanceof ColonyInsectEntity rival&&rival.species()!=species())setTarget(rival);if(source.getEntity() instanceof Player p){var c=colony();if(c!=null)c.remember(p.getUUID());setTarget(p);}return super.hurt(source,amount);}
    @Override public void jumpFromGround(){} // Wall routes climb continuously; no hopping at every ledge.
    public void dropMaterial(){if(carryingMaterial()&&!level().isClientSide){spawnAtLocation(new ItemStack(material().getBlock()));material(null);buildSite=null;}}
    @Override public void die(DamageSource s){dropMaterial();var c=colony();if(caste()==2&&c!=null)c.queenDied();super.die(s);}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<(enlarged()?128*128:40*40);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.SILVERFISH_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SILVERFISH_DEATH;}
    @Override protected float getSoundVolume(){return enlarged()?.35F:.04F;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(home!=null)t.putLong("ColonyHome",home.asLong());t.putBoolean("ColonyInside",inside);t.putInt("Caste",caste());t.putBoolean("LeafLoad",carrying());t.putBoolean("Invading",invading);if(buildSite!=null)t.putLong("BuildSite",buildSite.asLong());if(carryingMaterial())t.put("Material",net.minecraft.nbt.NbtUtils.writeBlockState(material()));}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);home=t.contains("ColonyHome")?BlockPos.of(t.getLong("ColonyHome")):null;inside=t.getBoolean("ColonyInside");entityData.set(CASTE,Math.max(0,Math.min(2,t.getInt("Caste"))));carry(t.getBoolean("LeafLoad"));invading=t.getBoolean("Invading");buildSite=t.contains("BuildSite")?BlockPos.of(t.getLong("BuildSite")):null;material(t.contains("Material")?net.minecraft.nbt.NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),t.getCompound("Material")):null);}
    private final class ColonyGoal extends Goal {
        private BlockPos destination,raid;private int rethink,wait,burrowing,stuck,cycles;private Vec3 previous;
        private final ColonySurfaceRoute route=new ColonySurfaceRoute();
        ColonyGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return getTarget()==null;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(!(level() instanceof ServerLevel l))return;var c=colony();
            if(c!=null&&c.abandoned()){carry(false);dropMaterial();invading=false;raid=null;}
            if(tickCount%40==0&&c!=null&&!c.abandoned()){
                var queen=c.queen(l);if(queen!=null)for(var p:l.getEntitiesOfClass(Player.class,queen.getBoundingBox().inflate(inside?12:queen.enlarged()?8:1.5),p->!p.isCreative()&&!p.isSpectator()))c.remember(p.getUUID());
                var threat=l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(inside?24:12),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&c.hostile(p));if(!threat.isEmpty()){setTarget(threat.get(0));return;}
            }
            if(invading&&c!=null&&!c.abandoned()){
                var queen=c.queen(l);if(queen!=null){setTarget(queen);return;}
                if(tickCount%40==0)c.birth(l);
                destination=home.offset(64,0,4);route.follow(ColonyInsectEntity.this,destination);return;
            }
            if(caste()!=2&&tickCount%40==0&&raid==null){
                double range=enlarged()?16:4;var rivals=l.getEntitiesOfClass(ColonyInsectEntity.class,getBoundingBox().inflate(range),e->e!=ColonyInsectEntity.this&&e.isAlive()&&e.species()!=species());
                ColonyInsectEntity closest=null;double distance=range*range;int checked=0;for(var rival:rivals){if(checked++>=64)break;double d=distanceToSqr(rival);if(d<distance&&hasLineOfSight(rival)){closest=rival;distance=d;}}if(closest!=null){setTarget(closest);return;}
            }
            if(caste()==2){getNavigation().stop();return;}
            if(burrowing>0){if(--burrowing==0&&c!=null){c.acceptLoad(ColonyInsectEntity.this);c.placeAtEntrance(l,ColonyInsectEntity.this,home.above());destination=null;route.reset();wait=30;cycles++;}return;}
            if(wait-->0)return;
            if(!inside&&c!=null&&!c.abandoned()&&!carrying()&&!carryingMaterial()&&caste()==1&&tickCount%200==getId()%200){var rival=c.rivalColony(l);if(rival!=null){raid=rival.getBlockPos();destination=null;route.reset();}}
            if(raid!=null){
                if(!ColonyCoreBlockEntity.loaded(l,raid)||!(l.getBlockEntity(raid) instanceof ColonyCoreBlockEntity victim)||victim.abandoned()){raid=null;destination=null;route.reset();return;}
                if(position().distanceToSqr(Vec3.atCenterOf(raid))<(enlarged()?16:3)){if(ColonyTravel.invade(l,victim,ColonyInsectEntity.this))return;}
                route.follow(ColonyInsectEntity.this,raid.above());return;
            }
            if(c!=null&&!c.abandoned()&&caste()==0&&enlarged()&&c.giant()&&!inside&&!carrying()){
                if(buildSite==null&&(cycles%2==1||(getId()&1)==0))buildSite=c.buildSite(l,ColonyInsectEntity.this);
                if(buildSite!=null){
                    if(carryingMaterial()){
                        if(c.placeMaterial(l,buildSite,ColonyInsectEntity.this)){buildSite=null;destination=null;route.reset();cycles++;wait=30;return;}
                        if(!l.getBlockState(buildSite).isAir()){buildSite=c.buildSite(l,ColonyInsectEntity.this);route.reset();if(buildSite==null){dropMaterial();return;}}
                        destination=buildSite;
                    }else{
                        if(destination==null||destination.equals(buildSite)){destination=c.soil(l);rethink=tickCount+40;route.reset();}
                        if(destination==null)return;
                        if(c.excavate(l,destination,ColonyInsectEntity.this)){destination=buildSite;route.reset();wait=15;return;}
                    }
                    route.follow(ColonyInsectEntity.this,destination);if(route.failed()){destination=null;wait=100;route.reset();}return;
                }
            }
            if(destination==null){
                if(c==null||c.abandoned())destination=blockPosition().offset(random.nextInt(7)-3,0,random.nextInt(7)-3);
                else if(caste()==1)destination=home.offset(random.nextInt(13)-6,0,random.nextInt(13)-6);
                else destination=carrying()?dropSite(c):c.food(l);
                if(destination==null){wait=20;return;}route.reset();stuck=0;
            }
            if(!ColonyCoreBlockEntity.loaded(l,destination)){destination=null;wait=40;return;}
            if(c!=null&&!c.abandoned()&&caste()==0&&!carrying()&&c.cut(l,destination,ColonyInsectEntity.this)){getNavigation().stop();destination=null;route.reset();wait=15;return;}
            double reach=enlarged()?3:1.2;
            if((carrying()||caste()!=0||c==null||c.abandoned())&&position().distanceToSqr(Vec3.atBottomCenterOf(destination))<reach*reach){
                getNavigation().stop();if(c!=null&&!c.abandoned()&&caste()==0&&carrying()){
                    if(!enlarged()&&!inside){moveTo(home.getX()+.5,home.getY()-3,home.getZ()+.5,getYRot(),0);burrowing=40;destination=null;route.reset();return;}
                    c.acceptLoad(ColonyInsectEntity.this);cycles++;
                }destination=null;route.reset();wait=30;return;
            }
            route.follow(ColonyInsectEntity.this,destination);
            // Ant-sized movement is real progress. Never teleport a forager away from its leaf.
            double epsilon=Math.max(.00002,getBbWidth()*.02);if(previous!=null&&position().distanceToSqr(previous)<epsilon*epsilon)stuck++;else stuck=0;previous=position();
            if(route.failed()||stuck>400){if(c!=null&&!carrying())c.rejectFood(destination);destination=null;route.reset();stuck=0;wait=40;}
        }
        private BlockPos dropSite(ColonyCoreBlockEntity c){if(inside)return home.offset(26,0,4);int r=ColonyRules.radius(c.stage(),species()==3);return enlarged()?home.offset(r+2,0,0):home.offset(0,0,1);}
        @Override public void stop(){getNavigation().stop();destination=null;route.reset();routeClimbing(false);}
    }
}
