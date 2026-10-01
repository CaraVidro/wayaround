package net.caravidro.wayaround.worldgen.feature;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Iteration command for the new organic tree architecture.
 */
public final class TreeArchitectureCommands {

    private TreeArchitectureCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        var root =
                Commands.literal(
                                "treegen"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "here"
                                        )
                                        .executes(
                                                context ->
                                                        place(
                                                                context.getSource()
                                                                        .getPlayerOrException(),
                                                                null
                                                        )
                                        )
                        );

        String[][] types = {
                {"oak", "forest"},
                {"birch", "birch_forest"},
                {"spruce", "taiga"},
                {"jungle", "jungle"},
                {"acacia", "savanna"},
                {"dark_oak", "dark_forest"},
                {"cherry", "cherry_grove"},
                {"mangrove", "mangrove_swamp"}
        };

        for (String[] type :
                types) {
            root.then(
                    Commands.literal(
                                    type[0]
                            )
                            .executes(
                                    context ->
                                            place(
                                                    context.getSource()
                                                            .getPlayerOrException(),
                                                    type[1]
                                            )
                            )
            );
        }

        event.getDispatcher()
                .register(
                        root
                );
    }

    private static int place(
            ServerPlayer player,
            String forcedBiome
    ) {
        ServerLevel level =
                player.serverLevel();

        int x =
                player.blockPosition()
                        .getX();

        int z =
                player.blockPosition()
                        .getZ();

        int y =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

        BlockPos base =
                new BlockPos(
                        x,
                        y,
                        z
                );

        String biome =
                forcedBiome != null
                        ? forcedBiome
                        : level.getBiome(
                        base
                )
                        .unwrapKey()
                        .map(
                                key ->
                                        key.location()
                                                .getPath()
                        )
                        .orElse(
                                "forest"
                        );

        OrganicTreeGenerator.Result result =
                OrganicTreeGenerator.place(
                        level,
                        base,
                        biome,
                        level.random
                );

        if (!result.placed()) {
            player.sendSystemMessage(
                    Component.literal(
                            "Árvore não coube aqui. Tente em chão livre."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Árvore orgânica gerada: "
                                + biome
                )
        );

        return Command.SINGLE_SUCCESS;
    }
}
