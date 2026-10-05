package net.caravidro.wayaround.pressure;

import net.caravidro.wayaround.physical.MatterState;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Canonical pressure facade used by migrated WayAround systems.
 */
public final class UniversalPressure {

    private UniversalPressure() {
    }

    public static PressureState naturalAt(
            ServerLevel level,
            BlockPos pos
    ) {
        return NaturalPressure.at(
                level,
                pos
        );
    }

    public static RegionPressureModel regionAt(
            ServerLevel level,
            PhysicalRegionSnapshot region,
            BlockPos sample
    ) {
        return RegionPressureModel.at(
                level,
                region,
                sample
        );
    }

    public static MatterState resolveGas(
            MatterState gas
    ) {
        return gas.withPressure(
                PressurePhysics.gasAbsoluteKPa(
                        gas
                )
        );
    }
}
