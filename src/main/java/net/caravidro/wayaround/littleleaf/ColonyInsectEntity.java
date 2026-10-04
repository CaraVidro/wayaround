package net.caravidro.wayaround.littleleaf;

import java.util.EnumSet;
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
    private BlockPos home;private boolean inside;private int unboundTicks;
    public ColonyInsectEntity(EntityType<? extends ColonyInsectEntity> type,Level level){super(type,level);}
    public int species(){return getType()==LittleLeafContent.RED_ANT.get()?1:getType()==LittleLeafContent.HONEY_ANT.get()?2:getType()==LittleLeafContent.TERMITE.get()?3:0;}
    public int caste(){return entityData.get(CASTE);}
    public boolean carrying(){return entityData.get(CARRY);}
    public void carry(boolean b){entityData.set(CARRY,b);}
    public BlockPos home(){return home;}
    public boolean inside(){return inside;}
    public boolean enlarged(){return getScale()>.5;}
    public void bind(BlockPos p,int role,boolean interior,boolean giant){home=p.immutable();inside=interior;entityData.set(CASTE,Math.max(0,Math.min(2,role)));getAttribute(Attributes.SCALE).setBaseValue((interior||giant?ColonyRules.GIANT:ColonyRules.TINY)*(role==2?2:role==1?1.2:1));getAttribute(Attributes.MAX_HEALTH).setBaseValue(role==2?40:role==1?16:8);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(role==1?4:2);setHealth(getMaxHealth());}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CASTE,0);b.define(CARRY,false);b.define(CLIMB,false);}
    @Override protected PathNavigation createNavigation(Level l){return new WallClimberNavigation(this,l);}
    @Override public boolean onClimbable(){return entityData.get(CLIMB);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,false));goalSelector.addGoal(3,new ColonyGoal());}
    @Override public void aiStep(){super.aiStep();if(!level().isClientSide){entityData.set(CLIMB,horizontalCollision&&!enlarged());if(home==null&&++unboundTicks>24000&&!hasCustomName())discard();}}
    public ColonyCoreBlockEntity colony(){if(home==null||!(level() instanceof ServerLevel l)||!ColonyCoreBlockEntity.loaded(l,home))return null;return l.getBlockEntity(home) instanceof ColonyCoreBlockEntity c?c:null;}
    @Override public boolean hurt(DamageSource source,float amount){if(source.getEntity() instanceof Player p){var c=colony();if(c!=null)c.remember(p.getUUID());setTarget(p);}return super.hurt(source,amount);}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<(enlarged()?128*128:40*40);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.SILVERFISH_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SILVERFISH_DEATH;}
    @Override protected float getSoundVolume(){return enlarged()?.35F:.04F;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(home!=null)t.putLong("ColonyHome",home.asLong());t.putBoolean("ColonyInside",inside);t.putInt("Caste",caste());t.putBoolean("LeafLoad",carrying());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);home=t.contains("ColonyHome")?BlockPos.of(t.getLong("ColonyHome")):null;inside=t.getBoolean("ColonyInside");entityData.set(CASTE,Math.max(0,Math.min(2,t.getInt("Caste"))));carry(t.getBoolean("LeafLoad"));}
    private final class ColonyGoal extends Goal {
        private BlockPos destination;private int rethink,repath,wait,stuck,burrowing;private Vec3 previous;
        ColonyGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return getTarget()==null;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(!(level() instanceof ServerLevel l))return;
            var c=colony();
            if(tickCount%40==0&&c!=null){
                // Remember queen intrusion even after the player backs away or reloads the world.
                var queen=inside?home.offset(64,0,4):home.offset(0,-3,1);
                for(var p:l.getEntitiesOfClass(Player.class,new AABB(queen).inflate(inside?12:1.5),p->!p.isCreative()&&!p.isSpectator()))c.remember(p.getUUID());
                var threat=l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(inside?24:12),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&c.hostile(p));
                if(!threat.isEmpty()){setTarget(threat.get(0));return;}
            }
            if(caste()==2){getNavigation().stop();return;}
            if(burrowing>0){
                if(--burrowing==0&&c!=null){c.delivered(false);carry(false);moveTo(home.getX()+.5,home.getY(),home.getZ()+ColonyRules.radius(c.stage(),species()==3)+2.5,getYRot(),0);destination=null;wait=30;}
                return;
            }
            if(wait-->0)return;
            if(tickCount>=rethink||destination==null){
                rethink=tickCount+100;stuck=0;
                if(c==null){destination=blockPosition().offset(random.nextInt(7)-3,0,random.nextInt(7)-3);}
                else if(caste()==1)destination=home.offset(random.nextInt(13)-6,0,random.nextInt(13)-6);
                else destination=carrying()?dropSite(c):c.food(l);
                if(destination==null){wait=60;return;}
            }
            if(!ColonyCoreBlockEntity.loaded(l,destination)){destination=null;wait=60;return;}
            Vec3 target=Vec3.atBottomCenterOf(destination);double reach=enlarged()?3:1.2;
            if(position().distanceToSqr(target)<reach*reach){
                getNavigation().stop();
                if(c!=null&&caste()==0){
                    if(carrying()){
                        if(enlarged()&&!c.giant()&&!inside){
                            // Oversized outsiders leave physical fragments outside and cannot feed the inner fungus.
                            if(random.nextInt(4)==0&&l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(destination).inflate(3)).size()<4)spawnAtLocation(new ItemStack(LittleLeafContent.LEAF_FRAGMENT.get(),4));
                        } else if(!enlarged()&&!inside){
                            moveTo(home.getX()+.5,home.getY()-3,home.getZ()+.5,getYRot(),0);getNavigation().stop();burrowing=40;destination=null;return;
                        } else {c.delivered(enlarged());if(species()==2&&random.nextInt(16)==0)spawnAtLocation(LittleLeafContent.HONEYDEW.get());}
                        carry(false);
                    }else c.cut(l,destination,ColonyInsectEntity.this);
                }
                destination=null;wait=40;return;
            }
            if(previous!=null&&position().distanceToSqr(previous)<.0002)stuck++;else stuck=0;previous=position();
            if(stuck>80){destination=null;wait=80;getNavigation().stop();return;}
            if(tickCount>=repath){getNavigation().moveTo(target.x,target.y,target.z,carrying()?.8:1);repath=tickCount+30;}
            getLookControl().setLookAt(target.x,target.y,target.z,20,20);
        }
        private BlockPos dropSite(ColonyCoreBlockEntity c){if(inside)return home.offset(26,0,4);int r=ColonyRules.radius(c.stage(),species()==3);return enlarged()?home.offset(r+2,0,0):home.offset(0,0,1);}
        @Override public void stop(){getNavigation().stop();destination=null;}
    }
}
