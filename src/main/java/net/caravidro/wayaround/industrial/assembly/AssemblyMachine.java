package net.caravidro.wayaround.industrial.assembly;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Contract implemented by machines constructed from physical parts.
 *
 * <p>Simulation stays in the machine. Assembly Engine only provides shared
 * inspection, health and external-load semantics.</p>
 */
public interface AssemblyMachine {

    ResourceLocation assemblyType();

    BlockPos assemblyAnchor();

    Collection<AssemblyPartNode> assemblyParts();

    Collection<AssemblyConnection> assemblyConnections();

    float currentAssemblyLoad();

    void applyAssemblyWear(float fraction);

    default AssemblySnapshot assemblySnapshot() {
        return AssemblyEngine.inspect(
                this
        );
    }
}
