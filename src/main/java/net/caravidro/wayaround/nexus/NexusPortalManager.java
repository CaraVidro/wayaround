package net.caravidro.wayaround.nexus;

import java.util.Set;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class NexusPortalManager {
    public static final ResourceKey<Level> NEXUS=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"nexus"));
    private static final String RETURN_DIMENSION="WayAroundNexusReturnDimension",RETURN_BASE="WayAroundNexusReturnBase",
            COOLDOWN="WayAroundNexusPortalCooldown",WARN_COOLDOWN="WayAroundNexusPortalWarnCooldown",INSIDE="WayAroundNexusInside";
    public static void sync(ServerLevel source,BlockPos base,NexustorBaseBlockEntity reactor) {
        if(source.dimension().equals(NEXUS))return;
        var data=NexusTransitData.get(source.getServer());var link=data.find(source.dimension(),base);
        if(link==null && reactor.complete())link=data.activate(source,base);
        if(link==null)return;
        reactor.setPortalVisual(data.open());
        setPlane(source,sourceAnchor(base),data.open());
        var nexus=source.getServer().getLevel(NEXUS);
        if(nexus!=null&&link.prepared&&loadedPlane(nexus,link.destination))setPlane(nexus,link.destination,data.open());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){if(e.getServer().getTickCount()%5==0)refreshLoaded(e.getServer());}
    public static void refreshLoaded(MinecraftServer server) {
        var data=NexusTransitData.get(server);var nexus=server.getLevel(NEXUS);
        for(var link:data.maintenanceSlice()) {
            var source=server.getLevel(link.source.dimension());var anchor=sourceAnchor(link.source.pos());
            if(source!=null&&loadedPlane(source,anchor))setPlane(source,anchor,data.open());
            if(source!=null) {
                var base=link.source.pos();var chunk=source.getChunkSource().getChunkNow(base.getX()>>4,base.getZ()>>4);
                if(chunk!=null&&chunk.getBlockEntity(base) instanceof NexustorBaseBlockEntity reactor)reactor.setPortalVisual(data.open());
            }
            if(nexus!=null&&link.prepared&&loadedPlane(nexus,link.destination))setPlane(nexus,link.destination,data.open());
        }
    }
    private static boolean loadedPlane(ServerLevel level,BlockPos p) {
        return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null
                &&level.getChunkSource().getChunkNow((p.getX()+2)>>4,p.getZ()>>4)!=null;
    }
    public static void touch(Level level,BlockPos touched,Entity entity) {
        if(!(level instanceof ServerLevel server)||!(entity instanceof ServerPlayer player)||player.isPassenger())return;
        var p=player.getPersistentData();long now=player.server.overworld().getGameTime();if(now<p.getLong(COOLDOWN))return;
        var data=NexusTransitData.get(player.server);
        if(server.dimension().equals(NEXUS)) {
            var link=returnLink(player);
            if(link==null||!data.open()){warnTrapped(player,p,now);return;}
            var source=player.server.getLevel(link.source.dimension());if(source==null){warnTrapped(player,p,now);return;}
            // Travel explicitly loads its one destination. Ambient maintenance never loads distant chunks.
            source.getChunkAt(link.source.pos());setPlane(source,sourceAnchor(link.source.pos()),true);
            p.putBoolean(INSIDE,false);p.putLong(COOLDOWN,now+50);arrive(player,source,link.source.pos().offset(0,1,2),.78F);
            return;
        }
        if(!data.open())return;
        NexusTransitData.Link link=null;
        // Nine exact candidates replace a volume scan and still work if the original reactor was destroyed.
        for(int x=0;x<3&&link==null;x++)for(int y=0;y<3;y++){link=data.find(server.dimension(),touched.offset(1-x,-1-y,-4));if(link!=null)break;}
        if(link==null)return;var nexus=player.server.getLevel(NEXUS);if(nexus==null)return;
        prepareArrival(nexus,link,data);setPlane(nexus,link.destination,true);
        p.putString(RETURN_DIMENSION,link.source.dimension().location().toString());p.putLong(RETURN_BASE,link.source.pos().asLong());
        p.putLong(COOLDOWN,now+50);p.putBoolean(INSIDE,true);arrive(player,nexus,link.destination.offset(1,0,-3),.62F);NexusAdvancements.theNexus(player);
    }
    private static NexusTransitData.Link returnLink(ServerPlayer player) {
        var p=player.getPersistentData();var id=ResourceLocation.tryParse(p.getString(RETURN_DIMENSION));
        if(id==null||!p.contains(RETURN_BASE))return null;
        var key=ResourceKey.create(Registries.DIMENSION,id);var base=BlockPos.of(p.getLong(RETURN_BASE));var data=NexusTransitData.get(player.server);
        var link=data.find(key,base);
        // A saved visit from the old cavern version is evidence of a real, previously activated link.
        var source=player.server.getLevel(key);
        return link==null&&source!=null&&p.getBoolean(INSIDE)?data.activate(source,base):link;
    }
    private static void arrive(ServerPlayer player,ServerLevel level,BlockPos pos,float pitch) {
        player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),player.getYRot(),player.getXRot());
        level.playSound(null,pos,SoundEvents.PORTAL_TRAVEL,SoundSource.PLAYERS,1,pitch);
    }
    public static void prepareArrival(ServerLevel nexus,NexusTransitData.Link link,NexusTransitData data) {
        nexus.getChunkAt(link.destination);if(link.prepared)return;
        // A small, one-time cleared pad also makes arrivals safe in old saved cave chunks.
        for(int x=-2;x<=4;x++)for(int z=-4;z<=1;z++) {
            var floor=link.destination.offset(x,-1,z);nexus.setBlock(floor,Blocks.POLISHED_ANDESITE.defaultBlockState(),Block.UPDATE_CLIENTS);
            for(int y=0;y<=4;y++)nexus.setBlock(floor.above(y+1),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        }
        link.prepared=true;data.setDirty();
    }
    private static void setPlane(ServerLevel level,BlockPos anchor,boolean open) {
        for(int x=0;x<3;x++)for(int y=0;y<3;y++) {
            var p=anchor.offset(x,y,0);var state=level.getBlockState(p);
            if(open){if(!state.is(NexusContent.NEXUS_PORTAL.get())&&state.canBeReplaced())level.setBlock(p,NexusContent.NEXUS_PORTAL.get().defaultBlockState(),Block.UPDATE_CLIENTS);}
            else if(state.is(NexusContent.NEXUS_PORTAL.get()))level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        }
    }
    private static void warnTrapped(ServerPlayer player,CompoundTag data,long now) {
        if(now<data.getLong(WARN_COOLDOWN))return;data.putLong(WARN_COOLDOWN,now+40);
        player.displayClientMessage(Component.translatable("message.wayaround.nexus.no_return_signal"),true);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if(!(e.getOriginal() instanceof ServerPlayer original)||!(e.getEntity() instanceof ServerPlayer player))return;
        var from=original.getPersistentData();var to=player.getPersistentData();
        if(from.contains(RETURN_DIMENSION))to.putString(RETURN_DIMENSION,from.getString(RETURN_DIMENSION));
        if(from.contains(RETURN_BASE))to.putLong(RETURN_BASE,from.getLong(RETURN_BASE));to.putBoolean(INSIDE,from.getBoolean(INSIDE));
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer player)||!player.getPersistentData().getBoolean(INSIDE))return;
        var nexus=player.server.getLevel(NEXUS);var data=NexusTransitData.get(player.server);var link=returnLink(player);if(nexus==null||link==null)return;
        prepareArrival(nexus,link,data);player.getPersistentData().putLong(COOLDOWN,player.server.overworld().getGameTime()+50);
        // Death still returns the player inside; a shut-down threshold never becomes an emergency exit.
        arrive(player,nexus,link.destination.offset(1,0,-3),.62F);
    }
    public static BlockPos sourceAnchor(BlockPos base){return base.offset(-1,1,4);}
    private NexusPortalManager() {}
}
