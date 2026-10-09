package net.caravidro.wayaround.worldgen.weather.command;

import net.caravidro.wayaround.worldgen.weather.local.CloudStormManager;
import net.caravidro.wayaround.worldgen.weather.local.CloudRainOverrides;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CloudStormCommand {
    private CloudStormCommand(){}
    public static void register(RegisterCommandsEvent event){
        String[] names={"flash","lightning","gust"};
        for(int i=0;i<names.length;i++){
            int kind=i;
            event.getDispatcher().register(Commands.literal("cloudstorm").requires(s->s.hasPermission(2))
                    .then(Commands.literal(names[i]).executes(c->{
                        boolean success=CloudStormManager.force(c.getSource().getPlayerOrException(),kind);
                        if(!success)c.getSource().sendFailure(Component.literal("Nenhuma nuvem em região carregada; aproxime-se de um banco de nuvens."));
                        return success?1:0;
                    })));
        }
        event.getDispatcher().register(Commands.literal("raincloud").requires(s -> s.hasPermission(2))
                .executes(c -> setRain(c.getSource().getPlayerOrException(), c.getSource(), true))
                .then(Commands.literal("clear")
                        .executes(c -> setRain(c.getSource().getPlayerOrException(), c.getSource(), false))));
        event.getDispatcher().register(Commands.literal("cloudstorm").requires(s -> s.hasPermission(2))
                .then(Commands.literal("rain")
                        .executes(c -> setRain(c.getSource().getPlayerOrException(), c.getSource(), true))));
    }

    private static int setRain(net.minecraft.server.level.ServerPlayer player,
                               net.minecraft.commands.CommandSourceStack source, boolean rain) {
        var cloud = CloudRainOverrides.setAimed(player, rain);
        if (cloud == null) {
            source.sendFailure(Component.literal(
                    "Look at a procedural cloud within 800 blocks. Procedural Clouds and Living Weather must be enabled."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                rain ? "Target cloud is now a rain cloud for 5 minutes (ID " + cloud.id() + ")."
                        : "Rain override removed from cloud " + cloud.id() + "."), false);
        return 1;
    }
}
