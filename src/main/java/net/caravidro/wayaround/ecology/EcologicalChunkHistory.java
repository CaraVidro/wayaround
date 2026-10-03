package net.caravidro.wayaround.ecology;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.mining.DeferredMiningManager;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Chunk-entry work, independent of nearby players; never requests a missing chunk. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class EcologicalChunkHistory {
    private static final Map<ServerLevel,LinkedHashSet<Long>> PENDING=new IdentityHashMap<>();
    @SubscribeEvent public static void loaded(ChunkEvent.Load e){
        if(!(e.getLevel() instanceof ServerLevel l)||l.dimension()!=Level.OVERWORLD)return;
        long key=e.getChunk().getPos().toLong();
        l.getServer().execute(()->{
            var q=PENDING.computeIfAbsent(l,k->new LinkedHashSet<>());
            if(q.size()<4096)q.add(key);
        });
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload e){
        if(e.getLevel() instanceof ServerLevel l){long key=e.getChunk().getPos().toLong();l.getServer().execute(()->{var q=PENDING.get(l);if(q!=null)q.remove(key);});}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){PENDING.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(e.getServer().getTickCount()%20!=0)return;
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)&&!WorldFeatureRuntime.serverEnabled(WorldFeature.MINING_REGIONS))return;
        for(var entry:PENDING.entrySet()){
            var l=entry.getKey();var q=entry.getValue();int checks=16,work=2;
            var it=q.iterator();
            while(it.hasNext()&&checks-->0&&work>0){
                long key=it.next();var p=new ChunkPos(key);
                if(!l.hasChunk(p.x,p.z)){it.remove();continue;}
                boolean ready=true;
                for(int x=p.x-1;x<=p.x+1;x++)for(int z=p.z-1;z<=p.z+1;z++)if(!l.hasChunk(x,z))ready=false;
                if(!ready)continue;
                it.remove();work--;
                if(WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)){
                    DeepOceanSurfaceRepair.repair(l,p);
                    EcologicalSuccession.seedHistoricalChunk(l,p.x,p.z);
                }
                if(WorldFeatureRuntime.serverEnabled(WorldFeature.MINING_REGIONS))DeferredMiningManager.onLoadedTerrain(l,p.x,p.z);
            }
            // Rotate blocked edge entries so they cannot starve ready interior chunks.
            if(!q.isEmpty()){long first=q.iterator().next();q.remove(first);q.add(first);}
        }
    }
}
