package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Compatibility bridge for machines that existed before staged physical
 * assembly. They expose virtual components to Assembly Engine while keeping
 * their existing gameplay and save format.
 */
public final class LegacyMachineAssembly {

    private LegacyMachineAssembly() {
    }

    public static AssemblyPartNode part(
            String id,
            String role,
            AssemblyPartProfile.Kind kind,
            AssemblyPartProfile.Material material,
            ResourceLocation sourceItem,
            float wear,
            boolean supported,
            float loadShare
    ) {
        return new AssemblyPartNode(
                id,
                role,
                AssemblyPartProfile.legacy(
                        kind,
                        material,
                        sourceItem,
                        0,
                        Mth.clamp(
                                wear,
                                0.0F,
                                1.0F
                        )
                ),
                supported,
                loadShare
        );
    }

    public static float addWear(
            float current,
            float fraction,
            float multiplier
    ) {
        return Mth.clamp(
                current
                        + Math.max(
                        0.0F,
                        fraction
                )
                        * Math.max(
                        0.0F,
                        multiplier
                ),
                0.0F,
                1.0F
        );
    }
}
