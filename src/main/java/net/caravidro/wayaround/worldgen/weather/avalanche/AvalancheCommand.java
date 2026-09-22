package net.caravidro.wayaround.worldgen.weather.avalanche;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AvalancheCommand {

    private AvalancheCommand() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {

        CommandDispatcher<CommandSourceStack>
                dispatcher =
                event.getDispatcher();

        dispatcher.register(

                Commands.literal(
                                "avalanche"
                        )

                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )

                        .then(

                                Commands.literal(
                                                "start"
                                        )

                                        .executes(
                                                context -> {

                                                    CommandSourceStack source =
                                                            context.getSource();

                                                    ServerPlayer player =
                                                            source.getPlayerOrException();

                                                    ServerLevel level =
                                                            player.serverLevel();

                                                    Vec3 look =
                                                            player.getLookAngle();

                                                    Vec3 direction =
                                                            new Vec3(
                                                                    look.x,
                                                                    0.0,
                                                                    look.z
                                                            );

                                                    if (
                                                            direction.lengthSqr()
                                                            <
                                                            0.001
                                                    ) {

                                                        direction =
                                                                new Vec3(
                                                                        0.0,
                                                                        0.0,
                                                                        1.0
                                                                );
                                                    }

                                                    direction =
                                                            direction.normalize();

                                                    /*
                                                     * Nasce 45 blocos
                                                     * na frente.
                                                     */
                                                    Vec3 start =
                                                            player.position()
                                                                    .add(
                                                                            direction.scale(
                                                                                    45.0
                                                                            )
                                                                    );

                                                    AvalancheManager.start(
                                                            level,

                                                            start,

                                                            direction,

                                                            180.0,
                                                            36.0,
                                                            0.90,
                                                            360.0
                                                    );

                                                    source.sendSuccess(
                                                            () ->
                                                                    Component.literal(
                                                                            "AVALANCHE INICIADA 🦅"
                                                                    ),

                                                            true
                                                    );

                                                    return 1;
                                                }
                                        )
                        )

                        .then(

                                Commands.literal(
                                                "clear"
                                        )

                                        .executes(
                                                context -> {

                                                    ServerPlayer player =
                                                            context
                                                                    .getSource()
                                                                    .getPlayerOrException();

                                                    AvalancheManager.clear(
                                                            player.serverLevel()
                                                    );

                                                    context
                                                            .getSource()
                                                            .sendSuccess(
                                                                    () ->
                                                                            Component.literal(
                                                                                    "Avalanches removidas."
                                                                            ),

                                                                    true
                                                            );

                                                    return 1;
                                                }
                                        )
                        )
        );
    }
}   