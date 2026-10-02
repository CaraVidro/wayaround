package net.caravidro.wayaround.industrial.material;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.minecraft.util.Mth;

/**
 * Shared physical traits for materials already used by Assembly.
 *
 * Values are normalized gameplay properties, not SI measurements. They give
 * mechanics, aging and electronics one material vocabulary.
 */
public final class MaterialProperties {

    public record Traits(
            float electricalConductivity,
            float thermalTolerance,
            float corrosionResistance,
            float ductility,
            float mechanicalStrength,
            float hardness,
            float fatigueEndurance,
            float vibrationDamping,
            float friction
    ) {
        public float conductivityAfter(
                MaterialMemory memory
        ) {
            if (memory == null) {
                return electricalConductivity;
            }

            return Mth.clamp(
                    electricalConductivity
                            * memory.conductivityFactor(),
                    0.0F,
                    1.0F
            );
        }

        /**
         * Low-strength, ductile materials visibly yield sooner. Very brittle
         * materials resist bending but do not receive a free fatigue bonus.
         */
        public float shaftDeformationMultiplier() {
            return Mth.clamp(
                    1.42F
                            - mechanicalStrength * 0.58F
                            - fatigueEndurance * 0.22F
                            + ductility * 0.18F,
                    0.48F,
                    1.48F
            );
        }

        /**
         * Gear teeth care about surface hardness and cyclic endurance.
         * Low ductility slightly increases shock sensitivity.
         */
        public float gearDamageMultiplier() {
            return Mth.clamp(
                    1.55F
                            - hardness * 0.66F
                            - fatigueEndurance * 0.32F
                            + (1.0F - ductility) * 0.16F,
                    0.42F,
                    1.58F
            );
        }

        /**
         * Bearings reward low friction and good fatigue endurance.
         */
        public float bearingDamageMultiplier() {
            return Mth.clamp(
                    0.72F
                            + friction * 0.55F
                            + (1.0F - fatigueEndurance) * 0.38F,
                    0.54F,
                    1.55F
            );
        }

        public float heatGenerationMultiplier() {
            return Mth.clamp(
                    0.70F
                            + friction * 0.62F
                            + (1.0F - thermalTolerance) * 0.20F,
                    0.58F,
                    1.46F
            );
        }

        public float vibrationFactor() {
            return Mth.clamp(
                    1.18F
                            - vibrationDamping * 0.55F,
                    0.58F,
                    1.18F
            );
        }

        public float cyclicWearMultiplier() {
            return Mth.clamp(
                    1.28F
                            - fatigueEndurance * 0.48F
                            - hardness * 0.12F,
                    0.62F,
                    1.26F
            );
        }
    }

    private MaterialProperties() {
    }

    public static Traits of(
            AssemblyPartProfile.Material material
    ) {
        return switch (material) {
            case STONE -> new Traits(
                    0.01F, 0.74F, 0.94F, 0.18F,
                    0.54F, 0.72F, 0.32F, 0.40F, 0.72F
            );
            case WOOD -> new Traits(
                    0.02F, 0.30F, 0.58F, 0.42F,
                    0.38F, 0.28F, 0.48F, 0.82F, 0.58F
            );
            case FIBER -> new Traits(
                    0.01F, 0.22F, 0.52F, 0.72F,
                    0.22F, 0.12F, 0.42F, 0.94F, 0.70F
            );
            case COPPER -> new Traits(
                    1.00F, 0.66F, 0.62F, 0.86F,
                    0.48F, 0.32F, 0.55F, 0.64F, 0.48F
            );
            case BRONZE -> new Traits(
                    0.46F, 0.72F, 0.76F, 0.70F,
                    0.72F, 0.68F, 0.74F, 0.58F, 0.36F
            );
            case IRON -> new Traits(
                    0.30F, 0.80F, 0.44F, 0.58F,
                    0.78F, 0.70F, 0.68F, 0.45F, 0.48F
            );
            case STEEL -> new Traits(
                    0.24F, 0.91F, 0.72F, 0.52F,
                    0.96F, 0.92F, 0.94F, 0.38F, 0.30F
            );
            case DIAMOND -> new Traits(
                    0.02F, 0.98F, 0.99F, 0.08F,
                    0.88F, 1.00F, 0.40F, 0.18F, 0.22F
            );
        };
    }
}
