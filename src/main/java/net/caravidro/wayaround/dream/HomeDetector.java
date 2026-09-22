package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class HomeDetector {
    public record HomeRegion(String dimension, ChunkPos center, List<ChunkPos> chunks, double confidence,
            BlockPos bed, List<BlockPos> containers, List<BlockPos> workstations, int preferredY) {}
    private HomeDetector() {}
    public static HomeRegion detect(PlayerBehaviorProfile profile) {
        PlayerBehaviorProfile.Activity best=null;
        double highest=0, total=profile.chunks.values().stream().mapToDouble(PlayerBehaviorProfile.Activity::score).sum();
        for(var candidate:profile.chunks.values()) {
            ChunkPos center=new ChunkPos(candidate.chunk); double score=0;
            for(var a:profile.chunks.values()) if(a.dimension.equals(candidate.dimension)
                    && near(center,new ChunkPos(a.chunk),1)) score+=a.score();
            if(score>highest) { highest=score; best=candidate; }
        }
        if(best==null) return null;
        String dimension=best.dimension; ChunkPos center=new ChunkPos(best.chunk);
        List<ChunkPos> chunks=new ArrayList<>(); List<BlockPos> containers=new ArrayList<>(), stations=new ArrayList<>();
        BlockPos bed=null; long bedUses=0; double y=0; long time=0;
        for(var a:profile.chunks.values()) if(a.dimension.equals(dimension)&&near(center,new ChunkPos(a.chunk),1)) {
            chunks.add(new ChunkPos(a.chunk)); y+=a.ySum; time+=a.seconds;
        }
        for(var p:profile.places.values()) if(p.dimension().equals(dimension)&&near(center,new ChunkPos(p.pos()),2)) {
            if(p.kind().equals("bed")&&p.uses()>bedUses) { bed=p.pos();bedUses=p.uses(); }
            else if(p.kind().equals("container")) containers.add(p.pos());
            else if(!p.kind().equals("bed")) stations.add(p.pos());
        }
        double confidence=Math.min(1,highest/200.0)*highest/Math.max(1,total);
        return new HomeRegion(dimension,center,List.copyOf(chunks),confidence,bed,List.copyOf(containers),
                List.copyOf(stations),time==0?64:(int)(y/time));
    }
    private static boolean near(ChunkPos a,ChunkPos b,int radius) {
        return Math.abs(a.x-b.x)<=radius&&Math.abs(a.z-b.z)<=radius;
    }
}
