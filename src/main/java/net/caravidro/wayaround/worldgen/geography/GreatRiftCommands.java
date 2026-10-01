package net.caravidro.wayaround.worldgen.geography;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class GreatRiftCommands {

    private GreatRiftCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "rift"
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
        GreatRiftField.Rift rift =
                GreatRiftField.nearest(
                        player.blockPosition()
                                .getX(),
                        player.blockPosition()
                                .getZ()
                );

        if (rift == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Nenhuma Great Rift disponível nesta região."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Great Rift: X="
                                + rift.centerX()
                                + " Z="
                                + rift.centerZ()
                                + " | comprimento ~"
                                + Math.round(
                                rift.halfLength()
                                        * 2.0
                        )
                                + " | largura do vale ~"
                                + Math.round(
                                rift.halfWidth()
                                        * 2.0
                        )
                                + " | piso Y="
                                + Math.round(
                                rift.floorY()
                        )
                                + (
                                rift.abyssal()
                                        ? " | ABYSSAL"
                                        : ""
                        )
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Teste: /tp @s "
                                + rift.centerX()
                                + " "
                                + Math.max(
                                90,
                                (int) Math.round(
                                        rift.rimY()
                                                + 18.0
                                )
                        )
                                + " "
                                + rift.centerZ()
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

        GreatRiftField.Rift rift =
                GreatRiftField.nearest(
                        x,
                        z
                );

        if (rift == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "Influência da Great Rift: 0%"
                    )
            );

            return Command.SINGLE_SUCCESS;
        }

        GreatRiftField.Sample sample =
                rift.sample(
                        x,
                        z
                );

        player.sendSystemMessage(
                Component.literal(
                        "Influência="
                                + Math.round(
                                sample.region()
                                        * 100.0
                        )
                                + "% | canyon="
                                + Math.round(
                                sample.canyon()
                                        * 100.0
                        )
                                + "% | distância da parede central="
                                + Math.round(
                                sample.crossDistance()
                        )
                                + " blocos"
                )
        );

        return Command.SINGLE_SUCCESS;
    }
}
