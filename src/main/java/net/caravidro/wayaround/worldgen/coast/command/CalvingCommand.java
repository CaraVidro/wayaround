package net.caravidro.wayaround.worldgen.coast.command;

import net.caravidro.wayaround.worldgen.coast.CalvingManager;

import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CalvingCommand {

    private CalvingCommand() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {

        event.getDispatcher()
                .register(

                        Commands
                                .literal(
                                        "calving"
                                )

                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )

                                .executes(
                                        context ->
                                                run(
                                                        context
                                                                .getSource()
                                                                .getPlayerOrException()
                                                )
                                )

                                .then(

                                        Commands
                                                .literal(
                                                        "here"
                                                )

                                                .executes(
                                                        context ->
                                                                run(
                                                                        context
                                                                                .getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                )
                                )
                );
    }

    private static int run(
            ServerPlayer player
    ) {

        boolean started =
                CalvingManager
                        .tryStartAt(
                                player
                        );

        if (
                started
        ) {

            player.sendSystemMessage(
                    Component.literal(
                            "§bA geleira começou a rachar..."
                    )
            );

            return 1;
        }

        player.sendSystemMessage(
                Component.literal(
                        "§cNenhuma parede glacial adequada encontrada perto de você."
                )
        );

        return 0;
    }
}