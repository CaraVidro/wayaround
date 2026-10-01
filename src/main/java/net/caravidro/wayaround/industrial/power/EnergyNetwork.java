package net.caravidro.wayaround.industrial.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EnergyNetwork {

    private static final int TRANSFER_BUDGET = 1280;
    private static final int MAX_CABLES = 128;
    private static final int MAX_RECEIVERS = 64;

    private EnergyNetwork() {
    }

    /**
     * Normal low-voltage distribution. A fresh bounded search avoids stale
     * routes, chunk loading and power crossing broken wires.
     */
    public static int distribute(
            ServerLevel level,
            BlockPos worldPosition,
            EnergyBudget buffer,
            int nextReceiver
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.POWER_NETWORKS
        )) {
            return nextReceiver;
        }

        List<IEnergyStorage> receivers =
                new ArrayList<>();

        Set<BlockPos> receiverPositions =
                new HashSet<>();

        Set<BlockPos> cables =
                new HashSet<>();

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        pending.add(
                worldPosition
        );

        collect(
                level,
                worldPosition,
                pending,
                cables,
                receiverPositions,
                receivers
        );

        return transfer(
                buffer,
                receivers,
                nextReceiver,
                TRANSFER_BUDGET
        );
    }

    /**
     * Distribute only through the network physically attached to one side.
     *
     * Transformers and protection devices use this so their output cannot
     * immediately backfeed into the cable feeding their input.
     */
    public static int distributeFromSide(
            ServerLevel level,
            BlockPos source,
            Direction outputSide,
            EnergyBudget buffer,
            int nextReceiver
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.POWER_NETWORKS
        )
                || outputSide == null
                || buffer.stored() <= 0) {
            return nextReceiver;
        }

        BlockPos first =
                source.relative(
                        outputSide
                );

        if (!level.hasChunkAt(
                first
        )) {
            return nextReceiver;
        }

        List<IEnergyStorage> receivers =
                new ArrayList<>();

        Set<BlockPos> receiverPositions =
                new HashSet<>();

        Set<BlockPos> cables =
                new HashSet<>();

        ArrayDeque<BlockPos> pending =
                new ArrayDeque<>();

        BlockState firstState =
                level.getBlockState(
                        first
                );

        if (firstState.getBlock()
                instanceof EnergyCableBlock) {
            cables.add(
                    first
            );

            pending.addLast(
                    first
            );

            collect(
                    level,
                    source,
                    pending,
                    cables,
                    receiverPositions,
                    receivers
            );

        } else {
            IEnergyStorage direct =
                    level.getCapability(
                            Capabilities.EnergyStorage.BLOCK,
                            first,
                            outputSide.getOpposite()
                    );

            if (direct != null
                    && direct.canReceive()) {
                receivers.add(
                        direct
                );
            }
        }

        return transfer(
                buffer,
                receivers,
                nextReceiver,
                TRANSFER_BUDGET
        );
    }

    private static void collect(
            ServerLevel level,
            BlockPos source,
            ArrayDeque<BlockPos> pending,
            Set<BlockPos> cables,
            Set<BlockPos> receiverPositions,
            List<IEnergyStorage> receivers
    ) {
        while (!pending.isEmpty()
                && receivers.size() < MAX_RECEIVERS) {

            BlockPos current =
                    pending.removeFirst();

            for (Direction direction :
                    Direction.values()) {

                BlockPos adjacent =
                        current.relative(
                                direction
                        );

                if (adjacent.equals(
                        source
                )
                        || !level.hasChunkAt(
                        adjacent
                )) {
                    continue;
                }

                BlockState state =
                        level.getBlockState(
                                adjacent
                        );

                if (state.getBlock()
                        instanceof EnergyCableBlock) {
                    if (cables.size() < MAX_CABLES
                            && cables.add(
                            adjacent
                    )) {
                        pending.addLast(
                                adjacent
                        );
                    }

                    continue;
                }

                if (receiverPositions.contains(
                        adjacent
                )) {
                    continue;
                }

                IEnergyStorage receiver =
                        level.getCapability(
                                Capabilities.EnergyStorage.BLOCK,
                                adjacent,
                                direction.getOpposite()
                        );

                if (receiver != null
                        && receiver.canReceive()) {
                    receiverPositions.add(
                            adjacent
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
    }

    private static int transfer(
            EnergyBudget buffer,
            List<IEnergyStorage> receivers,
            int nextReceiver,
            int transferBudget
    ) {
        if (receivers.isEmpty()) {
            return nextReceiver;
        }

        int remaining =
                Math.min(
                        transferBudget,
                        buffer.stored()
                );

        int start =
                Math.floorMod(
                        nextReceiver++,
                        receivers.size()
                );

        for (int index = 0;
             index < receivers.size()
                     && remaining > 0
                     && buffer.stored() > 0;
             index++) {

            IEnergyStorage receiver =
                    receivers.get(
                            (
                                    start + index
                            )
                                    % receivers.size()
                    );

            remaining -=
                    buffer.transferTo(
                            remaining,
                            offered -> receiver.receiveEnergy(
                                    offered,
                                    false
                            )
                    );
        }

        return nextReceiver;
    }
}
