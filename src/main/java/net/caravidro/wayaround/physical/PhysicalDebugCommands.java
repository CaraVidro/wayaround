package net.caravidro.wayaround.physical;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Runtime inspection for the universal physical-region layer.
 *
 * <p>Stand inside a room/cavity and run /wayaroundphysics region. Openings are
 * marked with particles so a vanilla build can be used as the test fixture.</p>
 */
public final class PhysicalDebugCommands {

    private PhysicalDebugCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wayaroundphysics"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "region"
                                                )
                                                .executes(
                                                        context ->
                                                                inspect(
                                                                        context.getSource(),
                                                                        PhysicalRegionScanner.DEFAULT_LIMITS
                                                                )
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "radius",
                                                                        IntegerArgumentType.integer(
                                                                                2,
                                                                                64
                                                                        )
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                inspect(
                                                                                        context.getSource(),
                                                                                        new PhysicalRegionScanner.ScanLimits(
                                                                                                2_048,
                                                                                                IntegerArgumentType.getInteger(
                                                                                                        context,
                                                                                                        "radius"
                                                                                                )
                                                                                        )
                                                                                )
                                                                )
                                                                .then(
                                                                        Commands.argument(
                                                                                        "maxCells",
                                                                                        IntegerArgumentType.integer(
                                                                                                16,
                                                                                                16_384
                                                                                        )
                                                                                )
                                                                                .executes(
                                                                                        context ->
                                                                                                inspect(
                                                                                                        context.getSource(),
                                                                                                        new PhysicalRegionScanner.ScanLimits(
                                                                                                                IntegerArgumentType.getInteger(
                                                                                                                        context,
                                                                                                                        "maxCells"
                                                                                                                ),
                                                                                                                IntegerArgumentType.getInteger(
                                                                                                                        context,
                                                                                                                        "radius"
                                                                                                                )
                                                                                                        )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static int inspect(
            CommandSourceStack source,
            PhysicalRegionScanner.ScanLimits limits
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        ServerLevel level =
                player.serverLevel();

        BlockPos seed =
                BlockPos.containing(
                        player.getX(),
                        player.getEyeY(),
                        player.getZ()
                );

        var result =
                PhysicalRegionScanner.scan(
                        level,
                        seed,
                        limits
                );

        if (result.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "REGIAO FISICA | o ponto atual nao e um volume de materia atravessavel ou nao esta carregado."
                    )
            );

            return 0;
        }

        PhysicalRegionSnapshot snapshot =
                result.get();

        int shown =
                0;

        for (PhysicalOpening opening :
                snapshot.openings()) {
            if (shown++
                    >= 48) {
                break;
            }

            BlockPos pos =
                    opening.outsideCell();

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    2,
                    0.15,
                    0.15,
                    0.15,
                    0.0
            );
        }

        String volume =
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        snapshot.volumeM3()
                );

        String walls =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        snapshot.boundaryAreaM2()
                );

        String openings =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        snapshot.openingAreaM2()
                );

        source.sendSuccess(
                () -> Component.literal(
                        "REGIAO FISICA | "
                                + snapshot.closure()
                                + " | celulas "
                                + snapshot.cells()
                                        .size()
                                + " | volume "
                                + volume
                                + " m3 | paredes "
                                + walls
                                + " m2 | aberturas "
                                + snapshot.openings()
                                        .size()
                                + " ("
                                + openings
                                + " m2)"
                                + " | limiteCelulas="
                                + snapshot.hitCellLimit()
                                + " limiteDistancia="
                                + snapshot.hitDistanceLimit()
                                + " chunkNaoCarregado="
                                + snapshot.touchedUnloadedChunk()
                ),
                false
        );

        return snapshot.cells()
                .size();
    }
}
