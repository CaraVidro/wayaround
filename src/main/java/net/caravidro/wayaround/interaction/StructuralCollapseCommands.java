package net.caravidro.wayaround.interaction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug entry point for the structural-collapse engine.
 *
 * <p>Aim at the part of a building that should be demolished, then use:
 * {@code /wayaroundcollapse test [scanRadius] [force]}.</p>
 */
public final class StructuralCollapseCommands {

    private StructuralCollapseCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal(
                                "wayaroundcollapse"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "test"
                                        )
                                        .executes(
                                                context ->
                                                        execute(
                                                                context.getSource(),
                                                                StructuralCollapseManager.DEFAULT_SCAN_RADIUS,
                                                                48.0F
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "radius",
                                                                IntegerArgumentType.integer(
                                                                        6,
                                                                        StructuralCollapseManager.MAX_SCAN_RADIUS
                                                                )
                                                        )
                                                        .executes(
                                                                context ->
                                                                        execute(
                                                                                context.getSource(),
                                                                                IntegerArgumentType.getInteger(
                                                                                        context,
                                                                                        "radius"
                                                                                ),
                                                                                48.0F
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "force",
                                                                                FloatArgumentType.floatArg(
                                                                                        4.0F,
                                                                                        160.0F
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        execute(
                                                                                                context.getSource(),
                                                                                                IntegerArgumentType.getInteger(
                                                                                                        context,
                                                                                                        "radius"
                                                                                                ),
                                                                                                FloatArgumentType.getFloat(
                                                                                                        context,
                                                                                                        "force"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int execute(
            CommandSourceStack source,
            int radius,
            float force
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        ServerLevel level =
                player.serverLevel();

        Vec3 eye =
                player.getEyePosition();

        Vec3 end =
                eye.add(
                        player.getViewVector(
                                        1.0F
                                )
                                .scale(
                                        96.0
                                )
                );

        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                eye,
                                end,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player
                        )
                );

        if (hit.getType()
                != HitResult.Type.BLOCK) {
            source.sendFailure(
                    Component.literal(
                            "Mire em um bloco da estrutura para testar o colapso."
                    )
            );

            return 0;
        }

        Vec3 origin =
                Vec3.atCenterOf(
                        hit.getBlockPos()
                );

        StructuralDamage damage =
                new StructuralDamage(
                        origin,
                        force,
                        force * 0.35F,
                        ResourceLocation.fromNamespaceAndPath(
                                WayAround.MODID,
                                "debug_demolition"
                        ),
                        player.getUUID()
                );

        StructuralCollapseManager.CollapseResult result =
                StructuralCollapseManager.applyDamage(
                        level,
                        damage,
                        radius
                );

        if (!result.collapsedAnything()) {
            source.sendFailure(
                    Component.literal(
                            "Análise estrutural: "
                                    + result.scannedBlocks()
                                    + " blocos lidos, mas nenhuma seção perdeu suporte. "
                                    + "Tente mirar mais perto de pilares/apoios ou aumentar a força."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "COLAPSO | lidos "
                                + result.scannedBlocks()
                                + " | falha direta "
                                + result.directlyFailedBlocks()
                                + " | sobrecarga "
                                + result.overloadedBlocks()
                                + " | ainda estáveis "
                                + result.stableBlocks()
                                + " | caindo "
                                + result.fallingBlocks()
                                + " em "
                                + result.clusters()
                                + " partes | queda máx "
                                + result.maxFallDistance()
                                + " blocos"
                ),
                true
        );

        return result.fallingBlocks();
    }
}
