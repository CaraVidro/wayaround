package net.caravidro.wayaround.dream;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;

/** Bounded gameplay aggregates, never a movement log or communication log. */
public final class PlayerBehaviorProfile {
    public static final int MAX_CHUNKS = 256, MAX_PLACES = 48;
    public static final int HOTSPOT_CELL = 8, MAX_HOTSPOTS = 64;
    public static final class Hotspot {
        public String dimension;
        public int cellX,cellY,cellZ;
        public long timeSpent,stationaryTime,visits;
        public BlockPos center(){return new BlockPos(cellX*8+4,cellY*8+4,cellZ*8+4);}
        public double importance(){return timeSpent+stationaryTime*.5+visits*10;}
    }
    public final Map<String,Hotspot> hotspots = new LinkedHashMap<>();
    private String lastHotspot;
    public static final class Activity {
        public String dimension;
        public long chunk, seconds, visits, containers, crafting, furnaces, machines, sleeps, placed, underground, stationary;
        public double ySum;
        public double score() {
            return Math.log1p(seconds) * 8 + Math.min(visits, 100) * 3 + Math.min(sleeps, 20) * 25
                    + Math.min(containers, 100) * 2 + Math.min(crafting + furnaces + machines, 100) * 3
                    + Math.min(placed, 400) * .3 + Math.log1p(stationary) * 3;
        }
    }
    public record Place(String dimension, BlockPos pos, String kind, long uses) {}
    public final Map<String, Activity> chunks = new LinkedHashMap<>();
    public final Map<String, Place> places = new LinkedHashMap<>();
    public long seconds, jumps, sprintSeconds, undergroundSeconds, containers, crafting, furnaces, machines, sleeps;
    public double ySum, distance;
    public Place lastBed;
    private String lastChunk;
    public Activity activity(String dimension, BlockPos pos) {
        long chunk = new ChunkPos(pos).toLong();
        String key = dimension + "/" + chunk;
        Activity a = chunks.get(key);
        if (a == null) {
            if (chunks.size() >= MAX_CHUNKS) {
                String weakest = chunks.entrySet().stream().min(Comparator.comparingDouble(e -> e.getValue().score())).orElseThrow().getKey();
                chunks.remove(weakest);
            }
            a = new Activity(); a.dimension = dimension; a.chunk = chunk; chunks.put(key, a);
        }
        return a;
    }
    public void sample(String dimension, BlockPos pos, double moved, boolean sprinting, boolean underground) {
        Activity a = activity(dimension, pos);
        String key = dimension + "/" + a.chunk;
        if (!key.equals(lastChunk)) a.visits++;
        lastChunk = key;
        a.seconds++; seconds++; a.ySum += pos.getY(); ySum += pos.getY();
        distance += Math.max(0, Math.min(moved, 20));
        if (moved < .1) a.stationary++;
        if (sprinting) sprintSeconds++;
        if (underground) { a.underground++; undergroundSeconds++; }
        int cx=Math.floorDiv(pos.getX(),HOTSPOT_CELL),cy=Math.floorDiv(pos.getY(),HOTSPOT_CELL),cz=Math.floorDiv(pos.getZ(),HOTSPOT_CELL);
        String hotkey=dimension+"/"+cx+"/"+cy+"/"+cz;
        Hotspot h=hotspots.get(hotkey);
        if(h==null){
            if(hotspots.size()>=MAX_HOTSPOTS)hotspots.remove(hotspots.entrySet().stream()
                    .min(Comparator.comparingDouble(e->e.getValue().importance())).orElseThrow().getKey());
            h=new Hotspot();h.dimension=dimension;h.cellX=cx;h.cellY=cy;h.cellZ=cz;hotspots.put(hotkey,h);
        }
        h.timeSpent++;if(moved<.1)h.stationaryTime++;if(!hotkey.equals(lastHotspot))h.visits++;
        lastHotspot=hotkey;
    }
    public void interact(String dimension, BlockPos pos, String kind) {
        Activity a = activity(dimension, pos);
        switch (kind) {
            case "bed" -> { a.sleeps++; sleeps++; }
            case "craft" -> { a.crafting++; crafting++; }
            case "furnace" -> { a.furnaces++; furnaces++; }
            case "machine" -> { a.machines++; machines++; }
            default -> { a.containers++; containers++; }
        }
        String key = dimension + "/" + pos.asLong() + "/" + kind;
        Place old = places.get(key);
        Place place = new Place(dimension, pos.immutable(), kind, old == null ? 1 : old.uses() + 1);
        if (places.size() >= MAX_PLACES && old == null) {
            places.remove(places.entrySet().stream().min(Comparator.comparingLong(e -> e.getValue().uses())).orElseThrow().getKey());
        }
        places.put(key, place);
        if (kind.equals("bed")) lastBed = place;
    }
    public double preferredY() { return seconds == 0 ? 64 : ySum / seconds; }
    public double undergroundRatio() { return seconds == 0 ? 0 : (double) undergroundSeconds / seconds; }
    public double jumpsPerMinute() { return jumps * 60.0 / Math.max(1, seconds); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seconds", seconds); tag.putLong("jumps", jumps); tag.putLong("sprint", sprintSeconds);
        tag.putLong("underground", undergroundSeconds); tag.putDouble("y", ySum); tag.putDouble("distance", distance);
        tag.putLong("containers", containers); tag.putLong("crafting", crafting); tag.putLong("furnaces", furnaces);
        tag.putLong("machines", machines); tag.putLong("sleeps", sleeps);
        ListTag activity = new ListTag();
        for (Activity a : chunks.values()) {
            CompoundTag n = new CompoundTag(); n.putString("dimension", a.dimension); n.putLong("chunk", a.chunk);
            n.putLongArray("counts", new long[]{a.seconds,a.visits,a.containers,a.crafting,a.furnaces,a.machines,a.sleeps,a.placed,a.underground,a.stationary});
            n.putDouble("y", a.ySum); activity.add(n);
        }
        tag.put("chunks", activity);
        ListTag points = new ListTag();
        for (Place p : places.values()) points.add(savePlace(p));
        tag.put("places", points);
        if (lastBed != null) tag.put("bed", savePlace(lastBed));
        ListTag hot=new ListTag();
        for(Hotspot h:hotspots.values()){
            CompoundTag n=new CompoundTag();n.putString("dimension",h.dimension);
            n.putIntArray("cell",new int[]{h.cellX,h.cellY,h.cellZ});
            n.putLongArray("counts",new long[]{h.timeSpent,h.stationaryTime,h.visits});hot.add(n);
        }
        tag.put("hotspots",hot);
        return tag;
    }
    private static CompoundTag savePlace(Place p) {
        CompoundTag n = new CompoundTag(); n.putString("dimension", p.dimension()); n.putLong("pos", p.pos().asLong());
        n.putString("kind", p.kind()); n.putLong("uses", p.uses()); return n;
    }
    private static Place loadPlace(CompoundTag n) {
        return new Place(n.getString("dimension"), BlockPos.of(n.getLong("pos")), n.getString("kind"), n.getLong("uses"));
    }
    public static PlayerBehaviorProfile load(CompoundTag tag) {
        PlayerBehaviorProfile p = new PlayerBehaviorProfile();
        p.seconds=tag.getLong("seconds"); p.jumps=tag.getLong("jumps"); p.sprintSeconds=tag.getLong("sprint");
        p.undergroundSeconds=tag.getLong("underground"); p.ySum=tag.getDouble("y"); p.distance=tag.getDouble("distance");
        p.containers=tag.getLong("containers"); p.crafting=tag.getLong("crafting"); p.furnaces=tag.getLong("furnaces");
        p.machines=tag.getLong("machines"); p.sleeps=tag.getLong("sleeps");
        for (Tag t : tag.getList("chunks", Tag.TAG_COMPOUND)) {
            CompoundTag n=(CompoundTag)t; long[] c=n.getLongArray("counts"); if(c.length!=10) continue;
            ChunkPos cp=new ChunkPos(n.getLong("chunk")); Activity a=p.activity(n.getString("dimension"), cp.getWorldPosition());
            a.seconds=c[0];a.visits=c[1];a.containers=c[2];a.crafting=c[3];a.furnaces=c[4];a.machines=c[5];
            a.sleeps=c[6];a.placed=c[7];a.underground=c[8];a.stationary=c[9];a.ySum=n.getDouble("y");
        }
        for (Tag t : tag.getList("places", Tag.TAG_COMPOUND)) {
            if(p.places.size()>=MAX_PLACES) break; Place point=loadPlace((CompoundTag)t);
            p.places.put(point.dimension()+"/"+point.pos().asLong()+"/"+point.kind(),point);
        }
        if(tag.contains("bed")) p.lastBed=loadPlace(tag.getCompound("bed"));
        for(Tag t:tag.getList("hotspots",Tag.TAG_COMPOUND)){
            if(p.hotspots.size()>=MAX_HOTSPOTS)break;
            CompoundTag n=(CompoundTag)t;int[] cell=n.getIntArray("cell");long[] counts=n.getLongArray("counts");
            if(cell.length!=3||counts.length!=3)continue;
            Hotspot h=new Hotspot();h.dimension=n.getString("dimension");h.cellX=cell[0];h.cellY=cell[1];h.cellZ=cell[2];
            h.timeSpent=counts[0];h.stationaryTime=counts[1];h.visits=counts[2];
            p.hotspots.put(h.dimension+"/"+h.cellX+"/"+h.cellY+"/"+h.cellZ,h);
        }
        return p;
    }
}
