package net.caravidro.wayaround.worldgen.volcanic;

import com.mojang.brigadier.Command;
import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class VolcanicCommands {

    private VolcanicCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "volcano"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "locate"
                                                )
                                                .executes(
                                                        context ->
                                                                locate(
                                                                        context.getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "info"
                                                )
                                                .executes(
                                                        context ->
                                                                info(
                                                                        context.getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                )
                                )
                );
    }

    private static int locate(
            ServerPlayer player
    ) {
        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        player.blockPosition()
                                .getX(),
                        player.blockPosition()
                                .getZ()
                );

        if (volcano == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Nenhuma região vulcânica disponível nesta faixa geográfica."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Vulcão: X="
                                + volcano.centerX()
                                + " Z="
                                + volcano.centerZ()
                                + " | raio ~"
                                + Math.round(
                                volcano.radius()
                        )
                                + " | cume Y="
                                + Math.round(
                                volcano.summitY()
                        )
                                + (
                                volcano.ceilingPeak()
                                        ? " | CEILING PEAK"
                                        : ""
                        )
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Teste rápido: /tp @s "
                                + volcano.centerX()
                                + " "
                                + Math.max(
                                120,
                                Math.min(
                                        315,
                                        (int) Math.round(
                                                volcano.summitY()
                                        )
                                )
                        )
                                + " "
                                + volcano.centerZ()
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int info(
            ServerPlayer player
    ) {
        int x =
                player.blockPosition()
                        .getX();

        int z =
                player.blockPosition()
                        .getZ();

        double influence =
                VolcanicField.influence(
                        x,
                        z
                );

        double crater =
                VolcanicField.craterStrength(
                        x,
                        z
                );

        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        x,
                        z
                );

        if (volcano == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Influência vulcânica: 0%"
                    )
            );

            return Command.SINGLE_SUCCESS;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Influência="
                                + Math.round(
                                influence * 100.0
                        )
                                + "% | cratera="
                                + Math.round(
                                crater * 100.0
                        )
                                + "% | atividade="
                                + Math.round(
                                volcano.activity()
                                        * 100.0
                        )
                                + "%"
                )
        );

        return Command.SINGLE_SUCCESS;
    }
}
