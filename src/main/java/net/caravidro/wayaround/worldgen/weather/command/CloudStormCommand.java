package net.caravidro.wayaround.worldgen.weather.command;

import net.caravidro.wayaround.worldgen.weather.local.CloudStormManager;
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
    }
}
