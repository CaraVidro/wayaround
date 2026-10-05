package net.caravidro.wayaround.physical;

import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Immutable definition of a material.
 *
 * <p>A material is not a phase. Water, for example, can expose solid, liquid
 * and gas phase properties while retaining one material identity.</p>
 */
public record MaterialDefinition(
        ResourceLocation id,
        MatterPhase defaultPhase,
        Map<MatterPhase, PhaseProperties> phases,
        PhaseTransitions transitions,
        EngineeringProperties engineering
) {
    public MaterialDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(defaultPhase, "defaultPhase");
        phases = Map.copyOf(Objects.requireNonNull(phases, "phases"));
        transitions = Objects.requireNonNull(transitions, "transitions");
        engineering = Objects.requireNonNull(engineering, "engineering");

        if (phases.isEmpty() || !phases.containsKey(defaultPhase)) {
            throw new IllegalArgumentException(
                    "Material must define its default phase: " + id
            );
        }
    }

    public boolean supports(
            MatterPhase phase
    ) {
        return phases.containsKey(phase);
    }

    public PhaseProperties phase(
            MatterPhase phase
    ) {
        PhaseProperties properties =
                phases.get(phase);

        if (properties == null) {
            throw new IllegalArgumentException(
                    id + " does not define phase " + phase
            );
        }

        return properties;
    }

    public record PhaseProperties(
            double densityKgPerM3,
            double specificHeatJPerKgK,
            double thermalConductivityWPerMK,
            double compressibilityPerPa,
            double dynamicViscosityPaS
    ) {
        public PhaseProperties {
            densityKgPerM3 = positive(densityKgPerM3, "density");
            specificHeatJPerKgK = positive(specificHeatJPerKgK, "specificHeat");
            thermalConductivityWPerMK =
                    nonNegative(
                            thermalConductivityWPerMK,
                            "thermalConductivity"
                    );
            compressibilityPerPa =
                    nonNegative(
                            compressibilityPerPa,
                            "compressibility"
                    );
            dynamicViscosityPaS =
                    nonNegative(
                            dynamicViscosityPaS,
                            "dynamicViscosity"
                    );
        }
    }

    /**
     * NaN means "not modelled / not applicable yet".
     */
    public record PhaseTransitions(
            double meltingPointC,
            double boilingPointC,
            double ignitionTemperatureC
    ) {
        public boolean hasMeltingPoint() {
            return Double.isFinite(meltingPointC);
        }

        public boolean hasBoilingPoint() {
            return Double.isFinite(boilingPointC);
        }

        public boolean hasIgnitionPoint() {
            return Double.isFinite(ignitionTemperatureC);
        }
    }

    /**
     * Normalized gameplay engineering traits shared with the older Assembly,
     * wear and electronics systems. Keeping these here prevents each subsystem
     * from maintaining a different idea of "steel" or "wood".
     */
    public record EngineeringProperties(
            float electricalConductivity,
            float thermalTolerance,
            float corrosionResistance,
            float ductility,
            float mechanicalStrength,
            float hardness,
            float fatigueEndurance,
            float vibrationDamping,
            float friction,
            float workability,
            float assemblyResistance,
            float assemblyFatigueResistance,
            float flammability
    ) {
        public EngineeringProperties {
            electricalConductivity = unit(electricalConductivity);
            thermalTolerance = unit(thermalTolerance);
            corrosionResistance = unit(corrosionResistance);
            ductility = unit(ductility);
            mechanicalStrength = unit(mechanicalStrength);
            hardness = unit(hardness);
            fatigueEndurance = unit(fatigueEndurance);
            vibrationDamping = unit(vibrationDamping);
            friction = unit(friction);
            workability = unit(workability);
            assemblyResistance = unit(assemblyResistance);
            assemblyFatigueResistance = unit(assemblyFatigueResistance);
            flammability = unit(flammability);
        }
    }

    private static double positive(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value <= 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and > 0"
            );
        }

        return value;
    }

    private static double nonNegative(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and >= 0"
            );
        }

        return value;
    }

    private static float unit(
            float value
    ) {
        return Float.isFinite(value)
                ? Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                )
                : 0.0F;
    }
}
