package net.caravidro.wayaround.spectrum;

import com.mojang.brigadier.Command;

import net.caravidro.wayaround.domain.VoidDomainPresentation;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Spectrum-facing debug/convenience commands.
 *
 * Void Domain progression does not have a natural unlock path yet, so this is
 * the temporary selector for testing the three stages.
 */
public final class SpectrumCommands {

    private SpectrumCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "spectrum"
                                )
                                .then(
                                        Commands.literal(
                                                        "domain"
                                                )
                                                .then(
                                                        Commands.literal(
                                                                        "get"
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                show(
                                                                                        context.getSource()
                                                                                                .getPlayerOrException()
                                                                                )
                                                                )
                                                )
                                                .then(
                                                        Commands.literal(
                                                                        "set"
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "innate",
                                                                                VoidDomainPresentation.INNATE
                                                                        )
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "inato",
                                                                                VoidDomainPresentation.INNATE
                                                                        )
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "simple",
                                                                                VoidDomainPresentation.SIMPLE
                                                                        )
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "simples",
                                                                                VoidDomainPresentation.SIMPLE
                                                                        )
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "absolute",
                                                                                VoidDomainPresentation.ABSOLUTE
                                                                        )
                                                                )
                                                                .then(
                                                                        mode(
                                                                                "absoluto",
                                                                                VoidDomainPresentation.ABSOLUTE
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<
            net.minecraft.commands.CommandSourceStack
            > mode(
            String literal,
            VoidDomainPresentation presentation
    ) {
        return Commands.literal(
                        literal
                )
                .executes(
                        context ->
                                set(
                                        context.getSource()
                                                .getPlayerOrException(),
                                        presentation
                                )
                );
    }

    private static int set(
            ServerPlayer player,
            VoidDomainPresentation presentation
    ) {
        if (!SpectrumAccess.has(
                player,
                SpectrumType.VOID
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "Você não possui o Spectrum Void."
                    )
            );

            return 0;
        }

        VoidDomainPresentation.set(
                player,
                presentation
        );

        String detail =
                switch (presentation) {
                    case INNATE ->
                            "domínio inato: nenhuma Expansão é exibida ou lançada.";
                    case SIMPLE ->
                            "Expansão simples: intro somente com texto.";
                    case ABSOLUTE ->
                            "Expansão absoluta: intro com a arte absoluta do Void.";
                };

        player.sendSystemMessage(
                Component.literal(
                        "Void Domain -> "
                                + presentation.label()
                                + " — "
                                + detail
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int show(
            ServerPlayer player
    ) {
        VoidDomainPresentation presentation =
                VoidDomainPresentation.get(
                        player
                );

        player.sendSystemMessage(
                Component.literal(
                        "Void Domain atual: "
                                + presentation.label()
                )
        );

        return Command.SINGLE_SUCCESS;
    }
}
