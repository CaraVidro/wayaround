package net.caravidro.wayaround.industrial.steam;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.pipework.IndustrialPipeBlock;
import net.caravidro.wayaround.industrial.pipework.PipeBlockEntity;
import net.caravidro.wayaround.industrial.pipework.PipeSpec;
import net.caravidro.wayaround.interaction.StructuralDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Bounded steam routing over the existing pipework topology.
 *
 * No chunks are loaded to complete a route. Pressure and temperature limits
 * come from the actual installed pipe specs.
 */
public final class SteamNetwork {

    private static final int MAX_PIPES = 256;
    private static final int MAX_RECEIVERS = 32;

    public record Delivery(
            int delivered,
            int nextReceiver,
            int pipeCount,
            int receiverCount,
            float bottleneckPressureBar,
            int bottleneckTemperatureC
    ) {}

    private SteamNetwork() {
    }

    public static Delivery distribute(
            ServerLevel level,
            BlockPos source,
            int availableSteam,
            float pressureBar,
            int temperatureC,
            int nextReceiver
    ) {
        if (availableSteam <= 0
                || pressureBar <= 0.01F) {
            return new Delivery(
                    0,
                    nextReceiver,
                    0,
                    0,
                    0.0F,
                    0
            );
        }

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        Set<BlockPos> visited =
                new HashSet<>();

        Set<BlockPos> receiverPositions =
                new HashSet<>();

        List<SteamReceiver> receivers =
                new ArrayList<>();

        int bottleneckFlow =
                Integer.MAX_VALUE;

        float bottleneckPressure =
                Float.MAX_VALUE;

        int bottleneckTemperature =
                Integer.MAX_VALUE;

        for (Direction direction :
                Direction.values()) {

            BlockPos adjacent =
                    source.relative(
                            direction
                    );

            if (!level.hasChunkAt(
                    adjacent
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            adjacent
                    );

            if (isSteamPipe(
                    state
            )) {
                visited.add(
                        adjacent
                );

                pending.addLast(
                        adjacent
                );
            }
        }

        while (!pending.isEmpty()
                && visited.size() <= MAX_PIPES
                && receivers.size() < MAX_RECEIVERS) {

            BlockPos current =
                    pending.removeFirst();

            BlockState state =
                    level.getBlockState(
                            current
                    );

            if (!(state.getBlock()
                    instanceof IndustrialPipeBlock pipe)) {
                continue;
            }

            PipeSpec spec =
                    pipe.spec();

            bottleneckFlow =
                    Math.min(
                            bottleneckFlow,
                            spec.flowPerTick()
                    );

            bottleneckPressure =
                    Math.min(
                            bottleneckPressure,
                            spec.maxPressureBar()
                    );

            bottleneckTemperature =
                    Math.min(
                            bottleneckTemperature,
                            spec.maxTemperatureC()
                    );

            abusePipeIfNecessary(
                    level,
                    current,
                    pressureBar,
                    temperatureC,
                    spec
            );

            for (Direction direction :
                    Direction.values()) {

                BlockPos next =
                        current.relative(
                                direction
                        );

                if (!level.hasChunkAt(
                        next
                )
                        || next.equals(
                        source
                )) {
                    continue;
                }

                BlockState nextState =
                        level.getBlockState(
                                next
                        );

                if (isSteamPipe(
                        nextState
                )) {
                    if (visited.size() < MAX_PIPES
                            && visited.add(
                            next
                    )) {
                        pending.addLast(
                                next
                        );
                    }

                    continue;
                }

                if (receiverPositions.contains(
                        next
                )) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                next
                        );

                if (blockEntity
                        instanceof SteamReceiver receiver) {

                    receiverPositions.add(
                            next
                    );

                    receivers.add(
                            receiver
                    );

                    if (receivers.size()
                            >= MAX_RECEIVERS) {
                        break;
                    }
                }
            }
        }

        if (receivers.isEmpty()
                || bottleneckFlow == Integer.MAX_VALUE) {

            return new Delivery(
                    0,
                    nextReceiver,
                    visited.size(),
                    receivers.size(),
                    bottleneckPressure == Float.MAX_VALUE
                            ? 0.0F
                            : bottleneckPressure,
                    bottleneckTemperature == Integer.MAX_VALUE
                            ? 0
                            : bottleneckTemperature
            );
        }

        int transferBudget =
                Math.min(
                        availableSteam,
                        Math.max(
                                1,
                                bottleneckFlow * 10
                        )
                );

        float deliveredPressure =
                Math.min(
                        pressureBar,
                        bottleneckPressure
                );

        int deliveredTemperature =
                Math.min(
                        temperatureC,
                        bottleneckTemperature
                );

        int start =
                Math.floorMod(
                        nextReceiver,
                        receivers.size()
                );

        int remaining =
                transferBudget;

        for (int index = 0;
             index < receivers.size()
                     && remaining > 0;
             index++) {

            SteamReceiver receiver =
                    receivers.get(
                            (
                                    start + index
                            )
                                    % receivers.size()
                    );

            int fairShare =
                    Math.max(
                            1,
                            (
                                    remaining
                                            + (
                                            receivers.size()
                                                    - index
                                    )
                                            - 1
                            )
                                    / (
                                    receivers.size()
                                            - index
                            )
                    );

            int accepted =
                    receiver.receiveSteam(
                            fairShare,
                            deliveredPressure,
                            deliveredTemperature
                    );

            remaining -=
                    Math.clamp(
                            accepted,
                            0,
                            fairShare
                    );
        }

        int delivered =
                transferBudget
                        - remaining;

        return new Delivery(
                delivered,
                nextReceiver + 1,
                visited.size(),
                receivers.size(),
                bottleneckPressure,
                bottleneckTemperature
        );
    }

    private static boolean isSteamPipe(
            BlockState state
    ) {
        return state.getBlock()
                instanceof IndustrialPipeBlock pipe
                && pipe.spec()
                        .supports(
                                PipeSpec.PipeMedium.STEAM
                        );
    }

    private static void abusePipeIfNecessary(
            ServerLevel level,
            BlockPos pos,
            float pressureBar,
            int temperatureC,
            PipeSpec spec
    ) {
        float pressureExcess =
                Math.max(
                        0.0F,
                        pressureBar
                                - spec.maxPressureBar()
                );

        float thermalExcess =
                Math.max(
                        0.0F,
                        temperatureC
                                - spec.maxTemperatureC()
                );

        if (pressureExcess <= 0.0F
                && thermalExcess <= 0.0F) {
            return;
        }

        if (level.getBlockEntity(
                pos
        ) instanceof PipeBlockEntity pipe) {

            float damage =
                    pressureExcess
                            * 0.014F
                            + thermalExcess
                                    * 0.00055F;

            pipe.receiveStructuralDamage(
                    new StructuralDamage(
                            Vec3.atCenterOf(
                                    pos
                            ),
                            damage,
                            pressureExcess
                                    * 0.12F,
                            ResourceLocation.fromNamespaceAndPath(
                                    WayAround.MODID,
                                    "steam_overpressure"
                            ),
                            null
                    )
            );
        }
    }
}
