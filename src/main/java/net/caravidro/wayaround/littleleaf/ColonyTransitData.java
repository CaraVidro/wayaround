package net.caravidro.wayaround.littleleaf;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Stable, separate interior cells. Return links survive destruction of the surface mound. */
public final class ColonyTransitData extends SavedData {
    public record Link(GlobalPos source,int cellX,int cellZ){public BlockPos arrival(){return new BlockPos(cellX*128+24,32,cellZ*128+64);}public BlockPos core(){return arrival().offset(0,0,-7);}}
    public record Raider(int species,int caste,float health){}
    private final Set<GlobalPos> abandoned=new HashSet<>();private final Map<GlobalPos,List<Raider>> raids=new LinkedHashMap<>();
    public boolean abandoned(GlobalPos p){return abandoned.contains(p);}
    public void abandon(GlobalPos p){abandoned.add(p);raids.remove(p);setDirty();}
    public boolean queue(GlobalPos p,Raider raid){if(abandoned.contains(p)||raids.size()>=1024&&!raids.containsKey(p))return false;var list=raids.computeIfAbsent(p,k->new ArrayList<>());if(list.size()>=4)return false;list.add(raid);setDirty();return true;}
    public List<Raider> pending(GlobalPos p){return List.copyOf(raids.getOrDefault(p,List.of()));}
    public void consumed(GlobalPos p){var list=raids.get(p);if(list==null||list.isEmpty())return;list.remove(0);if(list.isEmpty())raids.remove(p);setDirty();}
    private final Map<GlobalPos,Link> links=new LinkedHashMap<>();
    public static ColonyTransitData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ColonyTransitData::new,ColonyTransitData::load),"wayaround_little_leaf_transit");}
    public Link activate(ResourceKey<Level> dimension,BlockPos source){var key=GlobalPos.of(dimension,source.immutable());var existing=links.get(key);if(existing!=null)return existing;int index=links.size();if(index>=262144)throw new IllegalStateException("Colony interior capacity reached");var link=new Link(key,index%512,index/512);links.put(key,link);setDirty();return link;}
    public Link find(ResourceKey<Level> dim,BlockPos source){return links.get(GlobalPos.of(dim,source));}
    public static ColonyTransitData load(CompoundTag t,HolderLookup.Provider r){var d=new ColonyTransitData();for(var entry:t.getList("Links",Tag.TAG_COMPOUND)){var c=(CompoundTag)entry;var id=ResourceLocation.tryParse(c.getString("Dimension"));if(id==null)continue;var key=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(c.getLong("Source")));int x=c.getInt("CellX"),z=c.getInt("CellZ");if(x<0||x>=512||z<0||z>=512)continue;d.links.put(key,new Link(key,x,z));}for(var q:t.getList("Abandoned",Tag.TAG_COMPOUND)){var p=readPos((CompoundTag)q);if(p!=null)d.abandoned.add(p);}
        for(var q:t.getList("Raids",Tag.TAG_COMPOUND)){var c=(CompoundTag)q;var p=readPos(c);if(p==null||d.raids.size()>=1024)continue;var list=new ArrayList<Raider>();for(var value:c.getList("Entities",Tag.TAG_COMPOUND)){var e=(CompoundTag)value;if(list.size()>=4)break;list.add(new Raider(Math.clamp(e.getInt("Species"),0,3),Math.clamp(e.getInt("Caste"),0,1),Math.clamp(e.getFloat("Health"),.01F,1F)));}if(!list.isEmpty())d.raids.put(p,list);}return d;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){var list=new ListTag();for(var link:links.values()){var c=new CompoundTag();c.putString("Dimension",link.source.dimension().location().toString());c.putLong("Source",link.source.pos().asLong());c.putInt("CellX",link.cellX);c.putInt("CellZ",link.cellZ);list.add(c);}t.put("Links",list);var dead=new ListTag();for(var p:abandoned)dead.add(writePos(p));t.put("Abandoned",dead);var queued=new ListTag();for(var e:raids.entrySet()){var c=writePos(e.getKey());var entities=new ListTag();for(var raid:e.getValue()){var q=new CompoundTag();q.putInt("Species",raid.species());q.putInt("Caste",raid.caste());q.putFloat("Health",raid.health());entities.add(q);}c.put("Entities",entities);queued.add(c);}t.put("Raids",queued);return t;}
    private static CompoundTag writePos(GlobalPos p){var t=new CompoundTag();t.putString("Dimension",p.dimension().location().toString());t.putLong("Source",p.pos().asLong());return t;}
    private static GlobalPos readPos(CompoundTag t){var id=ResourceLocation.tryParse(t.getString("Dimension"));return id==null?null:GlobalPos.of(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(t.getLong("Source")));}

}
