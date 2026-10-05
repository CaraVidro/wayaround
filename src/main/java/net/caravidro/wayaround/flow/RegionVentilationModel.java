package net.caravidro.wayaround.flow;

import net.caravidro.wayaround.physical.PhysicalOpening;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;
import net.caravidro.wayaround.pressure.RegionPressureModel;
import net.caravidro.wayaround.pressure.UniversalPressure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Lumped ventilation summary for one physical region.
 */
public record RegionVentilationModel(
        double outwardM3PerS,
        double inwardM3PerS,
        double totalExchangeM3PerS,
        double airChangesPerHour,
        Vec3 netTransportM3PerS
) {

    public static RegionVentilationModel from(
            ServerLevel level,
            PhysicalRegionSnapshot region
    ) {
        if (region.openings().isEmpty()) {
            return new RegionVentilationModel(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Vec3.ZERO
            );
        }

        RegionPressureModel inside =
                UniversalPressure.regionAt(
                        level,
                        region,
                        region.seed()
                );

        FlowState wind =
                UniversalFlow.atmosphereAt(
                        level,
                        Vec3.atCenterOf(
                                region.seed()
                        )
                );

        double outward =
                0.0;

        double inward =
                0.0;

        Vec3 transport =
                Vec3.ZERO;

        for (PhysicalOpening opening :
                region.openings()) {

            FlowState flow =
                    UniversalFlow.ventilationAt(
                            level,
                            opening,
                            inside,
                            wind
                    );

            Vec3 outwardNormal =
                    new Vec3(
                            opening.outward()
                                    .getStepX(),
                            opening.outward()
                                    .getStepY(),
                            opening.outward()
                                    .getStepZ()
                    );

            double signed =
                    flow.velocityMPerS()
                            .dot(
                                    outwardNormal
                            )
                            >= 0.0
                            ? flow.volumetricRateM3PerS()
                            : -flow.volumetricRateM3PerS();

            if (signed >= 0.0) {
                outward +=
                        signed;
            } else {
                inward +=
                        -signed;
            }

            if (flow.velocityMPerS()
                    .lengthSqr()
                    > 1.0E-12) {
                transport =
                        transport.add(
                                flow.velocityMPerS()
                                        .normalize()
                                        .scale(
                                                flow.volumetricRateM3PerS()
                                        )
                        );
            }
        }

        /*
         * In a balanced cross-flow the same parcel entering is matched by one
         * leaving. Counting both would double ACH and thermal advection.
         */
        double exchange =
                Math.max(
                        outward,
                        inward
                );

        double volume =
                Math.max(
                        0.05,
                        region.volumeM3()
                );

        double airChanges =
                exchange
                        / volume
                        * 3600.0;

        return new RegionVentilationModel(
                outward,
                inward,
                exchange,
                airChanges,
                transport
        );
    }
}
