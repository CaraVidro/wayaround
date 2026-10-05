package net.caravidro.wayaround.worldgen.weather.local;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.caravidro.wayaround.environment.EnvironmentalFieldClientCache;
import net.caravidro.wayaround.environment.EnvironmentalFields;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Biome and water samples are cached; never requests an unloaded chunk. */
public final class RegionalCloudClimate {
    private record Climate(float humidity,long sampled) {}
    private static final Map<Level,LinkedHashMap<Long,Climate>> CACHE=new WeakHashMap<>();
    private RegionalCloudClimate() {}
    public static synchronized float humidity(Level level,double x,double z){
        if(level==null)return .55F;
        if(level instanceof ServerLevel server)return EnvironmentalFields.humidity(server,x,z);
        var synced=EnvironmentalFieldClientCache.get(x,z);
        if(synced!=null)return synced.humidity();
        int gx=Math.floorDiv((int)Math.floor(x),64),gz=Math.floorDiv((int)Math.floor(z),64);
        long key=((long)gx<<32)^(gz&0xffffffffL);
        var cache=CACHE.computeIfAbsent(level,unused->new LinkedHashMap<>());
        var old=cache.get(key);long time=level.getGameTime();
        if(old!=null && time-old.sampled<1200)return old.humidity;
        BlockPos probe=new BlockPos(gx*64+32,level.getSeaLevel()+2,gz*64+32);
        if(!level.hasChunkAt(probe))return old==null?.55F:old.humidity;
        var biome=level.getBiome(probe);
        String name=biome.unwrapKey().map(k->k.location().getPath()).orElse("");
        float humidity=.55F;
        if(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_RIVER))humidity=.96F;
        else if(name.contains("desert")||name.contains("badlands"))humidity=.10F;
        else if(name.contains("swamp")||name.contains("jungle"))humidity=.86F;
        else if(!biome.value().hasPrecipitation())humidity=.18F;
        int water=0,loaded=0;
        for(int dx=-48;dx<=48;dx+=48)for(int dz=-48;dz<=48;dz+=48){
            BlockPos column=probe.offset(dx,0,dz);
            if(!level.hasChunkAt(column))continue;
            loaded++;
            int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,column.getX(),column.getZ())-1;
            if(level.getFluidState(new BlockPos(column.getX(),y,column.getZ())).is(FluidTags.WATER))water++;
        }
        // A desert lake locally raises humidity; the wider desert stays sparse.
        if(loaded>0 && water>0)humidity=Math.max(humidity,.20F+.75F*water/loaded);
        if(cache.size()>=2048)cache.remove(cache.keySet().iterator().next());
        cache.put(key,new Climate(humidity,time));return humidity;
    }
    public static LocalWeatherField.CloudCell adapt(Level level,LocalWeatherField.CloudCell cell){
        if(level==null)return cell;
        float humidity=humidity(level,cell.x(),cell.z());
        var synced=level instanceof ServerLevel?null:EnvironmentalFieldClientCache.get(cell.x(),cell.z());
        float cloudWater=level instanceof ServerLevel server
                ? EnvironmentalFields.cloudWater(server,cell.x(),cell.z())
                : synced!=null
                ? synced.cloudWater()
                : Math.max(0.02F,humidity*.58F-.16F);
        float effective=(float)CloudStormMath.clamp(humidity*.62+cloudWater*.62,0,1);
        long hash=cell.id()^(cell.id()>>>29)^0x71D67FFFEDA60000L;
        double roll=(hash&0x1fffffffffffffL)/(double)0x20000000000000L;
        if(roll>CloudStormMath.cover(effective))return null;
        double waterBody=.78+.42*cloudWater;
        return new LocalWeatherField.CloudCell(cell.id(),cell.x(),cell.z(),cell.y(),
                cell.radius()*CloudStormMath.size(effective)*waterBody,
                CloudStormMath.storm(cell.storm(),effective));
    }
}
