package net.caravidro.wayaround.worldgen.weather.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.caravidro.wayaround.worldgen.weather.local.WindTestManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /wayaround windtest <limit> [radius] [rampSeconds]
 *
 * The wind begins at zero, rises smoothly to the requested limit, then fades
 * out automatically over two seconds.
 */
public final class WindTestCommand {

    private WindTestCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wayaround")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("windtest")
                                        .then(
                                                Commands.literal("stop")
                                                        .executes(context -> {
                                                            ServerLevel level =
                                                                    context.getSource().getLevel();

                                                            WindTestManager.stop(
                                                                    level.getServer().overworld()
                                                            );

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Vento de teste removido."
                                                                    ),
                                                                    true
                                                            );

                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.argument(
                                                                "limit",
                                                                DoubleArgumentType.doubleArg(
                                                                        0.05,
                                                                        1.0
                                                                )
                                                        )
                                                        .executes(context ->
                                                                start(
                                                                        context.getSource().getLevel(),
                                                                        context.getSource().getPosition(),
                                                                        DoubleArgumentType.getDouble(
                                                                                context,
                                                                                "limit"
                                                                        ),
                                                                        96.0,
                                                                        8,
                                                                        context
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "radius",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        8.0,
                                                                                        512.0
                                                                                )
                                                                        )
                                                                        .executes(context ->
                                                                                start(
                                                                                        context.getSource().getLevel(),
                                                                                        context.getSource().getPosition(),
                                                                                        DoubleArgumentType.getDouble(
                                                                                                context,
                                                                                                "limit"
                                                                                        ),
                                                                                        DoubleArgumentType.getDouble(
                                                                                                context,
                                                                                                "radius"
                                                                                        ),
                                                                                        8,
                                                                                        context
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "rampSeconds",
                                                                                                IntegerArgumentType.integer(
                                                                                                        1,
                                                                                                        60
                                                                                                )
                                                                                        )
                                                                                        .executes(context ->
                                                                                                start(
                                                                                                        context.getSource().getLevel(),
                                                                                                        context.getSource().getPosition(),
                                                                                                        DoubleArgumentType.getDouble(
                                                                                                                context,
                                                                                                                "limit"
                                                                                                        ),
                                                                                                        DoubleArgumentType.getDouble(
                                                                                                                context,
                                                                                                                "radius"
                                                                                                        ),
                                                                                                        IntegerArgumentType.getInteger(
                                                                                                                context,
                                                                                                                "rampSeconds"
                                                                                                        ),
                                                                                                        context
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int start(
            ServerLevel sourceLevel,
            Vec3 position,
            double limit,
            double radius,
            int rampSeconds,
            com.mojang.brigadier.context.CommandContext<
                    net.minecraft.commands.CommandSourceStack
                    > context
    ) {
        if (!sourceLevel.dimension().equals(Level.OVERWORLD)) {
            context.getSource().sendFailure(
                    Component.literal(
                            "O teste de vento local funciona no Overworld."
                    )
            );
            return 0;
        }

        WindTestManager.start(
                sourceLevel,
                position,
                radius,
                (float) limit,
                rampSeconds * 20
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        String.format(
                                "Vento de teste: 0 -> %.2f em %ds, raio %.0f. Depois desaparece.",
                                limit,
                                rampSeconds,
                                radius
                        )
                ),
                true
        );

        return 1;
    }
}
