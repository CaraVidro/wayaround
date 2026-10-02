package net.caravidro.wayaround.performance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /wayperf start [seconds]
 * /wayperf stop
 * /wayperf report
 * /wayperf dump
 * /wayperf status
 */
public final class PerformanceCommands {

    private PerformanceCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wayperf"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .executes(
                                        context ->
                                                status(
                                                        context.getSource()
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "start"
                                                )
                                                .executes(
                                                        context ->
                                                                start(
                                                                        context.getSource(),
                                                                        10
                                                                )
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "seconds",
                                                                        IntegerArgumentType.integer(
                                                                                2,
                                                                                300
                                                                        )
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                start(
                                                                                        context.getSource(),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "seconds"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "stop"
                                                )
                                                .executes(
                                                        context ->
                                                                stop(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "report"
                                                )
                                                .executes(
                                                        context ->
                                                                report(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "dump"
                                                )
                                                .executes(
                                                        context ->
                                                                dump(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "status"
                                                )
                                                .executes(
                                                        context ->
                                                                status(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                );
    }

    private static int start(
            CommandSourceStack source,
            int seconds
    ) {
        PerformanceProfiler.start(
                source.getServer(),
                seconds
        );

        source.sendSuccess(
                () -> Component.literal(
                        "WAYPERF iniciado por "
                                + seconds
                                + "s. Jogue normalmente para capturar uma carga real."
                ),
                false
        );

        return 1;
    }

    private static int stop(
            CommandSourceStack source
    ) {
        if (!PerformanceProfiler.enabled()) {
            source.sendFailure(
                    Component.literal(
                            "WAYPERF não está rodando."
                    )
            );

            return 0;
        }

        PerformanceProfiler.Snapshot snapshot =
                PerformanceProfiler.stop();

        sendReport(
                source,
                snapshot
        );

        return 1;
    }

    private static int report(
            CommandSourceStack source
    ) {
        PerformanceProfiler.Snapshot snapshot =
                PerformanceProfiler.reportSnapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "Nenhuma captura WAYPERF disponível ainda."
                    )
            );

            return 0;
        }

        sendReport(
                source,
                snapshot
        );

        return 1;
    }

    private static int dump(
            CommandSourceStack source
    ) {
        PerformanceProfiler.Snapshot snapshot =
                PerformanceProfiler.reportSnapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "Nenhuma captura WAYPERF disponível para exportar."
                    )
            );

            return 0;
        }

        try {
            Path output =
                    PerformanceProfiler.dumpCsv(
                            snapshot
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "WAYPERF CSV salvo em "
                                    + output
                                            .toAbsolutePath()
                    ),
                    false
            );

            return 1;

        } catch (IOException exception) {
            source.sendFailure(
                    Component.literal(
                            "Falha ao salvar WAYPERF: "
                                    + exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int status(
            CommandSourceStack source
    ) {
        if (PerformanceProfiler.enabled()) {
            long remaining =
                    PerformanceProfiler.remainingSeconds(
                            source.getServer()
                    );

            source.sendSuccess(
                    () -> Component.literal(
                            "WAYPERF ativo | faltam ~"
                                    + remaining
                                    + "s."
                    ),
                    false
            );

            return 1;
        }

        if (PerformanceProfiler.reportSnapshot()
                != null) {
            source.sendSuccess(
                    () -> Component.literal(
                            "WAYPERF parado | existe uma captura pronta. Use /wayperf report ou /wayperf dump."
                    ),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "WAYPERF parado | use /wayperf start 10."
                ),
                false
        );

        return 1;
    }

    private static void sendReport(
            CommandSourceStack source,
            PerformanceProfiler.Snapshot snapshot
    ) {
        List<String> lines =
                PerformanceProfiler.formatLines(
                        snapshot,
                        12
                );

        for (String line :
                lines) {
            source.sendSuccess(
                    () -> Component.literal(
                            line
                    ),
                    false
            );
        }
    }
}
