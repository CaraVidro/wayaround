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

                if (visited.contains(
                        neighbor
                )
                        || !level.hasChunkAt(
                        neighbor
                )) {
                    continue;
                }

                if (level.getBlockEntity(
                        neighbor
                )
                        instanceof AssemblyMachine) {
                    queue.addLast(
                            neighbor.immutable()
                    );
                }
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
}
