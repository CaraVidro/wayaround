package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.util.Mth;

/**
 * Mechanical/structural relationship between two parts.
 */
public record AssemblyConnection(
        String first,
        String second,
        Type type,
        float strength,
        float wear
) {

    public enum Type {
        FASTENED,
        BEARING,
        SHAFT,
        GEAR,
        BELT,
        SUPPORT,
        CONTACT
    }

    public AssemblyConnection {
        strength = Mth.clamp(strength, 0.0F, 1.0F);
        wear = Mth.clamp(wear, 0.0F, 1.0F);
    }

    public float condition() {
        return Mth.clamp(
                strength * (1.0F - wear),
                0.0F,
                1.0F
        );
    }
}
