package net.caravidro.wayaround.nexus;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NexusCommands {

    private NexusCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "nexus"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "finish"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            boolean finished =
                                                                    NexusEventManager.finishNearest(
                                                                            player
                                                                    );

                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    finished
                                                                                            ? "NEXUS // processo concluído à força."
                                                                                            : "NEXUS // nenhum Nexustor ativo encontrado."
                                                                            ),
                                                                            false
                                                                    );

                                                            return finished
                                                                    ? 1
                                                                    : 0;
                                                        }
                                                )
                                )
                );
    }
}
