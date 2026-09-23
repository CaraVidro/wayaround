package net.caravidro.wayaround.industrial.power.mechanical;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MechanicalNetwork {
    private static final double FACTOR_EPSILON = 0.02;

    private MechanicalNetwork() {}

    public static void solveIfNeeded(ServerLevel level, MechanicalNode trigger) {
        long gameTime = level.getGameTime();
        if (trigger.lastMechanicalTick() == gameTime) return;

        Map<BlockPos, Entry> entries = new LinkedHashMap<>();
        ArrayDeque<Entry> pending = new ArrayDeque<>();
        Entry root = new Entry(trigger, 1.0);
        entries.put(trigger.mechanicalPos(), root);
        pending.add(root);

        boolean jammed = false;

        while (!pending.isEmpty() && entries.size() < MechanicalUnits.MAX_NETWORK_NODES) {
            Entry current = pending.removeFirst();

            for (Direction side : Direction.values()) {
                double sourcePort = current.node.portFactor(side);
                if (Math.abs(sourcePort) < 0.0001) continue;

                BlockPos targetPos = current.node.mechanicalPos().relative(side);
                if (!level.hasChunkAt(targetPos)) continue;

                BlockEntity targetEntity = level.getBlockEntity(targetPos);
                if (!(targetEntity instanceof MechanicalNode target)) continue;

                double targetPort = target.portFactor(side.getOpposite());
                if (Math.abs(targetPort) < 0.0001) continue;

                double targetFactor = current.factor * sourcePort / targetPort;
                if (!Double.isFinite(targetFactor) || Math.abs(targetFactor) > 16.0)
                    continue;

                Entry previous = entries.get(targetPos);
                if (previous == null) {
                    Entry added = new Entry(target, targetFactor);
                    entries.put(targetPos, added);
                    pending.addLast(added);
                } else if (Math.abs(previous.factor - targetFactor) > FACTOR_EPSILON) {
                    jammed = true;
                }
            }
        }

        double totalInertia = 0.0;
        double weightedRootRpm = 0.0;
        double drive = 0.0;
        double resistance = 0.0;

        for (Entry entry : entries.values()) {
            double factor = entry.factor;
            double equivalentInertia = MechanicalMath.equivalentInertia(entry.node.inertia(), factor);
            totalInertia += equivalentInertia;

            if (Math.abs(factor) > 0.0001)
                weightedRootRpm += (entry.node.mechanicalRpm() / factor) * equivalentInertia;

            drive += MechanicalMath.equivalentTorque(entry.node.driveTorque(), factor);

            double localRpm = Math.abs(entry.node.mechanicalRpm());
            double speedDrag = 1.0 + localRpm / 20.0;
            resistance += (Math.max(0.0, entry.node.frictionTorque()) * speedDrag
                + Math.max(0.0, entry.node.loadTorque())) * Math.abs(factor);
        }

        if (totalInertia <= 0.0001) totalInertia = 1.0;
        double rootRpm = weightedRootRpm / totalInertia;
        double nextRootRpm = MechanicalMath.nextRpm(
            rootRpm, drive, resistance, totalInertia, jammed);

        for (Entry entry : entries.values()) {
            entry.node.setMechanicalRpm(nextRootRpm * entry.factor);
            entry.node.setLastMechanicalTick(gameTime);
        }

        for (Entry entry : entries.values())
            entry.node.afterMechanicalStep(level, jammed);
    }

    private record Entry(MechanicalNode node, double factor) {}
}
