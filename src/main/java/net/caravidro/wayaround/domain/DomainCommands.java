package net.caravidro.wayaround.domain;

import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DomainCommands {

    private DomainCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "domain"
                                )

                                .then(
                                        Commands.literal(
                                                        "awaken"
                                                )
                                                .executes(
                                                        context -> {

                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            boolean already =
                                                                    DomainPlayerData.awakened(
                                                                            player
                                                                    );

                                                            DomainProfile profile =
                                                                    DomainPlayerData.awaken(
                                                                            player
                                                                    );

                                                            if (already) {
                                                                player.sendSystemMessage(
                                                                        Component.literal(
                                                                                "Seu domínio já foi despertado: "
                                                                                + profile.name()
                                                                        )
                                                                );
                                                            } else {
                                                                player.sendSystemMessage(
                                                                        Component.literal(
                                                                                "Seu domínio despertou: "
                                                                                + profile.name()
                                                                        )
                                                                );

                                                                sendInfo(
                                                                        player,
                                                                        profile
                                                                );
                                                            }

                                                            return Command.SINGLE_SUCCESS;
                                                        }
                                                )
                                )

                                .then(
                                        Commands.literal(
                                                        "info"
                                                )
                                                .executes(
                                                        context -> {

                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            DomainProfile profile =
                                                                    DomainPlayerData.profile(
                                                                            player
                                                                    );

                                                            if (profile == null) {
                                                                player.sendSystemMessage(
                                                                        Component.literal(
                                                                                "Você ainda não possui um domínio. Use /domain awaken."
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            sendInfo(
                                                                    player,
                                                                    profile
                                                            );

                                                            return Command.SINGLE_SUCCESS;
                                                        }
                                                )
                                )

                                .then(
                                        Commands.literal(
                                                        "expand"
                                                )
                                                .executes(
                                                        context ->
                                                                DomainManager.expand(
                                                                        context.getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                                        ? Command.SINGLE_SUCCESS
                                                                        : 0
                                                )
                                )

                                .then(
                                        Commands.literal(
                                                        "quick"
                                                )
                                                .executes(
                                                        context ->
                                                                DomainManager.quick(
                                                                        context.getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                                        ? Command.SINGLE_SUCCESS
                                                                        : 0
                                                )
                                )
                );
    }

    private static void sendInfo(
            ServerPlayer player,
            DomainProfile profile
    ) {
        player.sendSystemMessage(
                Component.literal(
                        "§6"
                        + profile.name()
                        + "§r"
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Cor: "
                        + profile.color().title
                        + " | Alcance: "
                        + String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                profile.radius()
                        )
                        + " blocos | Duração: "
                        + String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                profile.durationTicks()
                                / 20.0
                        )
                        + "s"
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Regra: "
                        + profile.ruleDescription()
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Manifestação rápida: "
                        + profile.reward().description
                )
        );
    }
}
