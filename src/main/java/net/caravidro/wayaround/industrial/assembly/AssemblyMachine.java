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

    /**
     * Called when a localized physical constraint actually fails.
     *
     * Machines with real detachable/breakable components should override this
     * and translate the generic event into their own state. The fallback keeps
     * older machines compatible by converting failure severity into wear.
     */
    default void applyAssemblyFailure(
            AssemblyFailureEvent failure
    ) {
        if (failure == null) {
            return;
        }

        applyAssemblyWear(
                Math.min(
                        0.35F,
                        0.035F
                                + failure.severity()
                                        * 0.08F
                )
        );
    }

    default AssemblySnapshot assemblySnapshot() {
        return AssemblyEngine.inspect(
                this
        );
    }
}
