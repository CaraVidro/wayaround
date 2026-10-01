package net.caravidro.wayaround.industrial.grid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HighVoltageNetwork {

    private static final int MAX_CABLES = 512;
    private static final int MAX_RECEIVERS = 32;
    private static final int MAX_RAW_TRANSFER = 8_192;

    public record Delivery(
            int accepted,
            int rawSpent,
            int nextReceiver,
            int cableCount,
            float efficiency
    ) {}

    private record Endpoint(
            HighVoltageReceiver receiver,
            Direction side
    ) {}

    private HighVoltageNetwork() {
    }

    public static Delivery distribute(
            ServerLevel level,
            BlockPos source,
            Direction outputSide,
            int availableRaw,
            int nextReceiver,
            float sourceEfficiency
    ) {
        if (outputSide == null
                || availableRaw <= 0) {
            return new Delivery(
                    0,
                    0,
                    nextReceiver,
                    0,
                    1.0F
            );
        }

        BlockPos first =
                source.relative(
                        outputSide
                );

        if (!level.hasChunkAt(
                first
        )) {
            return new Delivery(
                    0,
                    0,
                    nextReceiver,
                    0,
                    1.0F
            );
        }

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        Set<BlockPos> cables =
                new HashSet<>();

        Set<BlockPos> endpointsSeen =
                new HashSet<>();

        List<Endpoint> endpoints =
                new ArrayList<>();

        BlockState firstState =
                level.getBlockState(
                        first
                );

        if (firstState.getBlock()
                instanceof HighVoltageCableBlock) {
            cables.add(
                    first
            );
            pending.addLast(
                    first
            );

        } else {
            addEndpoint(
                    level,
                    first,
                    outputSide.getOpposite(),
                    endpointsSeen,
                    endpoints
            );
        }

        while (!pending.isEmpty()
                && endpoints.size() < MAX_RECEIVERS) {
            BlockPos current =
                    pending.removeFirst();

            for (Direction direction :
                    Direction.values()) {
                BlockPos next =
                        current.relative(
                                direction
                        );

                if (next.equals(
                        source
                )
                        || !level.hasChunkAt(
                        next
                )) {
                    continue;
                }

                BlockState state =
                        level.getBlockState(
                                next
                        );

                if (state.getBlock()
                        instanceof HighVoltageCableBlock) {
                    if (cables.size() < MAX_CABLES
                            && cables.add(
                            next
                    )) {
                        pending.addLast(
                                next
                        );
                    }
                    continue;
                }

                addEndpoint(
                        level,
                        next,
                        direction.getOpposite(),
                        endpointsSeen,
                        endpoints
                );
            }
        }

        if (endpoints.isEmpty()) {
            return new Delivery(
                    0,
                    0,
                    nextReceiver,
                    cables.size(),
                    GridPhysics.highVoltageEfficiency(
                            cables.size(),
                            sourceEfficiency
                    )
            );
        }

        float efficiency =
                GridPhysics.highVoltageEfficiency(
                        cables.size(),
                        sourceEfficiency
                );

        int rawBudget =
                Math.min(
                        availableRaw,
                        MAX_RAW_TRANSFER
                );

        int usable =
                Math.max(
                        0,
                        (int) Math.floor(
                                rawBudget
                                        * efficiency
                        )
                );

        int remaining =
                usable;

        int start =
                Math.floorMod(
                        nextReceiver++,
                        endpoints.size()
                );

        for (int index = 0;
             index < endpoints.size()
                     && remaining > 0;
             index++) {

            Endpoint endpoint =
                    endpoints.get(
                            (
                                    start + index
                            )
                                    % endpoints.size()
                    );

            int fairShare =
                    Math.max(
                            1,
                            (
                                    remaining
                                            + (
                                            endpoints.size()
                                                    - index
                                    )
                                            - 1
                            )
                                    / (
                                    endpoints.size()
                                            - index
                            )
                    );

            int accepted =
                    endpoint.receiver()
                            .receiveHighVoltage(
                                    fairShare,
                                    endpoint.side()
                            );

            remaining -=
                    Math.clamp(
                            accepted,
                            0,
                            fairShare
                    );
        }

        int accepted =
                usable
                        - remaining;

        int rawSpent =
                accepted <= 0
                        ? 0
                        : Math.min(
                        rawBudget,
                        (int) Math.ceil(
                                accepted
                                        / Math.max(
                                        0.01F,
                                        efficiency
                                )
                        )
                );

        return new Delivery(
                accepted,
                rawSpent,
                nextReceiver,
                cables.size(),
                efficiency
        );
    }

    private static void addEndpoint(
            ServerLevel level,
            BlockPos pos,
            Direction side,
            Set<BlockPos> seen,
            List<Endpoint> endpoints
    ) {
        if (seen.contains(
                pos
        )) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (blockEntity
                instanceof HighVoltageReceiver receiver) {
            seen.add(
                    pos
            );

            endpoints.add(
                    new Endpoint(
                            receiver,
                            side
                    )
            );
        }
    }

}
