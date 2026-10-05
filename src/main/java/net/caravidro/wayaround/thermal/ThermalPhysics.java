package net.caravidro.wayaround.thermal;

import net.caravidro.wayaround.physical.MaterialDefinition;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.MatterState;
import net.caravidro.wayaround.physical.PhysicalBoundaryFace;

/**
 * Solver-independent thermal math shared by matter, rooms and machinery.
 *
 * <p>Internal energy uses joules, power uses watts, temperature uses Celsius
 * differences and geometry uses SI-like Minecraft metres. Celsius is valid for
 * temperature differences because one Celsius degree equals one kelvin.</p>
 */
public final class ThermalPhysics {

    private ThermalPhysics() {
    }

    public static double heatCapacityJPerK(
            MatterState matter
    ) {
        return heatCapacityJPerK(
                matter.material(),
                matter.phase(),
                matter.massKg()
        );
    }

    public static double heatCapacityJPerK(
            MaterialDefinition material,
            MatterPhase phase,
            double massKg
    ) {
        if (!Double.isFinite(
                massKg
        )
                || massKg < 0.0) {
            throw new IllegalArgumentException(
                    "massKg must be finite and >= 0"
            );
        }

        return massKg
                * material.phase(
                phase
        ).specificHeatJPerKgK();
    }

    public static double energyForTemperatureChangeJ(
            MatterState matter,
            double targetTemperatureC
    ) {
        if (!Double.isFinite(
                targetTemperatureC
        )) {
            throw new IllegalArgumentException(
                    "targetTemperatureC must be finite"
            );
        }

        return heatCapacityJPerK(
                matter
        )
                * (
                targetTemperatureC
                        - matter.temperatureC()
        );
    }

    public static double temperatureAfterEnergy(
            MatterState matter,
            double energyJ
    ) {
        if (!Double.isFinite(
                energyJ
        )) {
            throw new IllegalArgumentException(
                    "energyJ must be finite"
            );
        }

        double capacity =
                heatCapacityJPerK(
                        matter
                );

        if (capacity <= 1.0E-9) {
            return matter.temperatureC();
        }

        return TemperatureCurve.clamp(
                matter.temperatureC()
                        + energyJ
                                / capacity
        );
    }

    public static double boundaryConductanceWPerK(
            PhysicalBoundaryFace boundary
    ) {
        MaterialDefinition material =
                boundary.material();

        MatterPhase phase =
                material.supports(
                        MatterPhase.SOLID
                )
                        ? MatterPhase.SOLID
                        : material.defaultPhase();

        double conductivity =
                material.phase(
                        phase
                ).thermalConductivityWPerMK();

        return conductivity
                * boundary.areaM2()
                / Math.max(
                0.01,
                boundary.thicknessM()
        );
    }

    public static double conductivePowerW(
            PhysicalBoundaryFace boundary,
            double insideTemperatureC,
            double outsideTemperatureC
    ) {
        if (!Double.isFinite(
                insideTemperatureC
        )
                || !Double.isFinite(
                outsideTemperatureC
        )) {
            return 0.0;
        }

        return boundaryConductanceWPerK(
                boundary
        )
                * (
                insideTemperatureC
                        - outsideTemperatureC
        );
    }

    /**
     * Compatibility mapping for MaterialMemory's historic normalized heat
     * input. New callers can provide real Celsius while old persisted values
     * remain valid.
     */
    public static float normalizedLegacyHeat(
            MaterialDefinition material,
            double temperatureC
    ) {
        if (!Double.isFinite(
                temperatureC
        )) {
            return 0.0F;
        }

        double reference =
                damageReferenceTemperatureC(
                        material
                );

        double normalized =
                (
                        temperatureC
                                - TemperatureCurve.AMBIENT
                )
                        / Math.max(
                        1.0,
                        reference
                                - TemperatureCurve.AMBIENT
                );

        return (float) Math.clamp(
                normalized,
                0.0,
                2.0
        );
    }

    private static double damageReferenceTemperatureC(
            MaterialDefinition material
    ) {
        var transitions =
                material.transitions();

        if (transitions.hasMeltingPoint()) {
            return Math.max(
                    120.0,
                    transitions.meltingPointC()
                            * 0.75
            );
        }

        if (transitions.hasIgnitionPoint()) {
            return Math.max(
                    80.0,
                    transitions.ignitionTemperatureC()
                            * 0.90
            );
        }

        return 900.0;
    }
}
