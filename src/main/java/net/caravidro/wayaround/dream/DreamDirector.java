package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class DreamDirector {
    public enum DreamAnomalyType { OPEN_DOOR, CLOSE_DOOR, REMOVE_TORCH, CHANGE_BLOCK, CHEST_SOUND, FOOTSTEP_SOUND }
    private double instability=.02;
    private int normalTicks,cooldown;
    private DreamAnomalyType next=DreamAnomalyType.OPEN_DOOR;
    public double instability(){return instability;}
    public int cooldown(){return cooldown;}
    public void onActorHit(){instability=Math.min(.30,instability+.025);if(cooldown>200)cooldown-=60;}
    public void onBoundaryAttempt(){instability=Math.min(.30,instability+.01);}
    public void tick(ServerPlayer player,DreamSession session){
        if(session.state!=DreamState.NORMAL||!player.level().dimension().equals(DreamContent.DIMENSION))return;
        normalTicks++;
        if(normalTicks%200==0)instability=Math.min(.30,instability+.002);
        if(normalTicks==1){resetCooldown(player);return;}
        if(--cooldown>0)return;
        attempt(player,session,next);
        next=next==DreamAnomalyType.OPEN_DOOR?DreamAnomalyType.REMOVE_TORCH:DreamAnomalyType.OPEN_DOOR;
        resetCooldown(player);
    }
    private void resetCooldown(ServerPlayer player){
        cooldown=Math.max(200,200+player.getRandom().nextInt(501)-(int)(instability*120));
    }
    public boolean attempt(ServerPlayer player,DreamSession session,DreamAnomalyType type){
        if(session.state!=DreamState.NORMAL||player.serverLevel()!=session.region.target
                ||!session.region.target.dimension().equals(DreamContent.DIMENSION))return false;
        List<BlockPos> candidates=new ArrayList<>(type==DreamAnomalyType.OPEN_DOOR?session.region.doors:session.region.torches);
        candidates.sort(Comparator.comparingDouble(pos->importanceDistance(pos,session)));
        int checked=0;
        for(BlockPos pos:candidates){
            if(++checked>32)break;
            if(!session.region.contains(pos.getX(),pos.getZ())||!player.level().hasChunkAt(pos))continue;
            double distance=player.distanceToSqr(Vec3.atCenterOf(pos));
            if(distance<36||distance>1024||!unobserved(player,pos))continue;
            var state=player.level().getBlockState(pos);
            if(type==DreamAnomalyType.OPEN_DOOR&&state.getBlock() instanceof DoorBlock door&&!state.getValue(DoorBlock.OPEN)){
                if(!unobserved(player,pos.above()))continue;
                door.setOpen(null,player.level(),state,pos,true);
                return true;
            }
            if(type==DreamAnomalyType.REMOVE_TORCH&&state.getBlock() instanceof TorchBlock){
                player.level().setBlock(pos,Blocks.AIR.defaultBlockState(),3|32);
                return true;
            }
        }
        return false;
    }
    private double importanceDistance(BlockPos pos,DreamSession session){
        return session.semantics.zones.stream().filter(z->z.type()==BaseSemanticMap.ZoneType.BEDROOM
                        ||z.type()==BaseSemanticMap.ZoneType.STORAGE||z.type()==BaseSemanticMap.ZoneType.WORKSHOP)
                .mapToDouble(z->pos.distSqr(session.region.map(z.center()))).min().orElse(pos.distSqr(session.region.copiedBed));
    }
    /** Conservative: neither the front hemisphere nor any directly visible sample may change. */
    public static boolean unobserved(ServerPlayer player,BlockPos pos){
        Vec3 eye=player.getEyePosition();
        for(Vec3 target:List.of(Vec3.atCenterOf(pos),Vec3.atCenterOf(pos).add(.4,.4,.4),
                Vec3.atCenterOf(pos).add(-.4,.4,-.4),Vec3.atCenterOf(pos).add(.4,-.4,-.4),
                Vec3.atCenterOf(pos).add(-.4,-.4,.4))){
            Vec3 direction=target.subtract(eye);
            if(direction.lengthSqr()<36||player.getLookAngle().dot(direction.normalize())>-.15)return false;
            BlockHitResult hit=player.level().clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos))return false;
        }
        return true;
    }
}
