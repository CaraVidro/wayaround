package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.resources.ResourceLocation;

/** Immutable inspection result for any Assembly Engine machine. */
public record AssemblySnapshot(
        ResourceLocation type,
        int parts,
        int connections,
        float workmanship,
        float structuralIntegrity,
        float supportRatio,
        float loadCapacity,
        float currentLoad,
        float stressRatio,
        String weakestPart,
        boolean valid,
        boolean critical
) {
}
