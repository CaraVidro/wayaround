package net.caravidro.wayaround.ecology;

import java.util.*;
import net.caravidro.wayaround.network.KrakenShakeS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared server physics/pressure/light leases and client network interpolation. */
public abstract class AbyssVehicleEntity extends Entity {
    private static final EntityDataAccessor<Integer> PRESSURE = SynchedEntityData.defineId(AbyssVehicleEntity.class, EntityDataSerializers.INT);
    private static final Map<Level, Map<BlockPos, Lease>> LIGHTS = new WeakHashMap<>();
    private record Lease(BlockState original, Set<UUID> owners) {}
    private final Map<BlockPos, Integer> lampNodes = new HashMap<>();
    private Vec3 lampOrigin;
    private float lampYaw, lampPitch;
    private int lastImpact = -100, lastBeep = -100;
    private int lerpSteps;
    private double targetX, targetY, targetZ;
    private float targetYaw, targetPitch;

    protected AbyssVehicleEntity(EntityType<?> type, Level level) { super(type, level); blocksBuilding = true; }
    protected abstract boolean capsule();
    public final int pressureExposure() { return entityData.get(PRESSURE); }
    public final void setPressureExposure(int ticks) { entityData.set(PRESSURE, Mth.clamp(ticks, 0, OceanPressure.FAILURE)); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(PRESSURE, 0); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { setPressureExposure(tag.getInt("PressureExposure")); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("PressureExposure", pressureExposure()); }
    protected final boolean inWaterColumn() { return isInWater() || level().getFluidState(blockPosition()).is(FluidTags.WATER); }
    protected final Vec3 constrainAscent(Vec3 motion) {
        // The drive must not turn into a flying machine at the fluid boundary.
        if (motion.y > 0 && !level().getFluidState(BlockPos.containing(getX(), getY()+0.35, getZ())).is(FluidTags.WATER))
            return new Vec3(motion.x, Math.min(0, level().getSeaLevel()-0.85-getY()), motion.z);
        return motion;
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        targetX=x; targetY=y; targetZ=z; targetYaw=yaw; targetPitch=pitch; lerpSteps=Math.max(3, Math.min(6,steps));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double fraction=1.0/lerpSteps--;
                setPos(Mth.lerp(fraction,getX(),targetX),Mth.lerp(fraction,getY(),targetY),Mth.lerp(fraction,getZ(),targetZ));
                setYRot(getYRot()+(float)fraction*Mth.wrapDegrees(targetYaw-getYRot()));
                setXRot(Mth.lerp((float)fraction,getXRot(),targetPitch));
            }
            return;
        }
        ServerLevel server=(ServerLevel)level();
        boolean water=inWaterColumn();
        int exposure=OceanPressure.advance(pressureExposure(), level().getSeaLevel()-getY(), water,capsule());
        setPressureExposure(exposure);
        if (exposure >= OceanPressure.FAILURE) {
            sound(SoundEvents.ANVIL_DESTROY, 2.3F, .35F);
            server.sendParticles(ParticleTypes.BUBBLE, getX(),getY()+.6,getZ(),36,1.4,.7,1.4,.08);
            shake(30,.9F);
            for (Entity passenger : List.copyOf(getPassengers())) {
                if (passenger instanceof LivingEntity living) living.hurt(level().damageSources().genericKill(), Float.MAX_VALUE);
            }
            dropCargo(); discard(); return;
        }
        if (exposure>=OceanPressure.WARNING && tickCount%100==0) {
            sound(SoundEvents.IRON_DOOR_CLOSE,.9F,.45F); beep();
            shake(18,exposure>=OceanPressure.CRITICAL?.25F:.09F);
        }
        if (water && tickCount%160==0) sound(SoundEvents.CONDUIT_AMBIENT,.4F,capsule()?.55F:.7F);
        if (water && tickCount%100==40 && getDeltaMovement().lengthSqr()>.001) sound(SoundEvents.PISTON_CONTRACT,.25F,.6F);
        if (water && tickCount%20==0) {
            for(int dy=1;dy<=8;dy++) {
                BlockPos floor=blockPosition().below(dy);
                if(!level().isLoaded(floor)) break;
                if(!level().getBlockState(floor).getCollisionShape(level(),floor).isEmpty()) { if(tickCount-lastBeep>=100) beep(); break; }
            }
        }
        updateLamp(water);
    }
    protected void dropCargo() {}
    private void beep() { lastBeep=tickCount; sound(SoundEvents.NOTE_BLOCK_PLING.value(),.55F,pressureExposure()>=OceanPressure.WARNING?.7F:1.4F); }
    public final void shake(int ticks,float strength) {
        for(Entity rider:getPassengers()) if(rider instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player,new KrakenShakeS2CPayload(ticks,strength));
    }
    protected final void sound(SoundEvent sound,float volume,float pitch) { level().playSound(null,getX(),getY(),getZ(),sound,SoundSource.NEUTRAL,volume,pitch); }
    protected final void impact(Vec3 intended) {
        if (!level().isClientSide && inWaterColumn() && (verticalCollision || horizontalCollision)
                && intended.lengthSqr()>.001 && tickCount-lastImpact>25) {
            lastImpact=tickCount; sound(SoundEvents.ANVIL_LAND,.7F,.55F); shake(10,.16F);
            ServerLevel server=(ServerLevel)level();
            BlockPos floor=blockPosition().below(); BlockState state=level().getBlockState(floor);
            if(state.getCollisionShape(level(),floor).isEmpty()) state=Blocks.GRAVEL.defaultBlockState();
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4;
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),getX()+Math.cos(a),getY()+.08,getZ()+Math.sin(a),0,Math.cos(a),.08,Math.sin(a),.12);
            }
        }
    }
    private void updateLamp(boolean water) {
        if (!water) { clearLamp(); return; }
        Map<BlockPos,Lease> leases=LIGHTS.computeIfAbsent(level(), ignored->new HashMap<>());
        if(tickCount%2==0 && (lampOrigin==null || lampOrigin.distanceToSqr(position())>.0144
                || Math.abs(Mth.wrapDegrees(getYRot()-lampYaw))>1 || Math.abs(getXRot()-lampPitch)>1)) {
            lampOrigin=position();lampYaw=getYRot();lampPitch=getXRot();
            Vec3 beam=Vec3.directionFromRotation(getXRot(),getYRot());
            Vec3 origin=position().add(0,.9,0);
            double range=capsule()?28:42;
            // Check every sampled section before raycasting; never load an absent chunk.
            for(double d=0;d<=range;d+=4) if(!level().isLoaded(BlockPos.containing(origin.add(beam.scale(d))))) { range=d-4;break; }
            Vec3 end=origin.add(beam.scale(Math.max(0,range)));
            BlockHitResult hit=level().clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(hit.getType()!=HitResult.Type.MISS) range=Math.max(0,origin.distanceTo(hit.getLocation())-.7);
            Set<BlockPos> fresh=new HashSet<>();
            for(double d=.5;d<=range;d+=4) {
                Vec3 center=origin.add(beam.scale(d));
                for(int side=-1;side<=1;side++) {
                    BlockPos pos=BlockPos.containing(center.add(beam.z*side,0,-beam.x*side));
                    if(!level().isLoaded(pos)) continue;
                    BlockState state=level().getBlockState(pos);
                    Lease lease=leases.get(pos);
                    if(lease==null) {
                        if(!state.is(Blocks.WATER) && !state.isAir()) continue;
                        lease=new Lease(state,new HashSet<>());leases.put(pos,lease);
                        level().setBlock(pos,Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,14).setValue(LightBlock.WATERLOGGED,state.is(Blocks.WATER)),3);
                    }
                    lease.owners().add(getUUID());fresh.add(pos);lampNodes.put(pos,Integer.MAX_VALUE);
                }
            }
            for(var entry:lampNodes.entrySet()) if(!fresh.contains(entry.getKey()) && entry.getValue()==Integer.MAX_VALUE) entry.setValue(tickCount+8);
        }
        // Keep old sources until the new lighting has propagated. Bound all leases.
        for(var it=lampNodes.entrySet().iterator();it.hasNext();) { var entry=it.next();if(entry.getValue()<=tickCount) { release(leases,entry.getKey());it.remove(); } }
    }
    private void release(Map<BlockPos,Lease> leases,BlockPos pos) {
        Lease lease=leases.get(pos);if(lease==null)return;
        lease.owners().remove(getUUID());
        if(lease.owners().isEmpty()) {
            if(level().isLoaded(pos) && level().getBlockState(pos).is(Blocks.LIGHT)) level().setBlock(pos,lease.original(),3);
            leases.remove(pos);
        }
    }
    private void clearLamp() { Map<BlockPos,Lease> leases=LIGHTS.get(level());if(leases!=null) for(BlockPos pos:lampNodes.keySet()) release(leases,pos);lampNodes.clear();lampOrigin=null; }
    @Override public void remove(RemovalReason reason) { if(!level().isClientSide) clearLamp();super.remove(reason); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty() && passenger instanceof net.minecraft.world.entity.player.Player; }
}
