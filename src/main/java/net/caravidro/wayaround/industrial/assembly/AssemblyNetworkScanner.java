package net.caravidro.wayaround.industrial.assembly;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Bounded flood-fill across adjacent AssemblyMachine block entities.
 */
public final class AssemblyNetworkScanner {

    private static final int MAX_MACHINES =
            128;

    private AssemblyNetworkScanner() {
    }

    public static AssemblyNetworkSnapshot inspect(
            ServerLevel level,
            BlockPos start
    ) {
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        Set<BlockPos> visited =
                new HashSet<>();

        queue.add(
                start.immutable()
        );

        int machines =
                0;

        int parts =
                0;

        int connections =
                0;

        int critical =
                0;

        float integritySum =
                0.0F;

        float load =
                0.0F;

        float capacity =
                0.0F;

        while (!queue.isEmpty()
                && machines < MAX_MACHINES) {
            BlockPos pos =
                    queue.removeFirst();

            if (!visited.add(
                    pos
            )
                    || !level.hasChunkAt(
                    pos
            )) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity
                    instanceof AssemblyMachine machine)) {
                continue;
            }

            AssemblySnapshot snapshot =
                    machine.assemblySnapshot();

            machines++;
            parts +=
                    snapshot.parts();

            connections +=
                    snapshot.connections();

            integritySum +=
                    snapshot.structuralIntegrity();

            load +=
                    snapshot.currentLoad();

            capacity +=
                    snapshot.loadCapacity();

            if (snapshot.critical()) {
                critical++;
            }

            for (Direction direction :
                    Direction.values()) {
                BlockPos neighbor =
                        pos.relative(
                                direction
                        );

                enqueueMachine(
                        level,
                        queue,
                        visited,
                        pos,
                        neighbor
                );
            }

            for (BlockPos linked :
                    machine.assemblyLinkedAnchors()) {
                if (linked == null) {
                    continue;
                }

                enqueueMachine(
                        level,
                        queue,
                        visited,
                        pos,
                        linked
                );
            }
        }

        float averageIntegrity =
                machines == 0
                        ? 0.0F
                        : integritySum
                                / machines;

        float loadRatio =
                capacity <= 0.0001F
                        ? (
                        load <= 0.0001F
                                ? 0.0F
                                : Float.POSITIVE_INFINITY
                )
                        : load
                                / capacity;

        return new AssemblyNetworkSnapshot(
                machines,
                parts,
                connections,
                averageIntegrity,
                load,
                capacity,
                loadRatio,
                critical
        );
    }

    private static void enqueueMachine(
            ServerLevel level,
            ArrayDeque<BlockPos> queue,
            Set<BlockPos> visited,
            BlockPos origin,
            BlockPos candidate
    ) {
        if (candidate == null
                || candidate.equals(origin)
                || visited.contains(candidate)
                || !level.hasChunkAt(candidate)) {
            return;
        }

        /*
         * External assembly links are intentionally bounded. A belt/coupling
         * must not turn one inspection into a world-scale chunk walk.
         */
        if (origin.distSqr(candidate) > 4096.0D) {
            return;
        }

        if (level.getBlockEntity(candidate)
                instanceof AssemblyMachine) {
            queue.addLast(candidate.immutable());
        }
    }
}
