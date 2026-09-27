package net.caravidro.wayaround.ecology;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Developer ecology accelerator. It advances succession sampling, not the
 * world's clock, so it is safe to use while visually testing nature changes.
 */
public final class EcologyTimeCommand {

    private EcologyTimeCommand() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                "timetick"
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
                                                        context.getSource(),
                                                        10
                                                )
                                )
                                .then(
                                        Commands.argument(
                                                "steps",
                                                IntegerArgumentType.integer(
                                                        1,
                                                        200
                                                )
                                        )
                                                .executes(
                                                        context ->
                                                                run(
                                                                        context.getSource(),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "steps"
                                                                        )
                                                                )
                                                )
                                )
                );
    }

    private static int run(
            net.minecraft.commands.CommandSourceStack source,
            int steps
    ) {
        int applied =
                EcologicalSuccession.debugAdvance(
                        source.getServer(),
                        steps
                );

        source.sendSuccess(
                () -> Component.literal(
                        "Natureza acelerada em "
                                + applied
                                + " pulso(s) ecológico(s)."
                ),
                false
        );

        return applied;
    }
}
