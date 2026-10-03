package net.caravidro.wayaround.nexus;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NexusCommands {

    private NexusCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "nexus"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(Commands.literal("visit").executes(context->{
                                    var player=context.getSource().getPlayerOrException();var level=player.server.getLevel(NexusPortalManager.NEXUS);
                                    if(level==null)return 0;
                                    var room=net.caravidro.wayaround.nexus.world.NexusLayout.room(
                                            net.caravidro.wayaround.nexus.world.NexusChunkGenerator.layoutSeed(level.getChunkSource().randomState()),0,0);
                                    var p=new net.minecraft.core.BlockPos(room.x(),room.floor(),room.z());level.getChunkAt(p);
                                    player.getPersistentData().putBoolean("WayAroundNexusInside",false);
                                    player.teleportTo(level,p.getX()+.5,p.getY(),p.getZ()+.5,java.util.Set.of(),0,0);
                                    context.getSource().sendSuccess(()->Component.literal("NEXUS // inspeção. /nexus return é uma saída administrativa."),false);return 1;
                                }))
                                .then(Commands.literal("return").executes(context->{
                                    var player=context.getSource().getPlayerOrException();var level=player.server.overworld();var p=level.getSharedSpawnPos();
                                    player.getPersistentData().putBoolean("WayAroundNexusInside",false);
                                    player.teleportTo(level,p.getX()+.5,p.getY(),p.getZ()+.5,java.util.Set.of(),0,0);return 1;
                                }))
                                .then(Commands.literal("reopen").executes(context->{
                                    NexusTransitData.get(context.getSource().getServer()).reopen();NexusPortalManager.refreshLoaded(context.getSource().getServer());return 1;
                                }))
                                .then(
                                        Commands.literal(
                                                        "finish"
                                                )
                                                .executes(
                                                        context -> {
                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            boolean finished =
                                                                    NexusEventManager.finishNearest(
                                                                            player
                                                                    );

                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    finished
                                                                                            ? "NEXUS // processo concluído à força."
                                                                                            : "NEXUS // nenhum Nexustor ativo encontrado."
                                                                            ),
                                                                            false
                                                                    );

                                                            return finished
                                                                    ? 1
                                                                    : 0;
                                                        }
                                                )
                                )
                );
    }
}
