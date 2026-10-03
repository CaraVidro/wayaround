package net.caravidro.wayaround.nexus;

import java.util.*;
import net.caravidro.wayaround.nexus.world.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Permanent transit records outlive reactors and chunk unloading. The interior cutoff is persisted globally. */
public final class NexusTransitData extends SavedData {
    public static final class Link {
        public final GlobalPos source;public final BlockPos destination;public boolean prepared;
        Link(GlobalPos source,BlockPos destination,boolean prepared){this.source=source;this.destination=destination.immutable();this.prepared=prepared;}
    }
    private final Map<GlobalPos,Link> links=new LinkedHashMap<>();
    private final List<Link> maintenance=new ArrayList<>();
    private final Set<BlockPos> occupied=new HashSet<>();
    private int cursor;
    private boolean disabled;
    public static NexusTransitData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(NexusTransitData::new,NexusTransitData::load),"wayaround_nexus_transit");}
    public boolean open(){return !disabled;}
    public Collection<Link> links(){return Collections.unmodifiableCollection(links.values());}
    public List<Link> maintenanceSlice() {
        var result=new ArrayList<Link>(16);int count=Math.min(16,maintenance.size());
        for(int i=0;i<count;i++){if(cursor>=maintenance.size())cursor=0;result.add(maintenance.get(cursor++));}return result;
    }
    public Link find(ResourceKey<Level> dimension,BlockPos base){return links.get(GlobalPos.of(dimension,base));}
    public void shutDown(){if(!disabled){disabled=true;setDirty();}}
    /** Operator recovery only; ordinary terminals cannot reopen a lost route. */
    public void reopen(){if(disabled){disabled=false;setDirty();}}
    public Link activate(ServerLevel source,BlockPos base) {
        GlobalPos key=GlobalPos.of(source.dimension(),base.immutable());Link old=links.get(key);if(old!=null)return old;
        var nexus=source.getServer().getLevel(NexusPortalManager.NEXUS);if(nexus==null)return null;
        long seed=NexusChunkGenerator.layoutSeed(nexus.getChunkSource().randomState());
        int cx=Math.floorDiv(base.getX(),NexusLayout.CELL),cz=Math.floorDiv(base.getZ(),NexusLayout.CELL);
        BlockPos destination=null;
        // Prefer a large room, walking outward deterministically without generating any candidate chunks.
        for(int radius=0;radius<=4&&destination==null;radius++)for(int dx=-radius;dx<=radius&&destination==null;dx++)for(int dz=-radius;dz<=radius;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            var room=NexusLayout.room(seed,cx+dx,cz+dz);var candidate=new BlockPos(room.x()-1,room.floor(),room.z()+2);
            if(room.area()>=1500 && !occupied.contains(candidate)){destination=candidate;break;}
        }
        // Dense multiplayer reactor clusters get a unique room rather than overwriting another threshold.
        if(destination==null) {
            for(int attempt=0;attempt<256;attempt++) {
                var room=NexusLayout.room(seed,cx+links.size()+5+attempt,cz);var candidate=new BlockPos(room.x()-1,room.floor(),room.z()+2);
                if(!occupied.contains(candidate)){destination=candidate;break;}
            }
            if(destination==null)return null;
        }
        var link=new Link(key,destination,false);links.put(key,link);maintenance.add(link);occupied.add(destination);setDirty();return link;
    }
    public static NexusTransitData load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new NexusTransitData();data.disabled=tag.getBoolean("Disabled");var list=tag.getList("Links",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) {
            var t=list.getCompound(i);var id=ResourceLocation.tryParse(t.getString("Dimension"));if(id==null)continue;
            var key=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(t.getLong("Base")));
            var link=new Link(key,BlockPos.of(t.getLong("Destination")),t.getBoolean("Prepared"));data.links.put(key,link);
        }
        data.maintenance.addAll(data.links.values());
        for(var link:data.links.values())data.occupied.add(link.destination);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        tag.putBoolean("Disabled",disabled);var list=new ListTag();
        for(var link:links.values()) {var t=new CompoundTag();t.putString("Dimension",link.source.dimension().location().toString());
            t.putLong("Base",link.source.pos().asLong());t.putLong("Destination",link.destination.asLong());t.putBoolean("Prepared",link.prepared);list.add(t);}
        tag.put("Links",list);return tag;
    }
}
