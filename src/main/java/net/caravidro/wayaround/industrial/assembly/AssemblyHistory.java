package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.worldstate.WorldEventTypes;
import net.caravidro.wayaround.worldstate.WorldStateService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** World-history bridge for meaningful Assembly failures. */
public final class AssemblyHistory {

    private AssemblyHistory() {
    }

    public static void recordFailure(
            ServerLevel level,
            AssemblyMachine machine,
            String reason
    ) {
        AssemblySnapshot snapshot =
                machine.assemblySnapshot();

        CompoundTag data =
                new CompoundTag();

        data.putString(
                "assemblyType",
                snapshot.type()
                        .toString()
        );

        data.putString(
                "reason",
                reason == null
                        ? ""
                        : reason
        );

        data.putInt(
                "parts",
                snapshot.parts()
        );

        data.putFloat(
                "workmanship",
                snapshot.workmanship()
        );

        data.putFloat(
                "integrity",
                snapshot.structuralIntegrity()
        );

        data.putFloat(
                "load",
                snapshot.currentLoad()
        );

        data.putFloat(
                "stress",
                snapshot.stressRatio()
        );

        data.putString(
                "weakest",
                snapshot.weakestPart()
        );

        WorldStateService.record(
                level,
                WorldEventTypes.ASSEMBLY_FAILURE,
                machine.assemblyAnchor(),
                null,
                data
        );
    }
}
