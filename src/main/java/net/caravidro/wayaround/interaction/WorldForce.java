package net.caravidro.wayaround.interaction;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A physical influence emitted into the world without knowing what will react.
 */
public record WorldForce(
        Vec3 origin,
        Vec3 direction,
        double radius,
        float magnitude,
        Kind kind,
        ResourceLocation source,
        @Nullable UUID actor
) {

    public enum Kind {
        PUSH,
        PULL,
        IMPACT,
        EXPLOSION,
        VIBRATION,
        HYDRAULIC,
        MECHANICAL
    }

    public WorldForce {
        direction =
                direction == null
                        || direction.lengthSqr() < 0.000001
                        ? Vec3.ZERO
                        : direction.normalize();

        radius =
                Math.max(
                        0.0,
                        radius
                );

        magnitude =
                Math.max(
                        0.0F,
                        magnitude
                );
    }

    public float magnitudeAt(
            Vec3 target
    ) {
        if (radius <= 0.0) {
            return target.distanceToSqr(
                    origin
            ) < 0.25
                    ? magnitude
                    : 0.0F;
        }

        double distance =
                target.distanceTo(
                        origin
                );

        if (distance > radius) {
            return 0.0F;
        }

        double falloff =
                1.0
                        - distance
                                / radius;

        return (float) (
                magnitude
                        * (
                        0.15
                                + falloff
                                        * falloff
                                        * 0.85
                )
        );
    }
}
