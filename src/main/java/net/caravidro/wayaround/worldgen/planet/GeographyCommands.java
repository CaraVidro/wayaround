package net.caravidro.wayaround.worldgen.planet;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class GeographyCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("wayworld").requires(s->s.hasPermission(2))
            .then(Commands.literal("status").executes(c->{var s=WorldFeatureRuntime.serverCopy();c.getSource().sendSuccess(()->Component.literal("Geography="+s.enabled(WorldFeature.LARGE_GEOGRAPHY)+" finite="+s.enabled(WorldFeature.FINITE_WORLD)+" size=65536 x 65536; witnessed deaths="+s.enabled(WorldFeature.WITNESSED_DEATHS)+" lost respawn="+s.enabled(WorldFeature.RANDOM_RESPAWN)+". Terrain options are fixed at world creation."),false);return 1;}))
            .then(Commands.literal("respawn").then(Commands.literal("random").executes(c->set(c.getSource(),WorldFeature.RANDOM_RESPAWN,true))).then(Commands.literal("bed").executes(c->set(c.getSource(),WorldFeature.RANDOM_RESPAWN,false))))
            .then(Commands.literal("deaths").then(Commands.literal("witnessed").executes(c->set(c.getSource(),WorldFeature.WITNESSED_DEATHS,true))).then(Commands.literal("global").executes(c->set(c.getSource(),WorldFeature.WITNESSED_DEATHS,false)))));
    }
    private static int set(net.minecraft.commands.CommandSourceStack source,WorldFeature feature,boolean enabled) {
        var server=source.getServer();var settings=WorldFeatureRuntime.serverCopy();settings.set(feature,enabled);
        var data=server.overworld().getDataStorage().computeIfAbsent(WorldFeatureSavedData.factory(),WorldFeatureSavedData.ID);
        data.setSettings(settings);WorldFeatureRuntime.applyServer(settings);for(var p:server.getPlayerList().getPlayers())WorldFeatureService.sync(p);
        source.sendSuccess(()->Component.literal(feature.title()+": "+enabled),false);return 1;
    }
    private GeographyCommands() {}
}
