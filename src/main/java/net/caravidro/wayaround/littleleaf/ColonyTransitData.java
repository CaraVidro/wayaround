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
    private final Map<GlobalPos,Link> links=new LinkedHashMap<>();
    public static ColonyTransitData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ColonyTransitData::new,ColonyTransitData::load),"wayaround_little_leaf_transit");}
    public Link activate(ResourceKey<Level> dimension,BlockPos source){var key=GlobalPos.of(dimension,source.immutable());var existing=links.get(key);if(existing!=null)return existing;int index=links.size();if(index>=262144)throw new IllegalStateException("Colony interior capacity reached");var link=new Link(key,index%512,index/512);links.put(key,link);setDirty();return link;}
    public Link find(ResourceKey<Level> dim,BlockPos source){return links.get(GlobalPos.of(dim,source));}
    public static ColonyTransitData load(CompoundTag t,HolderLookup.Provider r){var d=new ColonyTransitData();for(var entry:t.getList("Links",Tag.TAG_COMPOUND)){var c=(CompoundTag)entry;var id=ResourceLocation.tryParse(c.getString("Dimension"));if(id==null)continue;var key=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(c.getLong("Source")));int x=c.getInt("CellX"),z=c.getInt("CellZ");if(x<0||x>=512||z<0||z>=512)continue;d.links.put(key,new Link(key,x,z));}return d;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){var list=new ListTag();for(var link:links.values()){var c=new CompoundTag();c.putString("Dimension",link.source.dimension().location().toString());c.putLong("Source",link.source.pos().asLong());c.putInt("CellX",link.cellX);c.putInt("CellZ",link.cellZ);list.add(c);}t.put("Links",list);return t;}
}
