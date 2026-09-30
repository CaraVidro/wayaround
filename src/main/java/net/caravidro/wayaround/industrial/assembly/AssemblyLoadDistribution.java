package net.caravidro.wayaround.industrial.assembly;

import java.util.Map;

/**
 * Immutable result of routing one machine/network load through an AssemblyGraph.
 *
 * Load values use the machine's native Assembly load units. Stress values are
 * normalized against per-part/per-connection carrying capacity.
 */
public record AssemblyLoadDistribution(
        Map<String, Float> partLoads,
        Map<String, Float> partStress,
        Map<String, Float> connectionLoads,
        Map<String, Float> connectionStress,
        float supportedRatio,
        float unsupportedLoad,
        float maxPartStress,
        float maxConnectionStress,
        String hottestPart,
        String hottestConnection
) {

    public AssemblyLoadDistribution {
        partLoads =
                Map.copyOf(
                        partLoads
                );

        partStress =
                Map.copyOf(
                        partStress
                );

        connectionLoads =
                Map.copyOf(
                        connectionLoads
                );

        connectionStress =
                Map.copyOf(
                        connectionStress
                );

        hottestPart =
                hottestPart == null
                        ? ""
                        : hottestPart;

        hottestConnection =
                hottestConnection == null
                        ? ""
                        : hottestConnection;
    }

    public float partLoad(
            String id
    ) {
        return partLoads.getOrDefault(
                id,
                0.0F
        );
    }

    public float partStress(
            String id
    ) {
        return partStress.getOrDefault(
                id,
                0.0F
        );
    }

    public float connectionLoad(
            String id
    ) {
        return connectionLoads.getOrDefault(
                id,
                0.0F
        );
    }

    public float connectionStress(
            String id
    ) {
        return connectionStress.getOrDefault(
                id,
                0.0F
        );
    }

    public float maxLocalizedStress() {
        return Math.max(
                maxPartStress,
                maxConnectionStress
        );
    }
}
