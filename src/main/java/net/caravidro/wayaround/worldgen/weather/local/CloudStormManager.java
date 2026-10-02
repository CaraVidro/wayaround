package net.caravidro.wayaround.worldgen.weather.local;

import java.util.HashMap;
import java.util.Map;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.CloudStormS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Cosmetic lightning: no block changes, fire ignition or lightning entities. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class CloudStormManager {
    private static final Map<ServerLevel,Long> NEXT=new HashMap<>();
    private static final Map<ServerLevel,Map<Long,Long>> CELL_COOLDOWN=new HashMap<>();
    private CloudStormManager(){}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){NEXT.clear();CELL_COOLDOWN.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(e.getServer().getTickCount()%40!=0||!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_WEATHER)
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.PROCEDURAL_CLOUDS))return;
        for(ServerLevel level:e.getServer().getAllLevels()){
            if(!level.dimension().equals(Level.OVERWORLD)||level.getGameTime()<NEXT.getOrDefault(level,0L))continue;
            int inspected=0;boolean emitted=false;
            var cooldown=CELL_COOLDOWN.computeIfAbsent(level,unused->new HashMap<>());
            cooldown.entrySet().removeIf(entry->entry.getValue()<level.getGameTime()-1200);
            for(ServerPlayer player:level.players()){
                if(inspected++>=12||emitted)break;
                if(level.getBiome(player.blockPosition()).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET))continue;
                for(var cell:LocalWeatherField.nearbyCells(level,player.getX(),player.getZ(),level.getGameTime(),580)){
                    if(cell.storm()<.64||cooldown.getOrDefault(cell.id(),0L)>level.getGameTime())continue;
                    if(!level.hasChunkAt(BlockPos.containing(cell.x(),level.getSeaLevel(),cell.z())))continue;
                    if(level.random.nextFloat()>.07F*cell.storm())continue;
                    int kind=level.random.nextFloat()<.30F?1:0;
                    if(!emit(level,cell,kind))continue;
                    cooldown.put(cell.id(),level.getGameTime()+500+level.random.nextInt(500));
                    emitted=true;break;
                }
            }
            if(emitted)NEXT.put(level,level.getGameTime()+160+level.random.nextInt(200));
        }
    }
    public static boolean force(ServerPlayer player,int kind){
        ServerLevel level=player.serverLevel();
        if(!level.dimension().equals(Level.OVERWORLD)||!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_WEATHER)
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.PROCEDURAL_CLOUDS))return false;
        var cells=LocalWeatherField.nearbyCells(level,player.getX(),player.getZ(),level.getGameTime(),480);
        var best=cells.stream().filter(c->level.hasChunkAt(BlockPos.containing(c.x(),level.getSeaLevel(),c.z())))
                .min(java.util.Comparator.comparingDouble(c->Math.pow(c.x()-player.getX(),2)+Math.pow(c.z()-player.getZ(),2))).orElse(null);
        if(best==null)return false;
        return emit(level,best,kind);
    }
    private static boolean emit(ServerLevel level,LocalWeatherField.CloudCell cell,int kind){
        double x=cell.x()+(level.random.nextDouble()-.5)*cell.radius()*.55;
        double z=cell.z()+(level.random.nextDouble()-.5)*cell.radius()*.55;
        BlockPos column=BlockPos.containing(x,level.getSeaLevel(),z);
        if(!level.hasChunkAt(column)) {
            x=cell.x();z=cell.z();column=BlockPos.containing(x,level.getSeaLevel(),z);
            if(!level.hasChunkAt(column))return false;
        }
        int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING,column.getX(),column.getZ());
        var p=new CloudStormS2CPayload(cell.id(),x,cell.y()+12,z,ground,kind,.65F+cell.storm()*.35F);
        for(ServerPlayer player:level.players())if(player.distanceToSqr(x,player.getY(),z)<800*800)PacketDistributor.sendToPlayer(player,p);
        return true;
    }
}
