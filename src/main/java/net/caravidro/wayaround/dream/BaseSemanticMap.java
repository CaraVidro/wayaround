package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class BaseSemanticMap {
    public enum ZoneType { BEDROOM, STORAGE, WORKSHOP, ACTIVITY_AREA, UNDERGROUND_AREA, UNKNOWN }
    public record SemanticZone(ZoneType type,BlockPos center,int radius,double importance,List<BlockPos> points){}
    public final HomeDetector.HomeRegion home;
    public final SemanticZone bedroom,storage,workshop;
    public final BlockPos mainBed,mainContainer,mainWorkstation;
    public final List<PlayerBehaviorProfile.Hotspot> hotspots;
    public final List<SemanticZone> zones;
    public final double undergroundPreference,jumpFrequency,sprintRatio;
    public BaseSemanticMap(PlayerBehaviorProfile profile,HomeDetector.HomeRegion home){
        this.home=home;
        var places=profile.places.values().stream().filter(p->inside(p.dimension(),p.pos(),home)).toList();
        var containers=places.stream().filter(p->p.kind().equals("container")).toList();
        var stations=places.stream().filter(p->Set.of("craft","furnace","machine").contains(p.kind())).toList();
        mainBed=home==null?null:home.bed();
        mainContainer=mostUsed(containers);mainWorkstation=mostUsed(stations);
        bedroom=mainBed==null?null:new SemanticZone(ZoneType.BEDROOM,mainBed,6,1,List.of(mainBed));
        storage=cluster(containers,ZoneType.STORAGE,3);workshop=cluster(stations,ZoneType.WORKSHOP,2);
        hotspots=profile.hotspots.values().stream().filter(h->inside(h.dimension,h.center(),home))
                .sorted(Comparator.comparingDouble(PlayerBehaviorProfile.Hotspot::importance).reversed()).toList();
        undergroundPreference=profile.undergroundRatio();jumpFrequency=profile.jumpsPerMinute();
        sprintRatio=Math.clamp((double)profile.sprintSeconds/Math.max(1,profile.seconds),0,1);
        List<SemanticZone> all=new ArrayList<>();if(bedroom!=null)all.add(bedroom);if(storage!=null)all.add(storage);if(workshop!=null)all.add(workshop);
        for(var h:hotspots.stream().limit(8).toList()){
            var a=profile.chunks.get(h.dimension+"/"+new ChunkPos(h.center()).toLong());
            boolean underground=a!=null&&a.seconds>0&&a.underground/(double)a.seconds>.6;
            all.add(new SemanticZone(underground?ZoneType.UNDERGROUND_AREA:ZoneType.ACTIVITY_AREA,h.center(),6,h.importance(),List.of(h.center())));
        }
        zones=List.copyOf(all);
    }
    private static boolean inside(String dimension,BlockPos pos,HomeDetector.HomeRegion home){
        if(home==null||!home.dimension().equals(dimension))return false;
        ChunkPos chunk=new ChunkPos(pos);
        return Math.abs(chunk.x-home.center().x)<=2&&Math.abs(chunk.z-home.center().z)<=2;
    }
    private static BlockPos mostUsed(List<PlayerBehaviorProfile.Place> places){
        return places.stream().max(Comparator.comparingLong(PlayerBehaviorProfile.Place::uses)).map(PlayerBehaviorProfile.Place::pos).orElse(null);
    }
    private static SemanticZone cluster(List<PlayerBehaviorProfile.Place> places,ZoneType type,int minimum){
        SemanticZone best=null;
        for(var seed:places){
            var group=places.stream().filter(p->p.pos().distSqr(seed.pos())<=64).toList();
            long importance=group.stream().mapToLong(PlayerBehaviorProfile.Place::uses).sum();
            if(group.size()>=minimum&&(best==null||importance>best.importance()))
                best=new SemanticZone(type,seed.pos(),8,importance,group.stream().map(PlayerBehaviorProfile.Place::pos).toList());
        }
        return best;
    }
    public BlockPos chooseContainer(PlayerBehaviorProfile profile,DreamRegion region){
        if(mainContainer!=null&&region.containers.contains(region.map(mainContainer)))return region.map(mainContainer);
        return profile.places.values().stream().filter(p->p.kind().equals("container")&&p.dimension().equals(region.source.dimension().location().toString()))
                .filter(p->region.containers.contains(region.map(p.pos())))
                .max(Comparator.comparingLong(PlayerBehaviorProfile.Place::uses)).map(p->region.map(p.pos()))
                .orElseGet(()->region.containers.stream().min(Comparator.comparingDouble(p->p.distSqr(region.copiedBed))).orElse(null));
    }
}
