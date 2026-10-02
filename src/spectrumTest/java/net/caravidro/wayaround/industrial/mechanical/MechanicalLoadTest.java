package net.caravidro.wayaround.industrial.mechanical;

import net.minecraft.core.Direction;

/**
 * Deterministic invariants for the shared mechanical-consumer language.
 */
public final class MechanicalLoadTest {

    private static int checks;

    private MechanicalLoadTest() {
    }

    public static void main(String[] args) {
        fullPowerRunsAtSourceSpeed();
        powerStarvationReducesDeliveredSpeed();
        torqueStarvationStopsWorkWithoutInventingTorque();
        overspeedRemainsVisibleAsARealOperatingState();
        multipleConsumersShareOneSourceBudget();

        System.out.println(
                "PASS: "
                        + checks
                        + " mechanical load operating-point checks"
        );
    }

    private static void fullPowerRunsAtSourceSpeed() {
        BudgetSource source =
                new BudgetSource(
                        40.0F,
                        2.0F,
                        4.0F
                );

        MechanicalLoad.OperatingPoint point =
                MechanicalLoad.operate(
                        source,
                        2.0F,
                        1.0F,
                        60.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        near(point.grantedPower(), 2.0F, "full power granted");
        near(point.fulfillment(), 1.0F, "full fulfillment");
        near(point.targetRpm(), 40.0F, "full power keeps source rpm");
        check(point.state() == MechanicalLoad.State.RUNNING, "ordinary load runs");
        check(point.canWork(8.0F, 0.7F), "ordinary load can work");
    }

    private static void powerStarvationReducesDeliveredSpeed() {
        BudgetSource source =
                new BudgetSource(
                        45.0F,
                        2.0F,
                        1.2F
                );

        MechanicalLoad.OperatingPoint point =
                MechanicalLoad.operate(
                        source,
                        3.0F,
                        0.5F,
                        80.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        near(point.grantedPower(), 1.2F, "starved source grants only its budget");
        near(point.fulfillment(), 0.4F, "starved fulfillment");
        near(point.targetRpm(), 18.0F, "starvation scales delivered rpm");
        check(point.powerStarved(), "power starvation is explicit");
        check(point.state() == MechanicalLoad.State.POWER_STARVED, "power-starved state");
    }

    private static void torqueStarvationStopsWorkWithoutInventingTorque() {
        BudgetSource source =
                new BudgetSource(
                        90.0F,
                        0.30F,
                        8.0F
                );

        MechanicalLoad.OperatingPoint point =
                MechanicalLoad.operate(
                        source,
                        2.0F,
                        1.50F,
                        120.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        near(point.grantedPower(), 2.0F, "locked load still reacts against source");
        near(point.targetRpm(), 0.0F, "insufficient torque cannot spin the machine");
        check(point.torqueStarved(), "torque starvation is explicit");
        check(point.state() == MechanicalLoad.State.TORQUE_STARVED, "torque-starved state");
        check(!point.canWork(1.0F, 0.2F), "torque-starved machine cannot work");
    }

    private static void overspeedRemainsVisibleAsARealOperatingState() {
        BudgetSource source =
                new BudgetSource(
                        120.0F,
                        3.0F,
                        5.0F
                );

        MechanicalLoad.OperatingPoint point =
                MechanicalLoad.operate(
                        source,
                        1.0F,
                        0.4F,
                        60.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        check(point.state() == MechanicalLoad.State.OVERSPEED, "overspeed classified");
        check(point.demand().overspeed() > 0.9F, "overspeed severity retained");
        near(point.targetRpm(), 120.0F, "overspeed is abuse, not an invisible speed clamp");
    }

    private static void multipleConsumersShareOneSourceBudget() {
        BudgetSource source =
                new BudgetSource(
                        36.0F,
                        4.0F,
                        4.0F
                );

        MechanicalLoad.OperatingPoint first =
                MechanicalLoad.operate(
                        source,
                        3.0F,
                        0.8F,
                        80.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        MechanicalLoad.OperatingPoint second =
                MechanicalLoad.operate(
                        source,
                        3.0F,
                        0.8F,
                        80.0F,
                        0.0F,
                        0.0F,
                        1.0F
                );

        near(first.grantedPower(), 3.0F, "first consumer draws from shared budget");
        near(second.grantedPower(), 1.0F, "second consumer receives remaining budget");
        near(source.power(), 0.0F, "shared budget is exhausted, not duplicated");
        check(second.powerStarved(), "second consumer observes competition");
        near(second.targetRpm(), 12.0F, "remaining third of power gives remaining third of rpm");
    }

    private static void near(
            float actual,
            float expected,
            String description
    ) {
        check(
                Math.abs(actual - expected) <= 0.001F,
                description
                        + " expected="
                        + expected
                        + " actual="
                        + actual
        );
    }

    private static void check(
            boolean condition,
            String description
    ) {
        checks++;

        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static final class BudgetSource
            implements IRotationalPower {

        private final float rpm;
        private final float torque;
        private float budget;

        private BudgetSource(
                float rpm,
                float torque,
                float budget
        ) {
            this.rpm = rpm;
            this.torque = torque;
            this.budget = budget;
        }

        @Override
        public float rpm() {
            return rpm;
        }

        @Override
        public float torque() {
            return torque;
        }

        @Override
        public float power() {
            return budget;
        }

        @Override
        public Direction.Axis axis() {
            return Direction.Axis.X;
        }

        @Override
        public float consumePower(
                float requestedPower
        ) {
            float granted =
                    Math.min(
                            Math.max(
                                    0.0F,
                                    requestedPower
                            ),
                            budget
                    );

            budget -=
                    granted;

            return granted;
        }

        @Override
        public int rotationDirection() {
            return rpm > 0.01F
                    ? 1
                    : rpm < -0.01F
                            ? -1
                            : 0;
        }
    }
}
