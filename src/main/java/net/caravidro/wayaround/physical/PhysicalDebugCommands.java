package net.caravidro.wayaround.physical;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.caravidro.wayaround.flow.FlowState;
import net.caravidro.wayaround.flow.RegionVentilationModel;
import net.caravidro.wayaround.flow.UniversalFlow;
import net.caravidro.wayaround.pressure.PressureState;
import net.caravidro.wayaround.pressure.RegionPressureModel;
import net.caravidro.wayaround.pressure.UniversalPressure;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.caravidro.wayaround.thermal.ThermalRegionModel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
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
                                .then(
                                        Commands.literal(
                                                        "pressure"
                                                )
                                                .executes(
                                                        context ->
                                                                inspectPressure(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "flow"
                                                )
                                                .executes(
                                                        context ->
                                                                inspectFlow(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                );
    }


    private static int inspectFlow(
            CommandSourceStack source
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

        boolean water =
                level.getFluidState(
                        seed
                ).is(
                        FluidTags.WATER
                );

        FlowState flow =
                water
                        ? UniversalFlow.waterAt(
                        level,
                        seed
                )
                        : UniversalFlow.atmosphereAt(
                        level,
                        player.position()
                );

        source.sendSuccess(
                () -> Component.literal(
                        "FLUXO UNIVERSAL | "
                                + flow.source()
                                + " | meio "
                                + flow.material().id()
                                + " | velocidade "
                                + String.format(
                                Locale.ROOT,
                                "%.3f",
                                flow.speedMPerS()
                        )
                                + " m/s | vetor ["
                                + String.format(
                                Locale.ROOT,
                                "%.3f, %.3f, %.3f",
                                flow.velocityMPerS().x,
                                flow.velocityMPerS().y,
                                flow.velocityMPerS().z
                        )
                                + "] | densidade "
                                + String.format(
                                Locale.ROOT,
                                "%.3f",
                                flow.densityKgPerM3()
                        )
                                + " kg/m3 | pressao "
                                + String.format(
                                Locale.ROOT,
                                "%.2f",
                                flow.absolutePressureKPa()
                        )
                                + " kPa | qdin "
                                + String.format(
                                Locale.ROOT,
                                "%.4f",
                                flow.dynamicPressureKPa()
                        )
                                + " kPa | turbulencia "
                                + String.format(
                                Locale.ROOT,
                                "%.2f",
                                flow.turbulence()
                        )
                ),
                false
        );

        if (!water) {
            var region =
                    PhysicalRegionScanner.scan(
                            level,
                            seed,
                            PhysicalRegionScanner.DEFAULT_LIMITS
                    );

            if (region.isPresent()
                    && !region.get()
                    .openings()
                    .isEmpty()) {

                RegionVentilationModel ventilation =
                        RegionVentilationModel.from(
                                level,
                                region.get()
                        );

                source.sendSuccess(
                        () -> Component.literal(
                                "VENTILACAO | troca "
                                        + String.format(
                                        Locale.ROOT,
                                        "%.3f",
                                        ventilation.totalExchangeM3PerS()
                                )
                                        + " m3/s | entrada "
                                        + String.format(
                                        Locale.ROOT,
                                        "%.3f",
                                        ventilation.inwardM3PerS()
                                )
                                        + " | saida "
                                        + String.format(
                                        Locale.ROOT,
                                        "%.3f",
                                        ventilation.outwardM3PerS()
                                )
                                        + " | "
                                        + String.format(
                                        Locale.ROOT,
                                        "%.1f",
                                        ventilation.airChangesPerHour()
                                )
                                        + " ACH"
                        ),
                        false
                );
            }
        }

        return 1;
    }


    private static int inspectPressure(
            CommandSourceStack source
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

        PressureState natural =
                UniversalPressure.naturalAt(
                        level,
                        seed
                );

        String absolute =
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        natural.absoluteKPa()
                );

        String absoluteBar =
                String.format(
                        Locale.ROOT,
                        "%.3f",
                        natural.absoluteBar()
                );

        String gaugeBar =
                String.format(
                        Locale.ROOT,
                        "%.3f",
                        natural.gaugeBar()
                );

        String depth =
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        natural.depthM()
                );

        source.sendSuccess(
                () -> Component.literal(
                        "PRESSAO NATURAL | "
                                + natural.source()
                                + " | absoluta "
                                + absolute
                                + " kPa ("
                                + absoluteBar
                                + " bar) | gauge "
                                + gaugeBar
                                + " bar | profundidade "
                                + depth
                                + " m | meio "
                                + natural.medium()
                                        .id()
                ),
                false
        );

        if (natural.source()
                == PressureState.Source.ATMOSPHERE) {
            var region =
                    PhysicalRegionScanner.scan(
                            level,
                            seed,
                            PhysicalRegionScanner.DEFAULT_LIMITS
                    );

            if (region.isPresent()) {
                RegionPressureModel model =
                        UniversalPressure.regionAt(
                                level,
                                region.get(),
                                seed
                        );

                double delta =
                        model.pressure()
                                .absoluteKPa()
                                - natural.absoluteKPa();

                String regionAbsolute =
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                model.pressure()
                                        .absoluteKPa()
                        );

                String regionGauge =
                        String.format(
                                Locale.ROOT,
                                "%.3f",
                                delta / 100.0
                        );

                String maxWallLoad =
                        String.format(
                                Locale.ROOT,
                                "%.1f",
                                model.maxBoundaryLoadN(
                                        region.get()
                                )
                        );

                source.sendSuccess(
                        () -> Component.literal(
                                "PRESSAO DA REGIAO | "
                                        + region.get()
                                                .closure()
                                        + " | "
                                        + regionAbsolute
                                        + " kPa | delta vs natural "
                                        + regionGauge
                                        + " bar | cargaMaxParede "
                                        + maxWallLoad
                                        + " N | temp "
                                        + String.format(
                                        Locale.ROOT,
                                        "%.1f",
                                        model.temperatureC()
                                )
                                        + " C"
                        ),
                        false
                );
            }
        }

        return 1;
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

        ThermalRegionModel thermal =
                ThermalRegionModel.from(
                        level,
                        snapshot
                );

        String localTemperature =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        EnvironmentalTemperature.at(
                                level,
                                seed
                        )
                );

        String ambientTemperature =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        EnvironmentalTemperature.ambientAt(
                                level,
                                seed
                        )
                );

        String thermalLoss =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        thermal.totalConductanceWPerK()
                );

        String thermalTau =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        thermal.timeConstantSeconds()
                );

        RegionVentilationModel ventilation =
                RegionVentilationModel.from(
                        level,
                        snapshot
                );

        String airExchange =
                String.format(
                        Locale.ROOT,
                        "%.3f",
                        ventilation.totalExchangeM3PerS()
                );

        String airChanges =
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        ventilation.airChangesPerHour()
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
                                + " | temp "
                                + localTemperature
                                + " C / ambiente "
                                + ambientTemperature
                                + " C | perdaTermica "
                                + thermalLoss
                                + " W/K | tau "
                                + thermalTau
                                + " s | ventilacao "
                                + airExchange
                                + " m3/s ("
                                + airChanges
                                + " ACH)"
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
