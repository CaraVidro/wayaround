package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class NatureCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("birds").requires(s->s.hasPermission(2))
            .then(Commands.literal("migration").executes(c->{
                BirdWorldEvents.migration(c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(()->Component.literal("Migration flock started nearby."),false);
                return 1;
            })));
    }
}
