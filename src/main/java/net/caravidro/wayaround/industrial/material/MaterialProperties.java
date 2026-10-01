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
            float ductility
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
    }

    private MaterialProperties() {
    }

    public static Traits of(
            AssemblyPartProfile.Material material
    ) {
        return switch (material) {
            case STONE -> new Traits(0.01F, 0.74F, 0.94F, 0.18F);
            case WOOD -> new Traits(0.02F, 0.30F, 0.58F, 0.42F);
            case FIBER -> new Traits(0.01F, 0.22F, 0.52F, 0.72F);
            case COPPER -> new Traits(1.00F, 0.66F, 0.62F, 0.86F);
            case BRONZE -> new Traits(0.46F, 0.72F, 0.76F, 0.70F);
            case IRON -> new Traits(0.30F, 0.80F, 0.44F, 0.58F);
            case STEEL -> new Traits(0.24F, 0.91F, 0.72F, 0.52F);
            case DIAMOND -> new Traits(0.02F, 0.98F, 0.99F, 0.08F);
        };
    }
}
