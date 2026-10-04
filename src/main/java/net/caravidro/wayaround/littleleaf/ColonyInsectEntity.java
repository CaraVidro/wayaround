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
    private static final EntityDataAccessor<Integer> LIFE=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.INT),JOB=SynchedEntityData.defineId(ColonyInsectEntity.class,EntityDataSerializers.INT);
    public static final int FORAGER=0,BUILDER=1,NURSE=2,UNDERTAKER=3;
    private UUID bodyId,carrier;private int fed,yieldUntil;private boolean trafficGrip;private long born;private boolean buried,bodyCreated,weatherWaiting;private int fleeUntil;
    public int job(){return entityData.get(JOB);}public void assignJob(int job){entityData.set(JOB,Math.clamp(job,0,3));}
    public boolean larva(){return entityData!=null&&entityData.get(LIFE)==1;}public boolean corpse(){return entityData!=null&&entityData.get(LIFE)==2;}public boolean activeAdult(){return isAlive()&&!larva()&&!corpse();}
    public boolean emergency(){return gardenMode==1||weatherWaiting||tickCount<fleeUntil;}
    public boolean carryingBody(){return bodyId!=null;}public int feeds(){return fed;}public long born(){return born;}
    public void feed(){fed=Math.min(3,fed+1);}
    public void yieldFor(int ticks){yieldUntil=tickCount+ticks;if(climbing()){trafficGrip=true;setNoGravity(true);}}public boolean yielding(){return tickCount<yieldUntil;}
    public ColonyInsectEntity carriedBody(){return bodyId!=null&&level() instanceof ServerLevel l&&l.getEntity(bodyId) instanceof ColonyInsectEntity e&&e.corpse()?e:null;}
    public void clearBody(){bodyId=null;}public void releaseBody(){carrier=null;setNoGravity(false);}public void bury(){buried=true;}public boolean buried(){return buried;}
    public void makeLarva(){entityData.set(LIFE,1);if(born==0)born=level().getGameTime();getAttribute(Attributes.SCALE).setBaseValue(getAttribute(Attributes.SCALE).getBaseValue()*.35);refreshDimensions();setNoAi(true);}
    public void grow(){entityData.set(LIFE,0);var c=colony();int task=job();bind(home,caste(),inside,c!=null&&c.giant());assignJob(task);setNoAi(false);}
    public void makeCorpse(){entityData.set(LIFE,2);bodyCreated=true;setNoAi(true);setPersistenceRequired();setDeltaMovement(Vec3.ZERO);}
    public boolean takeBody(ColonyInsectEntity body){if(carrying()||carryingMaterial()||carryingBody()||!body.corpse()||body.buried||body.carrier!=null||!getBoundingBox().inflate(enlarged()?1:.2).intersects(body.getBoundingBox()))return false;bodyId=body.getUUID();body.carrier=getUUID();body.setNoGravity(true);return true;}
    private ColonyGoal workerGoal;
    private BlockPos buildSite;private boolean invading,routeClimbing;
    public String workStatus(){return "job="+job()+", life="+entityData.get(LIFE)+", garden="+gardenMode+", weather="+weatherWaiting+", target="+(getTarget()==null?null:getTarget().getType())+", scale="+getScale()+", width="+getBbWidth()+", site="+buildSite+", destination="+(workerGoal==null?null:workerGoal.destination)+", route="+(workerGoal==null?null:workerGoal.route.status());}
    public boolean carryingMaterial(){return entityData.get(MATERIAL).isPresent();}
    public BlockState material(){return entityData.get(MATERIAL).orElse(null);}
    public void material(BlockState state){entityData.set(MATERIAL,Optional.ofNullable(state));}
    public void routeClimbing(boolean b){routeClimbing=b;entityData.set(CLIMB,b||horizontalCollision);}
    public boolean climbing(){return entityData.get(CLIMB);}
    public void invade(){invading=true;}
    private BlockPos home;private boolean inside;private int unboundTicks,gardenMode;
    public boolean sheltered(){return gardenMode==2||weatherWaiting;}
    public ColonyInsectEntity(EntityType<? extends ColonyInsectEntity> type,Level level){super(type,level);}
    public int species(){return getType()==LittleLeafContent.RED_ANT.get()?1:getType()==LittleLeafContent.HONEY_ANT.get()?2:getType()==LittleLeafContent.TERMITE.get()?3:0;}
    public int caste(){return entityData.get(CASTE);}
    public boolean carrying(){return entityData.get(CARRY);}
    public void carry(boolean b){entityData.set(CARRY,b);}
    public BlockPos home(){return home;}
    public boolean inside(){return inside;}
    public boolean enlarged(){return getScale()>.5;}
    public void bind(BlockPos p,int role,boolean interior,boolean giant){home=p.immutable();inside=interior;entityData.set(CASTE,Math.max(0,Math.min(2,role)));getAttribute(Attributes.SCALE).setBaseValue((interior||giant?ColonyRules.GIANT:ColonyRules.TINY)*(role==2?2:role==1?1.2:1));getAttribute(Attributes.MAX_HEALTH).setBaseValue(role==2?40:role==1?16:8);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(interior||giant?role==1?6:2:role==1?.35:.08);assignJob(Math.floorMod(getId(),4));setHealth(getMaxHealth());refreshDimensions();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CASTE,0);b.define(CARRY,false);b.define(CLIMB,false);b.define(MATERIAL,Optional.empty());b.define(LIFE,0);b.define(JOB,0);}
    @Override protected PathNavigation createNavigation(Level l){return new WallClimberNavigation(this,l);}
    @Override public boolean onClimbable(){return !yielding()&&entityData.get(CLIMB);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this){@Override public boolean canUse(){return enlarged()&&super.canUse();}});
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,false){
            @Override public void tick(){
                super.tick();
                var target=getTarget();
                // Vanilla navigation stops within a block of the target. An
                // ant's scaled melee reach is much smaller, so close that last
                // gap against the actual body rather than its integer block.
                if(enlarged()||target==null||!target.isAlive()||distanceToSqr(target)>2.5*2.5||Math.abs(target.getY()-getY())>.3)return;
                var delta=target.position().subtract(position());
                var flat=new Vec3(delta.x,0,delta.z);
                if(flat.lengthSqr()<.0025)return;
                var step=flat.normalize().scale(Math.min(.045,flat.length()));
                if(!level().noCollision(ColonyInsectEntity.this,getBoundingBox().move(step)))return;
                getNavigation().stop();setDeltaMovement(step.x,getDeltaMovement().y,step.z);
                setYRot((float)(Math.atan2(-step.x,step.z)*180/Math.PI));
            }
        });
        workerGoal=new ColonyGoal();goalSelector.addGoal(3,workerGoal);
    }
    @Override public void aiStep(){
        if(!level().isClientSide&&activeAdult()&&caste()!=2){var c=colony();if(c!=null&&c.wetWeather((ServerLevel)level()))setTarget(null);}
        super.aiStep();if(!level().isClientSide){
            if(corpse()){
                if(carrier!=null&&level() instanceof ServerLevel l){if(l.getEntity(carrier) instanceof ColonyInsectEntity worker&&worker.activeAdult()&&getUUID().equals(worker.bodyId)){var direction=Vec3.directionFromRotation(0,worker.getYRot()).scale(worker.getBbWidth()*.7);setPos(worker.getX()+direction.x,worker.getY()+worker.getBbHeight()*.5,worker.getZ()+direction.z);setDeltaMovement(Vec3.ZERO);}else releaseBody();}return;
            }
            if(larva()){var c=colony();if(c!=null&&!c.abandoned())c.mature((ServerLevel)level(),this);return;}
            if(trafficGrip&&!yielding()){trafficGrip=false;setNoGravity(false);}
            if(getAirSupply()>getMaxAirSupply())setAirSupply(getMaxAirSupply());
            entityData.set(CLIMB,horizontalCollision||routeClimbing);
            var target=getTarget();if(target!=null&&(!target.isAlive()||target.level()!=level()||distanceToSqr(target)>(inside?48*48:24*24)||target instanceof Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);
            if(home==null&&++unboundTicks>24000&&!hasCustomName())discard();
        }
    }
    @Override public int getMaxAirSupply(){return corpse()?300:enlarged()?100:60;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){if(corpse()||larva())return false;float grace=enlarged()?12:24;return distance>grace&&super.causeFallDamage(distance-grace,multiplier*.25F,source);}
    public ColonyCoreBlockEntity colony(){if(home==null||!(level() instanceof ServerLevel l)||!ColonyCoreBlockEntity.loaded(l,home))return null;return l.getBlockEntity(home) instanceof ColonyCoreBlockEntity c?c:null;}
    @Override public boolean hurt(DamageSource source,float amount){if(corpse())return false;if(source.getEntity() instanceof ColonyInsectEntity rival&&rival.species()!=species())setTarget(rival);if(source.getEntity() instanceof Player p){var c=colony();if(c!=null)c.remember(p.getUUID());setTarget(p);}return super.hurt(source,amount);}
    @Override public void jumpFromGround(){} // Wall routes climb continuously; no hopping at every ledge.
    public void dropMaterial(){if(carryingMaterial()&&!level().isClientSide){spawnAtLocation(new ItemStack(material().getBlock()));material(null);buildSite=null;}}
    @Override public void die(DamageSource s){
        if(corpse())return;dropMaterial();var carried=carriedBody();if(carried!=null)carried.releaseBody();clearBody();var c=colony();if(caste()==2&&c!=null)c.queenDied();
        if(!bodyCreated&&!level().isClientSide){bodyCreated=true;var body=LittleLeafContent.type(species()).create(level());if(body!=null){body.bind(home==null?blockPosition():home,caste(),inside,enlarged());body.getAttribute(Attributes.SCALE).setBaseValue(getScale());body.refreshDimensions();body.moveTo(getX(),getY(),getZ(),getYRot(),0);body.makeCorpse();level().addFreshEntity(body);}}super.die(s);
    }
    @Override public void push(Entity other){if(other instanceof ColonyInsectEntity ally&&ally.species()==species()&&Objects.equals(home,ally.home))return;super.push(other);}
    @Override public boolean isPushable(){return caste()==1&&activeAdult()&&!emergency();}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<(enlarged()?128*128:40*40);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.SILVERFISH_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SILVERFISH_DEATH;}
    @Override protected float getSoundVolume(){return enlarged()?.35F:.04F;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(home!=null)t.putLong("ColonyHome",home.asLong());t.putBoolean("ColonyInside",inside);t.putInt("GardenMode",gardenMode);t.putBoolean("TrafficGrip",trafficGrip);t.putInt("Life",entityData.get(LIFE));t.putInt("Job",job());t.putInt("Fed",fed);t.putLong("Born",born);t.putBoolean("Buried",buried);t.putBoolean("BodyCreated",bodyCreated);if(bodyId!=null)t.putUUID("Body",bodyId);if(carrier!=null)t.putUUID("Carrier",carrier);t.putInt("Caste",caste());t.putBoolean("LeafLoad",carrying());t.putBoolean("Invading",invading);if(buildSite!=null)t.putLong("BuildSite",buildSite.asLong());if(carryingMaterial())t.put("Material",net.minecraft.nbt.NbtUtils.writeBlockState(material()));}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);home=t.contains("ColonyHome")?BlockPos.of(t.getLong("ColonyHome")):null;inside=t.getBoolean("ColonyInside");gardenMode=Math.clamp(t.getInt("GardenMode"),0,3);if(t.getBoolean("TrafficGrip"))setNoGravity(false);entityData.set(LIFE,Math.clamp(t.getInt("Life"),0,2));assignJob(t.getInt("Job"));fed=Math.clamp(t.getInt("Fed"),0,3);born=t.getLong("Born");buried=t.getBoolean("Buried");bodyCreated=t.getBoolean("BodyCreated");bodyId=t.hasUUID("Body")?t.getUUID("Body"):null;carrier=t.hasUUID("Carrier")?t.getUUID("Carrier"):null;entityData.set(CASTE,Math.max(0,Math.min(2,t.getInt("Caste"))));carry(t.getBoolean("LeafLoad"));invading=t.getBoolean("Invading");buildSite=t.contains("BuildSite")?BlockPos.of(t.getLong("BuildSite")):null;material(t.contains("Material")?net.minecraft.nbt.NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),t.getCompound("Material")):null);}
    private final class ColonyGoal extends Goal {
        private BlockPos destination,raid;private int rethink,wait,stuck,cycles;private Vec3 previous;
        private final ColonySurfaceRoute route=new ColonySurfaceRoute();
        ColonyGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return activeAdult()&&getTarget()==null;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(!(level() instanceof ServerLevel l))return;var c=colony();
            if(c!=null&&c.abandoned()){carry(false);dropMaterial();invading=false;raid=null;}
            if(tickCount%40==0&&c!=null&&!c.abandoned()&&!c.wetWeather(l)){
                var threat=l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(inside?24:12),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&c.hostile(p));if(!threat.isEmpty()){setTarget(threat.get(0));return;}
            }
            if(invading&&c!=null&&!c.abandoned()){
                var queen=c.queen(l);if(queen!=null){if(distanceToSqr(queen)<12*12){setTarget(queen);return;}route.follow(ColonyInsectEntity.this,queen.blockPosition());return;}
                if(tickCount%40==0)c.birth(l);
                destination=home.offset(64,0,4);route.follow(ColonyInsectEntity.this,destination);return;
            }
            if(caste()==1&&tickCount%20==0&&raid==null&&(c==null||!c.wetWeather(l))){
                double range=enlarged()?16:4;var rivals=l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range),e->e!=ColonyInsectEntity.this&&e.isAlive()&&(e instanceof net.minecraft.world.entity.monster.Enemy||e instanceof ColonyInsectEntity insect&&insect.activeAdult()&&insect.species()!=species()));
                LivingEntity closest=null;double distance=range*range;int checked=0;for(var rival:rivals){if(checked++>=64)break;double d=distanceToSqr(rival);if(d<distance&&hasLineOfSight(rival)){closest=rival;distance=d;}}if(closest!=null){setTarget(closest);return;}
            }
            if(caste()==2){getNavigation().stop();return;}
            if(c!=null&&!c.abandoned()&&!inside&&enlarged()&&(c.wetWeather(l)||tickCount<fleeUntil)){
                destination=home.offset(ColonyRules.radius(c.stage(),species()==3)+2,0,0);weatherWaiting=true;
                if(position().distanceToSqr(Vec3.atBottomCenterOf(destination))<3*3){if(carrying())c.acceptLoad(ColonyInsectEntity.this);if(carryingBody())c.bury(l,ColonyInsectEntity.this);getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);routeClimbing(false);}else route.followPoint(ColonyInsectEntity.this,destination);return;
            }else if(weatherWaiting){weatherWaiting=false;destination=null;route.reset();}
            if(caste()==0&&tickCount%40==0&&!inside&&c!=null){for(var enemy:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(enlarged()?6:2),e->e.isAlive()&&(e instanceof net.minecraft.world.entity.monster.Enemy||e instanceof ColonyInsectEntity insect&&insect.activeAdult()&&insect.species()!=species()))){fleeUntil=tickCount+100;break;}}
            if(c!=null&&!c.abandoned()&&!inside&&!enlarged()){
                if((c.wetWeather(l)||tickCount<fleeUntil)&&gardenMode==0){gardenMode=1;destination=null;route.reset();wait=0;}
                if(gardenMode==2){getNavigation().stop();routeClimbing(false);setDeltaMovement(0,getDeltaMovement().y,0);if(!c.wetWeather(l)&&tickCount>=fleeUntil){gardenMode=3;destination=null;route.reset();}return;}
                if(gardenMode!=0){
                    if(!c.openEntrance(l)){destination=null;route.reset();return;}
                    destination=gardenMode==1?c.gardenEntry():c.gardenExit();
                    if(route.reached(ColonyInsectEntity.this,destination,.30)){
                        if(gardenMode==1){if(carrying()){c.acceptLoad(ColonyInsectEntity.this);cycles++;}if(carryingBody()&&!c.bury(l,ColonyInsectEntity.this))return;gardenMode=c.wetWeather(l)||tickCount<fleeUntil?2:3;}else gardenMode=0;
                        destination=null;route.reset();wait=15;return;
                    }route.followPoint(ColonyInsectEntity.this,destination);if(route.failed()){route.reset();destination=null;}return;
                }
            }
            if(yielding())return;
            if(wait-->0)return;
            if(c!=null&&!c.abandoned()&&caste()==0&&care(l,c))return;
            if(!inside&&c!=null&&!c.abandoned()&&!carrying()&&!carryingMaterial()&&caste()==1&&tickCount%200==getId()%200){var rival=c.rivalColony(l);if(rival!=null){raid=rival.getBlockPos();destination=null;route.reset();}}
            if(raid!=null){
                if(!ColonyCoreBlockEntity.loaded(l,raid)||!(l.getBlockEntity(raid) instanceof ColonyCoreBlockEntity victim)||victim.abandoned()){raid=null;destination=null;route.reset();return;}
                if(position().distanceToSqr(Vec3.atCenterOf(raid))<(enlarged()?16:3)){if(ColonyTravel.invade(l,victim,ColonyInsectEntity.this))return;}
                route.follow(ColonyInsectEntity.this,raid.above());return;
            }
            if(c!=null&&!c.abandoned()&&caste()==0&&enlarged()&&c.giant()&&!inside&&!carrying()&&job()==BUILDER){
                if(buildSite==null)buildSite=c.buildSite(l,ColonyInsectEntity.this);
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
                    route.follow(ColonyInsectEntity.this,destination);if(route.failed()){c.requestAccess(l,ColonyInsectEntity.this,destination);c.releaseWork(ColonyInsectEntity.this);buildSite=null;destination=null;wait=40;route.reset();}return;
                }
            }
            if(destination==null){
                if(c==null||c.abandoned())destination=blockPosition().offset(random.nextInt(7)-3,0,random.nextInt(7)-3);
                else if(caste()==1)destination=home.offset(random.nextInt(13)-6,0,random.nextInt(13)-6);
                else {if((carrying()||carryingBody())&&!inside&&!enlarged()){gardenMode=1;destination=null;route.reset();return;}destination=carrying()?dropSite(c):c.food(l,ColonyInsectEntity.this);}
                if(destination==null){wait=20;return;}route.reset();stuck=0;
            }
            if(!ColonyCoreBlockEntity.loaded(l,destination)){destination=null;wait=40;return;}
            if(c!=null&&!c.abandoned()&&caste()==0&&!carrying()&&c.cut(l,destination,ColonyInsectEntity.this)){getNavigation().stop();destination=null;route.reset();wait=15;return;}
            double reach=enlarged()?3:1.2;
            if((carrying()||caste()!=0||c==null||c.abandoned())&&position().distanceToSqr(Vec3.atBottomCenterOf(destination))<reach*reach){
                getNavigation().stop();if(c!=null&&!c.abandoned()&&caste()==0&&carrying()){
                    c.acceptLoad(ColonyInsectEntity.this);cycles++;
                }destination=null;route.reset();wait=30;return;
            }
            route.follow(ColonyInsectEntity.this,destination);
            // Ant-sized movement is real progress. Never teleport a forager away from its leaf.
            double epsilon=Math.max(.00002,getBbWidth()*.02);if(previous!=null&&position().distanceToSqr(previous)<epsilon*epsilon)stuck++;else stuck=0;previous=position();
            if(route.failed()||stuck>400){if(c!=null&&!carrying()){if(enlarged())c.requestAccess(l,ColonyInsectEntity.this,destination);c.rejectFood(destination);}destination=null;route.reset();stuck=0;wait=40;}
        }
        private boolean care(ServerLevel l,ColonyCoreBlockEntity c){
            if(carryingBody()){
                if(!inside&&!enlarged()){gardenMode=1;destination=null;route.reset();return true;}
                var goal=inside?c.cemetery():dropSite(c);if(position().distanceToSqr(Vec3.atBottomCenterOf(goal))<(enlarged()?9:.25)&&c.bury(l,ColonyInsectEntity.this)){destination=null;route.reset();wait=30;return true;}route.followPoint(ColonyInsectEntity.this,goal);return true;
            }
            if(job()==UNDERTAKER&&!carrying()&&!carryingMaterial()){
                var bodies=l.getEntitiesOfClass(ColonyInsectEntity.class,getBoundingBox().inflate(inside?96:enlarged()?24:8),e->e.corpse()&&!e.buried&&e.carrier==null&&e.species()==species());ColonyInsectEntity nearest=null;double best=Double.MAX_VALUE;int n=0;
                for(var body:bodies){if(n++>=64)break;double d=distanceToSqr(body);if(d<best){best=d;nearest=body;}}
                if(nearest!=null){if(takeBody(nearest)){destination=null;route.reset();}else route.followPoint(ColonyInsectEntity.this,nearest.blockPosition());return true;}
            }
            if(job()==NURSE&&ColonyTransitData.get(l.getServer()).food(c.identity())>0&&!carrying()&&!carryingMaterial()){
                var child=c.hungryLarva(l);if(child!=null){if(c.nurse(l,ColonyInsectEntity.this,child)){destination=null;route.reset();wait=20;}else route.followPoint(ColonyInsectEntity.this,child.blockPosition());return true;}
            }return false;
        }
        private BlockPos dropSite(ColonyCoreBlockEntity c){if(inside)return home.offset(26,0,4);int r=ColonyRules.radius(c.stage(),species()==3);return enlarged()?home.offset(r+2,0,0):home.offset(0,0,1);}
        @Override public void stop(){getNavigation().stop();destination=null;route.reset();routeClimbing(false);}
    }
}
