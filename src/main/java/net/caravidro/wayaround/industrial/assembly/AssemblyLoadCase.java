package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.world.phys.Vec3;

/**
 * A load presented to an AssemblyGraph without prescribing a visual outcome.
 */
public record AssemblyLoadCase(
        Kind kind,
        float magnitude,
        Vec3 direction,
        float cyclicity
) {

    public enum Kind {
        STATIC,
        TENSION,
        COMPRESSION,
        SHEAR,
        BENDING,
        TORSION,
        IMPACT,
        VIBRATION,
        PRESSURE,
        HYDRAULIC,
        MECHANICAL
    }

    public AssemblyLoadCase {
        magnitude =
                Math.max(
                        0.0F,
                        magnitude
                );

        direction =
                direction == null
                        || direction.lengthSqr() < 1.0E-8
                        ? Vec3.ZERO
                        : direction.normalize();

        cyclicity =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                cyclicity
                        )
                );
    }

    public static AssemblyLoadCase mechanical(
            float magnitude,
            Vec3 direction,
            float cyclicity
    ) {
        return new AssemblyLoadCase(
                Kind.MECHANICAL,
                magnitude,
                direction,
                cyclicity
        );
    }

    public static AssemblyLoadCase pressure(
            float pressureSideA,
            float pressureSideB,
            float exposedArea,
            Vec3 normalFromAToB
    ) {
        float delta =
                pressureSideA
                        - pressureSideB;

        float magnitude =
                Math.abs(
                        delta
                )
                        * Math.max(
                        0.0F,
                        exposedArea
                );

        Vec3 normal =
                normalFromAToB == null
                        ? Vec3.ZERO
                        : normalFromAToB;

        if (delta < 0.0F) {
            normal =
                    normal.scale(
                            -1.0
                    );
        }

        return new AssemblyLoadCase(
                Kind.PRESSURE,
                magnitude,
                normal,
                0.0F
        );
    }
}
