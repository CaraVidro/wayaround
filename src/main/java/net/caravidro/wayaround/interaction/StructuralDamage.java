package net.caravidro.wayaround.interaction;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Structural damage packet independent of combat damage. */
public record StructuralDamage(
        Vec3 origin,
        float amount,
        float impulse,
        ResourceLocation source,
        @Nullable UUID actor
) {

    public StructuralDamage {
        amount =
                Math.max(
                        0.0F,
                        amount
                );

        impulse =
                Math.max(
                        0.0F,
                        impulse
                );
    }
}
