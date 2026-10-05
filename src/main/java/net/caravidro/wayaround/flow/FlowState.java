package net.caravidro.wayaround.flow;

import java.util.Objects;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.minecraft.world.phys.Vec3;

/**
 * Canonical flow sample.
 *
 * <p>The solver that produced this state may be a weather field, open-water
 * approximation, pipe graph or pressure opening. Consumers do not need to
 * know which solver was used.</p>
 */
public record FlowState(
        MaterialDefinition material,
        MatterPhase phase,
        Vec3 velocityMPerS,
        double densityKgPerM3,
        double absolutePressureKPa,
        double volumetricRateM3PerS,
        double turbulence,
        Source source
) {
    public enum Source {
        ATMOSPHERE,
        OPEN_WATER,
        CONDUIT_LIQUID,
        STEAM,
        VENTILATION,
        PRESSURE_RELIEF,
        UNKNOWN
    }

    public FlowState {
        Objects.requireNonNull(
                material,
                "material"
        );

        Objects.requireNonNull(
                phase,
                "phase"
        );

        if (!material.supports(
                phase
        )) {
            throw new IllegalArgumentException(
                    material.id()
                            + " does not support "
                            + phase
            );
        }

        velocityMPerS =
                finiteVector(
                        Objects.requireNonNull(
                                velocityMPerS,
                                "velocityMPerS"
                        )
                );

        densityKgPerM3 =
                finiteNonNegative(
                        densityKgPerM3
                );

        absolutePressureKPa =
                finiteNonNegative(
                        absolutePressureKPa
                );

        volumetricRateM3PerS =
                finiteNonNegative(
                        volumetricRateM3PerS
                );

        turbulence =
                Math.clamp(
                        Double.isFinite(
                                turbulence
                        )
                                ? turbulence
                                : 0.0,
                        0.0,
                        1.0
                );

        Objects.requireNonNull(
                source,
                "source"
        );
    }

    public double speedMPerS() {
        return velocityMPerS.length();
    }

    public Vec3 velocityPerTick() {
        return velocityMPerS.scale(
                1.0 / FlowMath.TICKS_PER_SECOND
        );
    }

    public double massRateKgPerS() {
        return FlowMath.massRateKgPerS(
                volumetricRateM3PerS,
                densityKgPerM3()
        );
    }

    public double dynamicPressureKPa() {
        return FlowMath.dynamicPressureKPa(
                densityKgPerM3(),
                speedMPerS()
        );
    }

    public boolean moving() {
        return speedMPerS()
                > 1.0E-6
                || volumetricRateM3PerS
                > 1.0E-9;
    }

    private static double finiteNonNegative(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? Math.max(
                        0.0,
                        value
                )
                : 0.0;
    }

    private static Vec3 finiteVector(
            Vec3 value
    ) {
        return new Vec3(
                Double.isFinite(
                        value.x
                )
                        ? value.x
                        : 0.0,
                Double.isFinite(
                        value.y
                )
                        ? value.y
                        : 0.0,
                Double.isFinite(
                        value.z
                )
                        ? value.z
                        : 0.0
        );
    }
}
