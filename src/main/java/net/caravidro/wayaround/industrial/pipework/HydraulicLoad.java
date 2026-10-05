package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.pressure.PressureMath;
import net.minecraft.util.Mth;

/**
 * Shared hydraulic load rules for mechanically-driven liquid machines.
 *
 * This is intentionally a bounded gameplay model, not a CFD solver. It turns
 * flow demand, discharge restriction, suction quality and pump speed into the
 * same kind of scalar operating signals already used by MechanicalLoad.
 */
public final class HydraulicLoad {

    public record Demand(
            float desiredFlow,
            float effectiveFlow,
            float pressureBar,
            float requiredPower,
            float requiredTorque,
            float cavitation,
            float backpressure,
            float vibration,
            float hydraulicStress
    ) {
        public float flowFactor() {
            if (desiredFlow <= 0.001F) {
                return 0.0F;
            }

            return Mth.clamp(
                    effectiveFlow / desiredFlow,
                    0.0F,
                    1.0F
            );
        }
    }

    private HydraulicLoad() {
    }

    public static Demand evaluate(
            float driveRpm,
            float condition,
            float networkFlowCapacity,
            float networkPressureRating,
            float requestedFlow,
            float cavitation,
            float backpressure
    ) {
        float speed =
                Math.abs(
                        finite(driveRpm)
                );

        float health =
                Mth.clamp(
                        condition,
                        0.0F,
                        1.0F
                );

        float flowCapacity =
                Math.max(
                        0.0F,
                        finite(networkFlowCapacity)
                );

        float pressureRating =
                Math.max(
                        0.0F,
                        finite(networkPressureRating)
                );

        float requested =
                Math.min(
                        Math.max(
                                0.0F,
                                finite(requestedFlow)
                        ),
                        flowCapacity
                );

        float cav =
                Mth.clamp(
                        cavitation,
                        0.0F,
                        1.0F
                );

        float back =
                Mth.clamp(
                        backpressure,
                        0.0F,
                        1.0F
                );

        float speedHead =
                speed
                        * 0.13F
                        * (
                        0.58F
                                + health * 0.42F
                );

        float normalizedFlow =
                requested <= 0.001F
                        || flowCapacity <= 0.001F
                        ? 0.0F
                        : Mth.clamp(
                        requested / flowCapacity,
                        0.0F,
                        1.0F
                );

        float restriction =
                Mth.clamp(
                        back * 0.82F
                                + cav * 0.08F,
                        0.0F,
                        1.0F
                );

        /*
         * Open discharge sees only part of the pump's available head.
         * A blocked line pushes the operating point toward full shutoff head,
         * which may exceed the connected pipe rating.
         */
        float pressure =
                Math.min(
                        speedHead,
                        pressureRating
                                * (
                                0.20F
                                        + back * 0.92F
                                )
                                + normalizedFlow * 0.85F
                );

        float flowEfficiency =
                Mth.clamp(
                        1.0F
                                - cav * 0.78F
                                - restriction * 0.46F
                                - (
                                1.0F
                                        - health
                        ) * 0.20F,
                        0.05F,
                        1.0F
                );

        float effectiveFlow =
                requested
                        * flowEfficiency;

        float pressureLoad =
                pressureRating <= 0.001F
                        ? 0.0F
                        : pressure / pressureRating;

        float requiredTorque =
                0.28F
                        + pressure * 0.13F
                        + normalizedFlow * 0.62F
                        + back * 0.52F;

        float requiredPower =
                2.0F
                        + normalizedFlow * 1.10F
                        + pressure * 0.075F
                        + cav * 0.38F
                        + back * 0.32F;

        float vibration =
                Mth.clamp(
                        cav * 0.72F
                                + back * 0.24F
                                + Math.max(
                                0.0F,
                                pressureLoad - 1.0F
                        ) * 0.65F
                                + (
                                1.0F
                                        - health
                        ) * 0.22F,
                        0.0F,
                        1.5F
                );

        float stress =
                Mth.clamp(
                        normalizedFlow * 0.32F
                                + pressureLoad * 0.72F
                                + cav * 0.58F
                                + back * 0.42F
                                + (
                                1.0F
                                        - health
                        ) * 0.34F,
                        0.0F,
                        3.0F
                );

        return new Demand(
                requested,
                effectiveFlow,
                pressure,
                requiredPower,
                requiredTorque,
                cav,
                back,
                vibration,
                stress
        );
    }

    /**
     * Cavitation grows when a fast impeller cannot get enough liquid at the
     * suction side and the local buffer is also low.
     */
    public static float cavitationTarget(
            float rpm,
            float requestedIntake,
            float actualIntake,
            float bufferFill
    ) {
        float speed =
                Mth.clamp(
                        Math.abs(finite(rpm))
                                / 72.0F,
                        0.0F,
                        2.0F
                );

        float request =
                Math.max(
                        0.0F,
                        finite(requestedIntake)
                );

        float intakeRatio =
                request <= 0.001F
                        ? 1.0F
                        : Mth.clamp(
                        finite(actualIntake)
                                / request,
                        0.0F,
                        1.0F
                );

        float dry =
                1.0F
                        - Mth.clamp(
                        bufferFill,
                        0.0F,
                        1.0F
                );

        return Mth.clamp(
                Math.max(
                        0.0F,
                        speed - 0.35F
                )
                        * (
                        1.0F
                                - intakeRatio
                )
                        * (
                        0.35F
                                + dry * 0.65F
                ),
                0.0F,
                1.0F
        );
    }

    /**
     * Backpressure is the remembered mismatch between what the pump tried to
     * send and what the downstream system actually accepted.
     */
    public static float backpressureTarget(
            float attemptedFlow,
            float acceptedFlow,
            float bufferFill
    ) {
        float attempted =
                Math.max(
                        0.0F,
                        finite(attemptedFlow)
                );

        float accepted =
                Math.max(
                        0.0F,
                        finite(acceptedFlow)
                );

        float rejection =
                attempted <= 0.001F
                        ? 0.0F
                        : 1.0F
                                - Mth.clamp(
                                accepted / attempted,
                                0.0F,
                                1.0F
                        );

        return Mth.clamp(
                rejection * 0.82F
                        + Mth.clamp(
                        bufferFill,
                        0.0F,
                        1.0F
                ) * 0.18F,
                0.0F,
                1.0F
        );
    }

    public static float pressureDamage(
            float actualPressure,
            float ratedPressure,
            float integrity
    ) {
        return (float) PressureMath.overloadDamage(
                PressureMath.barToKPa(
                        finite(
                                actualPressure
                        )
                ),
                PressureMath.barToKPa(
                        finite(
                                ratedPressure
                        )
                ),
                integrity
        );
    }

    public static float staticHeadBar(
            double densityKgPerM3,
            double verticalRiseM
    ) {
        return (float) PressureMath.kPaToBar(
                PressureMath.hydrostaticGaugeKPa(
                        densityKgPerM3,
                        Math.max(
                                0.0,
                                verticalRiseM
                        )
                )
        );
    }

    private static float finite(
            float value
    ) {
        return Float.isFinite(value)
                ? value
                : 0.0F;
    }
}
