package net.caravidro.wayaround.industrial.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Industrial material quality is distinct from ordinary Minecraft material
 * identity. Vanilla iron remains useful, but it is the low-grade reference.
 */
public enum IndustrialMetalGrade {
    LOW(0.60F, 0.86F, 0.72F),
    MEDIUM(0.82F, 1.00F, 0.86F),
    HIGH(0.96F, 1.12F, 0.96F);

    private final float workmanshipFloor;
    private final float resistanceMultiplier;
    private final float fatigueFactor;

    IndustrialMetalGrade(
            float workmanshipFloor,
            float resistanceMultiplier,
            float fatigueFactor
    ) {
        this.workmanshipFloor = workmanshipFloor;
        this.resistanceMultiplier = resistanceMultiplier;
        this.fatigueFactor = fatigueFactor;
    }

    public float workmanshipFloor() {
        return workmanshipFloor;
    }

    public float resistanceMultiplier() {
        return resistanceMultiplier;
    }

    public float fatigueFactor() {
        return fatigueFactor;
    }

    public static IndustrialMetalGrade of(
            ItemStack stack
    ) {
        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                );

        String path =
                id == null
                        ? ""
                        : id.getPath();

        if (path.contains("precision_iron")
                || path.contains("high_grade_iron")) {
            return HIGH;
        }

        if (path.contains("refined_iron")
                || path.contains("industrial_iron")) {
            return MEDIUM;
        }

        return LOW;
    }
}
