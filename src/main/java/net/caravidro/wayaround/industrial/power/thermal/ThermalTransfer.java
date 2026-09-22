package net.caravidro.wayaround.industrial.power.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Phase-one heat transfer: direct adjacency only.
 * Later heat pipes can replace this routing without changing producer/receiver contracts.
 */
public final class ThermalTransfer {
    private ThermalTransfer() {}

    public static int pushAdjacent(ServerLevel level, BlockPos sourcePos, HeatSource source, int nextSide) {
        int remaining = Math.max(0, source.heatOutputPerTick());
        Direction[] directions = Direction.values();
        int start = Math.floorMod(nextSide, directions.length);

        for (int i = 0; i < directions.length && remaining > 0; i++) {
            Direction direction = directions[(start + i) % directions.length];
            if (!source.canOutputHeat(direction)) continue;
            BlockPos targetPos = sourcePos.relative(direction);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity target = level.getBlockEntity(targetPos);
            if (!(target instanceof HeatReceiver receiver)) continue;
            int accepted = receiver.receiveHeat(direction.getOpposite(), remaining, false);
            remaining -= Math.max(0, Math.min(remaining, accepted));
        }
        return (start + 1) % directions.length;
    }
}
