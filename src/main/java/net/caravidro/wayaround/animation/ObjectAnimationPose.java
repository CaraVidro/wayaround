package net.caravidro.wayaround.animation;

import net.minecraft.util.Mth;

/**
 * Reusable transform for any animated object/part.
 *
 * Translation is in model-local blocks, rotation is in degrees and scale is
 * multiplicative. The type deliberately knows nothing about a specific model.
 */
public record ObjectAnimationPose(
        float x,
        float y,
        float z,
        float xRot,
        float yRot,
        float zRot,
        float xScale,
        float yScale,
        float zScale
) {
    public static final ObjectAnimationPose IDENTITY =
            new ObjectAnimationPose(
                    0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F,
                    1.0F, 1.0F, 1.0F
            );

    public static ObjectAnimationPose translated(
            float x,
            float y,
            float z
    ) {
        return new ObjectAnimationPose(
                x, y, z,
                0.0F, 0.0F, 0.0F,
                1.0F, 1.0F, 1.0F
        );
    }

    public static ObjectAnimationPose rotated(
            float xRot,
            float yRot,
            float zRot
    ) {
        return new ObjectAnimationPose(
                0.0F, 0.0F, 0.0F,
                xRot, yRot, zRot,
                1.0F, 1.0F, 1.0F
        );
    }

    public ObjectAnimationPose withTranslation(
            float x,
            float y,
            float z
    ) {
        return new ObjectAnimationPose(
                x, y, z,
                xRot, yRot, zRot,
                xScale, yScale, zScale
        );
    }

    public ObjectAnimationPose withRotation(
            float xRot,
            float yRot,
            float zRot
    ) {
        return new ObjectAnimationPose(
                x, y, z,
                xRot, yRot, zRot,
                xScale, yScale, zScale
        );
    }

    public ObjectAnimationPose withScale(
            float xScale,
            float yScale,
            float zScale
    ) {
        return new ObjectAnimationPose(
                x, y, z,
                xRot, yRot, zRot,
                xScale, yScale, zScale
        );
    }

    public static ObjectAnimationPose lerp(
            ObjectAnimationPose from,
            ObjectAnimationPose to,
            float amount
    ) {
        float t =
                Mth.clamp(
                        amount,
                        0.0F,
                        1.0F
                );

        return new ObjectAnimationPose(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z),
                Mth.lerp(t, from.xRot, to.xRot),
                Mth.lerp(t, from.yRot, to.yRot),
                Mth.lerp(t, from.zRot, to.zRot),
                Mth.lerp(t, from.xScale, to.xScale),
                Mth.lerp(t, from.yScale, to.yScale),
                Mth.lerp(t, from.zScale, to.zScale)
        );
    }
}
