package net.caravidro.wayaround.physical;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Compatibility bridge from Minecraft/NeoForge fluids into the universal
 * material vocabulary.
 */
public final class FluidMatterResolver {

    private FluidMatterResolver() {
    }

    public static MaterialDefinition resolve(
            FluidStack fluid
    ) {
        if (fluid == null
                || fluid.isEmpty()) {
            return PhysicalMaterials.UNKNOWN_LIQUID;
        }

        if (fluid.getFluid()
                .is(
                        FluidTags.WATER
                )) {
            return PhysicalMaterials.WATER;
        }

        if (fluid.getFluid()
                .is(
                        FluidTags.LAVA
                )) {
            return PhysicalMaterials.MOLTEN_ROCK;
        }

        return PhysicalMaterials.UNKNOWN_LIQUID;
    }

    public static MaterialDefinition resolve(
            FluidState fluid
    ) {
        if (fluid == null
                || fluid.isEmpty()) {
            return PhysicalMaterials.UNKNOWN_LIQUID;
        }

        if (fluid.is(
                FluidTags.WATER
        )) {
            return PhysicalMaterials.WATER;
        }

        if (fluid.is(
                FluidTags.LAVA
        )) {
            return PhysicalMaterials.MOLTEN_ROCK;
        }

        return PhysicalMaterials.UNKNOWN_LIQUID;
    }
}
