package net.caravidro.wayaround.war.outpost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** One network entity per screen/flare, with bounded particles and existing smoke LOD. */
public final class FieldCanisterEntity extends Entity {
    private static final EntityDataAccessor<Boolean> FLARE=SynchedEntityData.defineId(FieldCanisterEntity.class,EntityDataSerializers.BOOLEAN),ACTIVE=SynchedEntityData.defineId(FieldCanisterEntity.class,EntityDataSerializers.BOOLEAN);
    private int age,life;
    public FieldCanisterEntity(EntityType<? extends FieldCanisterEntity> t,Level l) {
        super(t,l);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(FLARE,false);
        b.define(ACTIVE,false);
    }
    public void configure(boolean flare) {
        entityData.set(FLARE,flare);
    }
    public boolean flare() {
        return entityData.get(FLARE);
    }
    public boolean active() {
        return entityData.get(ACTIVE);
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide)return;
        if(++age>1600||!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.enabled(level(),net.caravidro.wayaround.worldconfig.WorldFeature.WAR_WITHOUT_REASON)) {
            discard();
            return;
        }
        if(!active()) {
            Vec3 v=getDeltaMovement().add(0,-.04,0);
            if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(position().add(v)))) {
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            setDeltaMovement(v);
            move(MoverType.SELF,v);
            setDeltaMovement(getDeltaMovement().scale(.92));
            if(onGround()||isInWater()||age>=30) {
                entityData.set(ACTIVE,true);
                setDeltaMovement(Vec3.ZERO);
                setNoGravity(true);
            }
        }
        else {
            if(++life>(flare()?1200:400)) {
                discard();
                return;
            }
            if(level() instanceof ServerLevel s&&tickCount%5==0&&OutpostBudget.effect(s)) {
                s.sendParticles(flare()?ParticleTypes.FLAME:ParticleTypes.LARGE_SMOKE,getX(),getY()+.3,getZ(),flare()?2:8,flare()?.1:2.5,flare()?.15:1.2,flare()?.1:2.5,.02);
                if(!flare()&&tickCount%80==0) {
                    var coarse=net.caravidro.wayaround.worldgen.weather.fire.FireContent.SMOKE_VOLUME.get().create(s);
                    if(coarse!=null) {
                        coarse.setPos(position());
                        coarse.configure(3,120,.4F);
                        s.addFreshEntity(coarse);
                    }
                }
            }
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag n) {
        n.putBoolean("Flare",flare());
        n.putBoolean("Active",active());
        n.putInt("Age",age);
        n.putInt("Life",life);
    }
    @Override protected void readAdditionalSaveData(CompoundTag n) {
        configure(n.getBoolean("Flare"));
        entityData.set(ACTIVE,n.getBoolean("Active"));
        age=Math.clamp(n.getInt("Age"),0,1600);
        life=Math.clamp(n.getInt("Life"),0,1200);
        setNoGravity(active());
    }
}
