package net.caravidro.wayaround.industrial.mining;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MiningCommands {
    private MiningCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("wayaroundmine")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("locate")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            var nearest =
                                                    DeferredMiningManager.nearestRegion(
                                                            player.serverLevel(),
                                                            player.blockPosition(),
                                                            20
                                                    );

                                            if (nearest.isEmpty()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Nenhuma região mineral encontrada no raio de teste.")
                                                );
                                                return 0;
                                            }

                                            var region = nearest.get();
                                            String form = region.openPit()
                                                    ? "céu aberto"
                                                    : "subterrânea";

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            region.kind().id().toUpperCase()
                                                                    + " COMPLEX REGION | "
                                                                    + form
                                                                    + " | X "
                                                                    + region.centerX()
                                                                    + " Z "
                                                                    + region.centerZ()
                                                                    + (region.openPit()
                                                                    ? ""
                                                                    : " | Y ~" + region.undergroundY())
                                                    ),
                                                    false
                                            );

                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("awaken")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            boolean started =
                                                    DeferredMiningManager.forceAwaken(player);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            started
                                                                    ? "Região mineral próxima marcada para geração adiada."
                                                                    : "Chegue a até 160 blocos de uma região mineral no Overworld."
                                                    ),
                                                    false
                                            );

                                            return started ? 1 : 0;
                                        })
                        )
        );
    }
}
