package net.caravidro.wayaround.ecology;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class KrakenCommands {

    private KrakenCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(Commands.literal("kraken").requires(source -> source.hasPermission(2))
                .then(Commands.literal("stop").executes(context ->
                        KrakenManager.stop(context.getSource().getLevel()) ? 1 : 0)));
        String[] scenes = {"migrate", "bubbles", "passing", "eyes", "submarine"};
        for (int i = 0; i < scenes.length; i++) {
            final int kind = i + 2;
            dispatcher.register(Commands.literal("kraken").requires(source -> source.hasPermission(2))
                    .then(Commands.literal(scenes[i]).executes(context -> {
                        boolean started = KrakenManager.forceSceneCommand(context.getSource().getPlayerOrException(), kind);
                        if (!started) context.getSource().sendFailure(Component.literal("Precisa de mar aberto e nenhum evento ativo."));
                        return started ? 1 : 0;
                    })));
        }
        dispatcher.register(
                Commands.literal(
                                "kraken"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
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

                                                    boolean haunted =
                                                            KrakenManager.isKrakenWaters(
                                                                    player
                                                            );

                                                    context.getSource()
                                                            .sendSuccess(
                                                                    () -> Component.literal(
                                                                            haunted
                                                                                    ? "Kraken waters: SIM. Talvez não olhe para baixo."
                                                                                    : "Kraken waters: não nesta região."
                                                                    ),
                                                                    false
                                                            );

                                                    return haunted
                                                            ? 1
                                                            : 0;
                                                }
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "tentacle"
                                        )
                                        .executes(
                                                context -> {
                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    boolean started =
                                                            KrakenManager.forceTentacle(
                                                                    player
                                                            );

                                                    context.getSource()
                                                            .sendSuccess(
                                                                    () -> Component.literal(
                                                                            started
                                                                                    ? "Tentáculo do Kraken iniciado."
                                                                                    : "Não achei mar aberto suficiente ou já há outro evento ativo."
                                                                    ),
                                                                    false
                                                            );

                                                    return started
                                                            ? 1
                                                            : 0;
                                                }
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "watch"
                                        )
                                        .executes(
                                                context -> {
                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    boolean started =
                                                            KrakenManager.forceWatch(
                                                                    player
                                                            );

                                                    context.getSource()
                                                            .sendSuccess(
                                                                    () -> Component.literal(
                                                                            started
                                                                                    ? "Ele está olhando."
                                                                                    : "Não achei mar aberto suficiente ou já há outro evento ativo."
                                                                    ),
                                                                    false
                                                            );

                                                    return started
                                                            ? 1
                                                            : 0;
                                                }
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "rumble"
                                        )
                                        .executes(
                                                context -> {
                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    KrakenManager.forceRumble(
                                                            player
                                                    );

                                                    context.getSource()
                                                            .sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Alguma coisa se mexeu lá embaixo."
                                                                    ),
                                                                    false
                                                            );

                                                    return 1;
                                                }
                                        )
                        )
        );
    }
}
