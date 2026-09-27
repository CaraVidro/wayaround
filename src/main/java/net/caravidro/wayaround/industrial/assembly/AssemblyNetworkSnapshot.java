package net.caravidro.wayaround.industrial.assembly;

public record AssemblyNetworkSnapshot(
        int machines,
        int parts,
        int connections,
        float averageIntegrity,
        float totalLoad,
        float totalCapacity,
        float loadRatio,
        int criticalMachines
) {
}
