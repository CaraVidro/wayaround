package net.caravidro.wayaround.industrial.mechanical;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;

/**
 * Shared language for mechanical consumers.
 *
 * This class does not know what a sawmill, pump or crusher is. It only knows
 * demand, available power, torque and speed. Machines remain free to decide
 * what those physical limits mean for their own operation.
 */
public final class MechanicalLoad {

    public enum State {
        IDLE,
        RUNNING,
        POWER_STARVED,
        TORQUE_STARVED,
        OVERSPEED,
        OVERHEATED,
        CRITICAL
    }

    public record Demand(
            float requestedPower,
            float availablePower,
            float requiredTorque,
            float availableTorque,
            float rpm,
            float safeRpm
    ) {
        public float powerRatio() {
            if (requestedPower <= 0.001F) return 1.0F;
            return Mth.clamp(availablePower / requestedPower, 0.0F, 4.0F);
        }

        public float torqueRatio() {
            if (requiredTorque <= 0.001F) return 1.0F;
            return Mth.clamp(availableTorque / requiredTorque, 0.0F, 4.0F);
        }

        public boolean powerStarved() {
            return requestedPower > 0.001F
                    && availablePower + 0.001F < requestedPower;
        }

        public boolean torqueStarved() {
            return requiredTorque > 0.001F
                    && availableTorque + 0.001F < requiredTorque;
        }

        public float overspeed() {
            return MechanicalLoad.overspeed(rpm, safeRpm);
        }
    }

    private MechanicalLoad() {
    }

    public static Demand sample(
            @Nullable IRotationalPower source,
            float requestedPower,
            float requiredTorque,
            float safeRpm
    ) {
        float request = finitePositive(requestedPower);
        float torqueRequest = finitePositive(requiredTorque);
        float safe = Math.max(0.01F, finitePositive(safeRpm));

        if (source == null) {
            return new Demand(
                    request,
                    0.0F,
                    torqueRequest,
                    0.0F,
                    0.0F,
                    safe
            );
        }

        return new Demand(
                request,
                finitePositive(source.power()),
                torqueRequest,
                finitePositive(Math.abs(source.torque())),
                Float.isFinite(source.rpm()) ? source.rpm() : 0.0F,
                safe
        );
    }

    public static float fulfillment(
            float requestedPower,
            float grantedPower
    ) {
        float request = finitePositive(requestedPower);

        if (request <= 0.001F) {
            return 0.0F;
        }

        return Mth.clamp(
                finitePositive(grantedPower) / request,
                0.0F,
                1.0F
        );
    }

    public static float normalized(
            float value,
            float ratedValue
    ) {
        float rated = Math.max(0.001F, finitePositive(ratedValue));

        return Mth.clamp(
                finitePositive(Math.abs(value)) / rated,
                0.0F,
                2.0F
        );
    }

    public static float overspeed(
            float rpm,
            float safeRpm
    ) {
        float safe = Math.max(0.01F, finitePositive(safeRpm));

        return Math.max(
                0.0F,
                (Math.abs(Float.isFinite(rpm) ? rpm : 0.0F) - safe)
                        / safe
        );
    }

    /**
     * Dimensionless severity used for wear/failure feedback.
     *
     * 0..~0.8 is ordinary service. Around 1 the machine is asking for
     * attention. Values above 1 represent combinations of load, speed, heat,
     * vibration and poor condition that should accelerate failure.
     */
    public static float failureStress(
            float loadRatio,
            float overspeed,
            float vibration,
            float heat,
            float condition
    ) {
        float load = Mth.clamp(loadRatio, 0.0F, 2.0F);
        float speed = Mth.clamp(overspeed, 0.0F, 2.0F);
        float shake = Mth.clamp(vibration, 0.0F, 1.5F);
        float thermal = Mth.clamp(heat, 0.0F, 2.0F);
        float damage = 1.0F - Mth.clamp(condition, 0.0F, 1.0F);

        return Mth.clamp(
                load * 0.46F
                        + speed * 0.42F
                        + shake * 0.38F
                        + thermal * 0.34F
                        + damage * 0.52F,
                0.0F,
                3.0F
        );
    }

    public static State classify(
            Demand demand,
            float grantedPower,
            float heat,
            float vibration,
            float condition
    ) {
        if (demand.requestedPower() <= 0.001F
                && Math.abs(demand.rpm()) <= 0.01F) {
            return State.IDLE;
        }

        float stress = failureStress(
                normalized(
                        demand.requestedPower(),
                        Math.max(0.001F, demand.availablePower())
                ),
                demand.overspeed(),
                vibration,
                heat,
                condition
        );

        if (condition < 0.08F || stress > 1.55F) {
            return State.CRITICAL;
        }

        if (heat > 1.0F) {
            return State.OVERHEATED;
        }

        if (demand.overspeed() > 0.12F) {
            return State.OVERSPEED;
        }

        if (demand.torqueStarved()) {
            return State.TORQUE_STARVED;
        }

        if (demand.powerStarved()
                || fulfillment(
                        demand.requestedPower(),
                        grantedPower
                ) < 0.72F) {
            return State.POWER_STARVED;
        }

        return State.RUNNING;
    }

    private static float finitePositive(float value) {
        return Float.isFinite(value)
                ? Math.max(0.0F, value)
                : 0.0F;
    }
}
