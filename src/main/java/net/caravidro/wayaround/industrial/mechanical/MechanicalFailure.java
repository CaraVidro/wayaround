package net.caravidro.wayaround.industrial.mechanical;

import net.minecraft.util.Mth;

/**
 * Shared progressive failure rules for mechanical transmission hardware.
 *
 * Failure is state, not disappearance. Components accumulate deformation,
 * tooth damage, bearing damage or belt slip and express those consequences
 * through efficiency, vibration, heat and eventual seizure/snap behavior.
 */
public final class MechanicalFailure {

    public enum Mode {
        HEALTHY,
        WORN,
        MISALIGNED,
        SLIPPING,
        OVERHEATED,
        CRITICAL,
        SEIZED
    }

    private MechanicalFailure() {
    }

    public static float accumulate(
            float current,
            float stress,
            float threshold,
            float rate,
            float condition
    ) {
        float existing =
                Mth.clamp(
                        current,
                        0.0F,
                        1.0F
                );

        float excess =
                Math.max(
                        0.0F,
                        finite(stress) - Math.max(0.0F, threshold)
                );

        float weakness =
                1.0F
                        + (
                        1.0F
                                - Mth.clamp(condition, 0.0F, 1.0F)
                )
                                * 1.35F;

        return Mth.clamp(
                existing
                        + excess
                                * Math.max(0.0F, rate)
                                * weakness,
                0.0F,
                1.0F
        );
    }

    public static float shaftDeformation(
            float current,
            float stress,
            float overspeed,
            float condition
    ) {
        return shaftDeformation(
                current,
                stress,
                overspeed,
                condition,
                1.0F
        );
    }

    public static float shaftDeformation(
            float current,
            float stress,
            float overspeed,
            float condition,
            float materialMultiplier
    ) {
        float response =
                Mth.clamp(
                        materialMultiplier,
                        0.35F,
                        1.75F
                );

        float value =
                accumulate(
                        current,
                        stress,
                        0.72F,
                        0.0055F * response,
                        condition
                );

        return Mth.clamp(
                value
                        + Math.max(0.0F, overspeed)
                                * 0.0018F
                                * response
                                * (
                                1.15F
                                        - Mth.clamp(condition, 0.0F, 1.0F)
                        ),
                0.0F,
                1.0F
        );
    }

    public static float toothDamage(
            float current,
            float stress,
            float overspeed,
            float condition
    ) {
        return toothDamage(
                current,
                stress,
                overspeed,
                condition,
                1.0F
        );
    }

    public static float toothDamage(
            float current,
            float stress,
            float overspeed,
            float condition,
            float materialMultiplier
    ) {
        float response =
                Mth.clamp(
                        materialMultiplier,
                        0.35F,
                        1.75F
                );

        float value =
                accumulate(
                        current,
                        stress,
                        0.82F,
                        0.0065F * response,
                        condition
                );

        return Mth.clamp(
                value
                        + Math.max(0.0F, overspeed)
                                * 0.0024F
                                * response
                                * (
                                1.20F
                                        - Mth.clamp(condition, 0.0F, 1.0F)
                        ),
                0.0F,
                1.0F
        );
    }

    public static float bearingDamage(
            float current,
            float stress,
            float heat,
            float condition
    ) {
        return bearingDamage(
                current,
                stress,
                heat,
                condition,
                1.0F
        );
    }

    public static float bearingDamage(
            float current,
            float stress,
            float heat,
            float condition,
            float materialMultiplier
    ) {
        float response =
                Mth.clamp(
                        materialMultiplier,
                        0.35F,
                        1.75F
                );

        float value =
                accumulate(
                        current,
                        stress,
                        0.62F,
                        0.0045F * response,
                        condition
                );

        return Mth.clamp(
                value
                        + Math.max(
                        0.0F,
                        finite(heat) - 0.52F
                )
                                * 0.0028F
                                * response
                                * (
                                1.25F
                                        - Mth.clamp(condition, 0.0F, 1.0F)
                        ),
                0.0F,
                1.0F
        );
    }

    /**
     * Dynamic slip target. A healthy, lightly-loaded belt stays near zero.
     * Overload, overspeed and poor condition progressively push it toward 1.
     */
    public static float beltSlip(
            float loadRatio,
            float overspeed,
            float condition
    ) {
        float overload =
                Math.max(
                        0.0F,
                        finite(loadRatio) - 0.78F
                );

        float damage =
                1.0F
                        - Mth.clamp(
                        condition,
                        0.0F,
                        1.0F
                );

        return Mth.clamp(
                overload * 0.72F
                        + Math.max(0.0F, overspeed) * 0.28F
                        + damage * 0.48F,
                0.0F,
                1.0F
        );
    }

    public static float transmissionFactor(
            float deformation,
            float toothDamage,
            float bearingDamage
    ) {
        return Mth.clamp(
                1.0F
                        - Mth.clamp(deformation, 0.0F, 1.0F) * 0.24F
                        - Mth.clamp(toothDamage, 0.0F, 1.0F) * 0.34F
                        - Mth.clamp(bearingDamage, 0.0F, 1.0F) * 0.28F,
                0.08F,
                1.0F
        );
    }

    public static float vibration(
            float deformation,
            float toothDamage,
            float bearingDamage
    ) {
        return Mth.clamp(
                Mth.clamp(deformation, 0.0F, 1.0F) * 0.72F
                        + Mth.clamp(toothDamage, 0.0F, 1.0F) * 0.88F
                        + Mth.clamp(bearingDamage, 0.0F, 1.0F) * 0.52F,
                0.0F,
                1.5F
        );
    }

    public static boolean seized(
            float condition,
            float deformation,
            float toothDamage,
            float bearingDamage
    ) {
        return condition <= 0.012F
                || deformation >= 0.985F
                || toothDamage >= 0.985F
                || bearingDamage >= 0.985F;
    }

    public static Mode classify(
            float heat,
            float deformation,
            float toothDamage,
            float bearingDamage,
            float slip,
            boolean seized
    ) {
        if (seized) {
            return Mode.SEIZED;
        }

        float worst =
                Math.max(
                        Math.max(
                                Mth.clamp(deformation, 0.0F, 1.0F),
                                Mth.clamp(toothDamage, 0.0F, 1.0F)
                        ),
                        Mth.clamp(bearingDamage, 0.0F, 1.0F)
                );

        if (worst >= 0.82F) {
            return Mode.CRITICAL;
        }

        if (heat >= 0.90F) {
            return Mode.OVERHEATED;
        }

        if (slip >= 0.24F) {
            return Mode.SLIPPING;
        }

        if (deformation >= 0.22F
                || toothDamage >= 0.22F) {
            return Mode.MISALIGNED;
        }

        if (worst >= 0.08F) {
            return Mode.WORN;
        }

        return Mode.HEALTHY;
    }

    private static float finite(
            float value
    ) {
        return Float.isFinite(value)
                ? Math.max(0.0F, value)
                : 0.0F;
    }
}
