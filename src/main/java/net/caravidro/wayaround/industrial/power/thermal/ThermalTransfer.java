package net.caravidro.wayaround.industrial.power.thermal;

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

/**
 * Bounded heat routing through adjacent components and heat conduits.
 * The search never force-loads chunks.
 */
public final class ThermalTransfer {
    private static final int MAX_CONDUITS = 128;
    private static final int MAX_RECEIVERS = 32;

    private ThermalTransfer() {}

    public static int pushNetwork(ServerLevel level, BlockPos sourcePos, HeatSource source, int nextReceiver) {
        int available = Math.max(0, source.heatOutputPerTick());
        if (available <= 0) return nextReceiver;

        List<HeatReceiver> receivers = new ArrayList<>();
        Set<BlockPos> receiverPositions = new HashSet<>();
        Set<BlockPos> conduits = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(sourcePos);

        while (!pending.isEmpty() && receivers.size() < MAX_RECEIVERS) {
            BlockPos current = pending.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (adjacent.equals(sourcePos) || !level.hasChunkAt(adjacent)) continue;

                BlockState state = level.getBlockState(adjacent);
                if (state.getBlock() instanceof HeatConduitBlock) {
                    if (conduits.size() < MAX_CONDUITS && conduits.add(adjacent))
                        pending.addLast(adjacent);
                    continue;
                }

                if (!receiverPositions.add(adjacent)) continue;
                BlockEntity target = level.getBlockEntity(adjacent);
                if (target instanceof HeatReceiver receiver)
                    receivers.add(receiver);
            }
        }

        if (receivers.isEmpty()) return nextReceiver;

        int start = Math.floorMod(nextReceiver++, receivers.size());
        int remaining = available;

        for (int i = 0; i < receivers.size() && remaining > 0; i++) {
            HeatReceiver receiver = receivers.get((start + i) % receivers.size());
            int left = receivers.size() - i;
            int offer = Math.max(1, remaining / left);
            int accepted = receiver.receiveHeat(Direction.UP, offer, false);
            remaining -= Math.max(0, Math.min(offer, accepted));
        }

        return nextReceiver;
    }
}
