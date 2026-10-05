package net.caravidro.wayaround.flow;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.PhysicalOpening;
import net.caravidro.wayaround.pressure.PressureState;
import net.minecraft.world.phys.Vec3;

/**
 * Pressure-driven transfer through a known physical opening.
 */
public final class OpeningFlow {

    private static final double DEFAULT_DISCHARGE_COEFFICIENT =
            0.62;

    private OpeningFlow() {
    }

    public static FlowState between(
            MaterialDefinition medium,
            MatterPhase phase,
            PressureState inside,
            PressureState outside,
            PhysicalOpening opening
    ) {
        double density =
                medium.phase(
                        phase
                ).densityKgPerM3();

        double signedRate =
                FlowMath.signedOrificeRateM3PerS(
                        inside.absoluteKPa(),
                        outside.absoluteKPa(),
                        density,
                        opening.areaM2(),
                        DEFAULT_DISCHARGE_COEFFICIENT
                );

        Vec3 outward =
                new Vec3(
                        opening.outward()
                                .getStepX(),
                        opening.outward()
                                .getStepY(),
                        opening.outward()
                                .getStepZ()
                );

        Vec3 direction =
                signedRate >= 0.0
                        ? outward
                        : outward.scale(
                                -1.0
                        );

        double speed =
                FlowMath.velocityFromVolumetricRateMPerS(
                        Math.abs(
                                signedRate
                        ),
                        opening.areaM2()
                );

        return new FlowState(
                medium,
                phase,
                direction.scale(
                        speed
                ),
                density,
                Math.max(
                        inside.absoluteKPa(),
                        outside.absoluteKPa()
                ),
                Math.abs(
                        signedRate
                ),
                Math.clamp(
                        speed / 35.0,
                        0.0,
                        1.0
                ),
                FlowState.Source.VENTILATION
        );
    }
}
