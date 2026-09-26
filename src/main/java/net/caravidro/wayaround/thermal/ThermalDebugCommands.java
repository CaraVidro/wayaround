package net.caravidro.wayaround.thermal;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class ThermalDebugCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("spectrum_heat").requires(s->s.hasPermission(2))
                .executes(c->{var p=c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(()->Component.literal("Temperatura local: "+Math.round(RegionalTemperature.at(p.serverLevel(),p.blockPosition()))+" graus de jogo"),false);return 1;})
                .then(Commands.argument("temperature",DoubleArgumentType.doubleArg(20,3200))
                        .then(Commands.argument("radius",DoubleArgumentType.doubleArg(1,48)).executes(c->{
                            var p=c.getSource().getPlayerOrException();
                            RegionalTemperature.pulse(p.serverLevel(),p.position(),DoubleArgumentType.getDouble(c,"radius"),DoubleArgumentType.getDouble(c,"temperature"));
                            c.getSource().sendSuccess(()->Component.literal("Pulso térmico aplicado. A região esfria gradualmente."),false);return 1;
                        }))));
    }
}
