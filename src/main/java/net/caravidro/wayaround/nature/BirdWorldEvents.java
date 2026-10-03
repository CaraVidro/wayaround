package net.caravidro.wayaround.nature;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=WayAround.MODID)
public final class BirdWorldEvents {
    private static final Map<ServerLevel,Long> NEXT_FLOCK=new WeakHashMap<>();
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){NEXT_FLOCK.clear();AppleTreeBlockEntity.clear();ParrotMimicManager.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        ParrotMimicManager.tick(e.getServer());
        if(e.getServer().getTickCount()%400!=0||!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION))return;
        ServerLevel l=e.getServer().overworld();int budget=8;
        for(ServerPlayer p:l.players()){
            if(budget--<=0)break;
            var biome=l.getBiome(p.blockPosition());
            if(!biome.is(BiomeTags.IS_FOREST)&&!biome.is(BiomeTags.IS_JUNGLE))continue;
            if(l.getEntitiesOfClass(WoodlandBirdEntity.class,p.getBoundingBox().inflate(64)).size()<5){
                int x=p.getBlockX()+l.random.nextInt(49)-24,z=p.getBlockZ()+l.random.nextInt(49)-24;
                BlockPos column=new BlockPos(x,0,z);
                if(l.hasChunkAt(column)){
                    int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z);
                    BlockPos spot=new BlockPos(x,y,z);
                    if(l.getBlockState(spot).isAir()&&l.getFluidState(spot.below()).isEmpty()){
                        var type=biome.is(BiomeTags.IS_JUNGLE)?NatureContent.PARROT.get():l.random.nextBoolean()?NatureContent.HUMMINGBIRD.get():NatureContent.THRUSH.get();
                        WoodlandBirdEntity bird=type.create(l);
                        if(bird!=null){bird.moveTo(x+.5,y+.5,z+.5,l.random.nextFloat()*360,0);if(l.noCollision(bird))l.addFreshEntity(bird);}
                    }
                }
            }
            long now=l.getGameTime();
            if(now>=NEXT_FLOCK.getOrDefault(l,now+6000L)){migration(p);NEXT_FLOCK.put(l,now+6000+l.random.nextInt(6000));}
            else NEXT_FLOCK.putIfAbsent(l,now+6000L);
        }
    }
    public static void migration(ServerPlayer p){
        var l=p.serverLevel();long seed=l.random.nextLong();
        var packet=new NatureAmbientPayload(0,0,seed,p.getX(),Math.max(p.getY()+45,150),p.getZ(),new byte[0]);
        for(var viewer:l.players())if(viewer.distanceToSqr(p)<256*256)PacketDistributor.sendToPlayer(viewer,packet);
    }
}
