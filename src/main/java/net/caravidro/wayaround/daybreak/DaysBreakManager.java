package net.caravidro.wayaround.daybreak;

import java.util.LinkedHashSet;
import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Global, saved, command-only activation. Entity work is staggered and queue-bounded. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class DaysBreakManager {
    private static boolean active;
    private static final LinkedHashSet<Entity> candidates=new LinkedHashSet<>();
    private static DaysBreakPayload payload(net.minecraft.server.MinecraftServer server){var data=DaysBreakData.get(server);return new DaysBreakPayload(data.active(),Math.max(0,server.overworld().getGameTime()-data.started()));}
    public static void synchronize(ServerPlayer player){PacketDistributor.sendToPlayer(player,payload(player.server));}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("then_days_break").requires(s->s.hasPermission(2))
            .then(Commands.literal("start").executes(c->set(c.getSource(),true)))
            .then(Commands.literal("stop").executes(c->set(c.getSource(),false)))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal("Then Days Break: "+(DaysBreakData.get(c.getSource().getServer()).active()?"ON":"OFF")),false);return 1;})));
    }
    private static int set(net.minecraft.commands.CommandSourceStack source,boolean enabled){
        var server=source.getServer();DaysBreakData.get(server).set(enabled,server.overworld().getGameTime());active=enabled;candidates.clear();
        for(ServerPlayer player:server.getPlayerList().getPlayers())synchronize(player);
        source.sendSuccess(()->Component.literal("Then Days Break: "+(enabled?"ON":"OFF")),true);return 1;
    }
    @SubscribeEvent public static void before(ServerTickEvent.Pre event){active=DaysBreakData.get(event.getServer()).active();}
    @SubscribeEvent public static void entity(EntityTickEvent.Post event){
        Entity entity=event.getEntity();
        if(active&&!entity.level().isClientSide&&(entity.tickCount+entity.getId())%20==0&&candidates.size()<4096)candidates.add(entity);
    }
    @SubscribeEvent public static void after(ServerTickEvent.Post event){
        if(!active){candidates.clear();return;}
        var iterator=candidates.iterator();int budget=32;
        while(iterator.hasNext()&&budget-->0){Entity entity=iterator.next();iterator.remove();SolarExposure.sample(entity,DaysBreakMath.day(entity.level().getDayTime()));}
        if(event.getServer().getTickCount()%100==0)for(ServerPlayer player:event.getServer().getPlayerList().getPlayers())synchronize(player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)synchronize(player);}
    @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)synchronize(player);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)synchronize(player);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){active=false;candidates.clear();}
}
