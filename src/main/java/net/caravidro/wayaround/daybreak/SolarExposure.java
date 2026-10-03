package net.caravidro.wayaround.daybreak;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SolarExposure {
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("wayaround","then_days_break"));
    private SolarExposure(){}
    public static boolean exposed(Entity entity) {
        if(!(entity.level() instanceof ServerLevel l)||!l.dimensionType().hasSkyLight()||l.dimensionType().hasFixedTime())return false;
        Vec3 from=entity.getEyePosition();BlockPos eye=BlockPos.containing(from);
        if(!l.hasChunkAt(eye)||!l.canSeeSky(eye))return false;
        double angle=l.getSunAngle(1),up=Math.cos(angle);
        if(up<=.04)return false;
        Vec3 direction=new Vec3(-Math.sin(angle),up,0);
        Vec3 end=from.add(direction.scale(128));
        BlockPos last=null;
        // Nearby directional shadow: maximum 128 cell probes and no missing-chunk lookup.
        for(int step=1;step<=128;step++){
            Vec3 point=from.add(direction.scale(step));BlockPos p=BlockPos.containing(point);
            if(p.getY()>=l.getMaxBuildHeight())return true;
            if(p.equals(last))continue;last=p;
            if(!l.hasChunkAt(p))return false;
            var state=l.getBlockState(p);
            if(state.getLightBlock(l,p)>0&&!state.getCollisionShape(l,p).isEmpty()
                    &&state.getCollisionShape(l,p).clip(from,end,p)!=null)return false;
        }
        return true;
    }
    public static void sample(Entity entity,boolean day) {
        if(!(entity.level() instanceof ServerLevel l)||!entity.isAlive())return;
        var data=entity.getPersistentData();int exposure=data.getInt("WayAroundSolarExposure");
        if(!day||!exposed(entity)){if(exposure>0)data.putInt("WayAroundSolarExposure",Math.max(0,exposure-2));return;}
        exposure=Math.min(20,exposure+1);data.putInt("WayAroundSolarExposure",exposure);
        entity.hurt(new DamageSource(l.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)),DaysBreakMath.damage(exposure));
        if(entity instanceof LivingEntity living&&living.isAlive())living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,DaysBreakMath.slowLevel(exposure),false,true));
    }
}
