package net.caravidro.wayaround.cursed;

import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Small runClient diagnostic surface for Tukuna.
 *
 * It deliberately uses the same possession/music payload and Fuga parser as
 * normal gameplay, while collapsing host + spirit into the local player so a
 * second account is not required during development.
 */
public final class TukunaDebugCommands {

    private TukunaDebugCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "tukuna_test"
                                )
                                .then(
                                        Commands.literal(
                                                        "possession"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            return TukunaManager.debugSelfPossession(
                                                                    player
                                                            );
                                                        }
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "sound"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            return TukunaManager.debugSound(
                                                                    player
                                                            );
                                                        }
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "status"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            return TukunaManager.debugStatus(
                                                                    player
                                                            );
                                                        }
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "stop"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            return TukunaManager.debugStop(
                                                                    player
                                                            );
                                                        }
                                                )
                                )
                                .executes(
                                        context -> {
                                            context.getSource()
                                                    .sendSuccess(
                                                            () -> net.minecraft.network.chat.Component.literal(
                                                                    "Use /tukuna_test possession, sound, status ou stop."
                                                            ),
                                                            false
                                                    );

                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                );
    }
}
