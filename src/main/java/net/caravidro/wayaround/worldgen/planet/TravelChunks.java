package net.caravidro.wayaround.worldgen.planet;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Explicit travel destinations only. Four async chunk requests/tick; never join a generation future. */
public final class TravelChunks {
    private static final TicketType<ChunkPos> TICKET=TicketType.create("wayaround_travel",Comparator.comparingLong(ChunkPos::toLong),600);
    private static final ArrayDeque<Batch> QUEUE=new ArrayDeque<>();
    private static final Set<Batch> ACTIVE=new HashSet<>();
    public static final class Batch {
        final ServerLevel level;final List<ChunkPos> chunks;final Consumer<Boolean> done;
        int requested,completed;boolean failed,canceled,delivered;final long deadline;
        Batch(ServerLevel level,List<ChunkPos> chunks,Consumer<Boolean> done){this.level=level;this.chunks=chunks;this.done=done;deadline=level.getGameTime()+600;}
        private void deliver(boolean success){if(delivered||canceled)return;delivered=true;done.accept(success);}
        public void release(){canceled=true;QUEUE.remove(this);ACTIVE.remove(this);for(int i=0;i<requested;i++){var p=chunks.get(i);level.getChunkSource().removeRegionTicket(TICKET,p,2,p);}}
    }
    public static Batch request(ServerLevel level,int x,int z,int radius,Consumer<Boolean> done) {
        if(ACTIVE.size()>=16)return null;
        var list=new ArrayList<ChunkPos>();
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
            int cx=(x>>4)+dx,cz=(z>>4)+dz;
            // Travel requests never generate beyond the canonical finite domain.
            if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.FINITE_WORLD)
                    ||cx>=-PlanetMath.HALF/16&&cx<PlanetMath.HALF/16&&cz>=-PlanetMath.HALF/16&&cz<PlanetMath.HALF/16)list.add(new ChunkPos(cx,cz));
        }
        var batch=new Batch(level,list,done);ACTIVE.add(batch);QUEUE.addLast(batch);return batch;
    }
    public static void tick() {
        for(var batch:List.copyOf(ACTIVE))if(batch.level.getGameTime()>batch.deadline&&!batch.canceled){batch.deliver(false);batch.release();}
        for(int budget=0;budget<4&&!QUEUE.isEmpty();budget++) {
            var batch=QUEUE.pollFirst();if(batch.canceled)continue;
            if(batch.requested>=batch.chunks.size())continue;
            var pos=batch.chunks.get(batch.requested++);batch.level.getChunkSource().addRegionTicket(TICKET,pos,2,pos);
            var future=batch.level.getChunkSource().getChunkFuture(pos.x,pos.z,ChunkStatus.FULL,true);
            future.whenComplete((result,error)->batch.level.getServer().execute(()->{
                if(batch.canceled)return;
                batch.failed|=error!=null||!batch.level.hasChunk(pos.x,pos.z);batch.completed++;
                if(batch.completed==batch.chunks.size())batch.deliver(!batch.failed);
            }));
            if(batch.requested<batch.chunks.size())QUEUE.addLast(batch);
        }
    }
    public static void clear(){for(var batch:List.copyOf(ACTIVE))batch.release();QUEUE.clear();}
    private TravelChunks() {}
}
