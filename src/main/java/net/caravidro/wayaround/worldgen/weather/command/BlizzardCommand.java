package net.caravidro.wayaround.worldgen.weather.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.caravidro.wayaround.worldgen.weather.BlizzardManager;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BlizzardCommand {

    private BlizzardCommand() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {

        event.getDispatcher().register(

                Commands.literal("wayaround")

                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )

                        .then(

                                Commands.literal("blizzard")

                                        /*
                                         * ===========================
                                         * START
                                         * ===========================
                                         */

                                        .then(

                                                Commands.literal("start")

                                                        /*
                                                         * /wayaround blizzard start
                                                         */
                                                        .executes(
                                                                context -> {

                                                                    ServerLevel level =
                                                                            context.getSource()
                                                                                    .getLevel();

                                                                    Vec3 position =
                                                                            context.getSource()
                                                                                    .getPosition();

                                                                    BlizzardManager.start(
                                                                            level,

                                                                            position,

                                                                            450.0,

                                                                            20L
                                                                                    * 60L
                                                                                    * 3L,

                                                                            1.0
                                                                    );

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Blizzard iniciada!"
                                                                                            ),
                                                                                    true
                                                                            );

                                                                    return 1;
                                                                }
                                                        )

                                                        /*
                                                         * /wayaround blizzard start 700 180
                                                         */
                                                        .then(

                                                                Commands.argument(
                                                                                "radius",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        32.0,
                                                                                        3000.0
                                                                                )
                                                                        )

                                                                        .then(

                                                                                Commands.argument(
                                                                                                "seconds",
                                                                                                IntegerArgumentType.integer(
                                                                                                        5,
                                                                                                        3600
                                                                                                )
                                                                                        )

                                                                                        .executes(
                                                                                                context -> {

                                                                                                    ServerLevel level =
                                                                                                            context.getSource()
                                                                                                                    .getLevel();

                                                                                                    Vec3 position =
                                                                                                            context.getSource()
                                                                                                                    .getPosition();

                                                                                                    double radius =
                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                    context,
                                                                                                                    "radius"
                                                                                                            );

                                                                                                    int seconds =
                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                    context,
                                                                                                                    "seconds"
                                                                                                            );

                                                                                                    BlizzardManager.start(
                                                                                                            level,
                                                                                                            position,
                                                                                                            radius,
                                                                                                            seconds * 20L,
                                                                                                            1.0
                                                                                                    );

                                                                                                    return 1;
                                                                                                }
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        /*
                                         * ===========================
                                         * STARTAT
                                         * ===========================
                                         *
                                         * Cria a tempestade em uma
                                         * região específica.
                                         */

                                        .then(

                                                Commands.literal("startat")

                                                        .then(

                                                                Commands.argument(
                                                                                "x",
                                                                                DoubleArgumentType.doubleArg()
                                                                        )

                                                                        .then(

                                                                                Commands.argument(
                                                                                                "z",
                                                                                                DoubleArgumentType.doubleArg()
                                                                                        )

                                                                                        .then(

                                                                                                Commands.argument(
                                                                                                                "radius",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        32.0,
                                                                                                                        3000.0
                                                                                                                )
                                                                                                        )

                                                                                                        .then(

                                                                                                                Commands.argument(
                                                                                                                                "seconds",
                                                                                                                                IntegerArgumentType.integer(
                                                                                                                                        5,
                                                                                                                                        3600
                                                                                                                                )
                                                                                                                        )

                                                                                                                        .executes(
                                                                                                                                context -> {

                                                                                                                                    ServerLevel level =
                                                                                                                                            context.getSource()
                                                                                                                                                    .getLevel();

                                                                                                                                    double x =
                                                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                                                    context,
                                                                                                                                                    "x"
                                                                                                                                            );

                                                                                                                                    double z =
                                                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                                                    context,
                                                                                                                                                    "z"
                                                                                                                                            );

                                                                                                                                    double radius =
                                                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                                                    context,
                                                                                                                                                    "radius"
                                                                                                                                            );

                                                                                                                                    int seconds =
                                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                                    context,
                                                                                                                                                    "seconds"
                                                                                                                                            );

                                                                                                                                    Vec3 center =
                                                                                                                                            new Vec3(
                                                                                                                                                    x,
                                                                                                                                                    100.0,
                                                                                                                                                    z
                                                                                                                                            );

                                                                                                                                    BlizzardManager.start(
                                                                                                                                            level,
                                                                                                                                            center,
                                                                                                                                            radius,
                                                                                                                                            seconds * 20L,
                                                                                                                                            1.0
                                                                                                                                    );

                                                                                                                                    context.getSource()
                                                                                                                                            .sendSuccess(
                                                                                                                                                    () ->
                                                                                                                                                            Component.literal(
                                                                                                                                                                    "Blizzard criada em X="
                                                                                                                                                                            + x
                                                                                                                                                                            + " Z="
                                                                                                                                                                            + z
                                                                                                                                                            ),
                                                                                                                                                    true
                                                                                                                                            );

                                                                                                                                    return 1;
                                                                                                                                }
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        /*
                                         * ===========================
                                         * STOP
                                         * ===========================
                                         */

                                        .then(

                                                Commands.literal("stop")

                                                        .executes(
                                                                context -> {

                                                                    BlizzardManager.stop(
                                                                            context.getSource()
                                                                                    .getLevel()
                                                                    );

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Blizzard encerrada."
                                                                                            ),
                                                                                    true
                                                                            );

                                                                    return 1;
                                                                }
                                                        )
                                        )
                        )
        );
    }
}