package net.caravidro.wayaround.environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common-safe client mirror used by deterministic cloud sampling.
 *
 * <p>No Minecraft client classes live here, so dedicated servers can load the
 * class safely. Only network handlers mutate it on a physical client.</p>
 */
public final class EnvironmentalFieldClientCache {

    private static final int MAX_CELLS =
            512;

    private static final LinkedHashMap<Long, EnvironmentalFields.Snapshot> CELLS =
            new LinkedHashMap<>();

    private EnvironmentalFieldClientCache() {
    }

    public static synchronized void receive(
            long key,
            EnvironmentalFields.Snapshot snapshot
    ) {
        CELLS.remove(
                key
        );

        CELLS.put(
                key,
                snapshot
        );

        while (CELLS.size()
                > MAX_CELLS) {
            CELLS.remove(
                    CELLS.keySet()
                            .iterator()
                            .next()
            );
        }
    }

    public static synchronized EnvironmentalFields.Snapshot get(
            double x,
            double z
    ) {
        return CELLS.get(
                EnvironmentalFields.key(
                        EnvironmentalFields.cellX(
                                x
                        ),
                        EnvironmentalFields.cellZ(
                                z
                        )
                )
        );
    }

    public static synchronized void clear() {
        CELLS.clear();
    }

    public static synchronized Map<Long, EnvironmentalFields.Snapshot> snapshot() {
        return Map.copyOf(
                CELLS
        );
    }
}
