package net.caravidro.wayaround.jujutsu;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class JujutsuCommands {
    private JujutsuCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("jujutsu")
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("cast")
                                .executes(context -> {
                                    JujutsuManager.cast(context.getSource().getPlayerOrException());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("give")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("combination", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(JujutsuTechnique.ids(), builder))
                                        .executes(context -> giveTechnique(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "combination")
                                        ))))
                        .then(Commands.literal("list")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> list(context.getSource().getPlayerOrException())))
        );

        // Debug shorthand requested by the design:
        // /give jujutsu <combination>
        // Brigadier merges this literal child with vanilla's existing /give root.
        event.getDispatcher().register(
                Commands.literal("give")
                        .then(Commands.literal("jujutsu")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("combination", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(JujutsuTechnique.ids(), builder))
                                        .executes(context -> giveTechnique(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "combination")
                                        ))))
        );

        event.getDispatcher().register(
                Commands.literal("spectrum")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                java.util.List.of("void", "tukuna", "justice"),
                                                builder
                                        ))
                                .executes(context -> giveSpectrum(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "type")
                                )))
        );
    }

    private static int status(ServerPlayer player) {
        player.sendSystemMessage(
                Component.literal(JujutsuManager.publicStatus(player))
                        .withStyle(ChatFormatting.AQUA)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int giveTechnique(ServerPlayer player, String raw) {
        JujutsuTechnique technique = JujutsuTechnique.find(raw).orElse(null);
        if (technique == null) {
            player.sendSystemMessage(Component.literal(
                    "Combinação não reconhecida. Ex.: summon-ice-explosion-touch "
                            + "ou 'sumonar + gelo + explosões + ao toque'."
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        JujutsuManager.forceTechnique(player, technique);
        return Command.SINGLE_SUCCESS;
    }

    private static int giveSpectrum(ServerPlayer player, String raw) {
        SpectrumType type = switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "void", "vazio", "gojo" -> SpectrumType.VOID;
            case "tukuna", "sukuna" -> SpectrumType.TUKUNA;
            case "justice", "justica", "justiça" -> SpectrumType.JUSTICE;
            default -> null;
        };

        if (type == null) {
            player.sendSystemMessage(Component.literal(
                    "Spectrum desconhecido. Use: void, tukuna ou justice."
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        JujutsuManager.forceSpectrum(player, type);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "Catálogo procedural: " + JujutsuTechnique.catalog().size() + " combinações."
        ).withStyle(ChatFormatting.LIGHT_PURPLE));

        int shown = 0;
        for (JujutsuTechnique technique : JujutsuTechnique.catalog()) {
            player.sendSystemMessage(Component.literal(
                    technique.id() + " -> " + technique.displayName()
            ).withStyle(ChatFormatting.GRAY));
            if (++shown >= 12) break;
        }

        player.sendSystemMessage(Component.literal(
                "Use TAB em /give jujutsu para procurar as demais."
        ).withStyle(ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }
}
