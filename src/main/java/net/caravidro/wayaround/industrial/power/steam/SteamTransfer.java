package net.caravidro.wayaround.industrial.power.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SteamTransfer {
    private static final double MIN_PRESSURE_DIFFERENCE = 0.08;

    private SteamTransfer() {}

    public static void balanceAdjacent(ServerLevel level, BlockPos pos, SteamNode source) {
        for (Direction direction : Direction.values()) {
            if (!source.canConnectSteam(direction)) continue;
            BlockPos targetPos = pos.relative(direction);
            if (!level.hasChunkAt(targetPos)) continue;

            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (!(blockEntity instanceof SteamNode target)
                    || !target.canConnectSteam(direction.getOpposite())) continue;

            double difference = source.pressureBar() - target.pressureBar();
            if (difference <= MIN_PRESSURE_DIFFERENCE || source.steamStored() <= 0) continue;

            double sourceFill = source.steamStored() / (double) source.steamCapacity();
            double targetFill = target.steamStored() / (double) target.steamCapacity();
            double fillDifference = sourceFill - targetFill;
            if (fillDifference <= 0.0) continue;

            int equalizing = (int) Math.ceil(
                fillDifference * Math.min(source.steamCapacity(), target.steamCapacity()) * 0.5);
            int offer = Math.min(SteamUnits.TRANSFER_LIMIT_PER_TICK, Math.max(1, equalizing));
            int accepted = target.receiveSteam(offer, true);
            if (accepted <= 0) continue;

            int extracted = source.extractSteam(accepted, false);
            int actuallyAccepted = target.receiveSteam(extracted, false);
            if (actuallyAccepted < extracted)
                source.receiveSteam(extracted - actuallyAccepted, false);
        }
    }
}
