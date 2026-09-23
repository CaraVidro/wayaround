package net.caravidro.wayaround.industrial.power.mechanical;

public final class MechanicalMathTest {
    private MechanicalMathTest() {}

    public static void main(String[] args) {
        requireClose(MechanicalMath.equivalentInertia(4.0, 2.0), 16.0, "ratio inertia");
        requireClose(MechanicalMath.equivalentTorque(10.0, 0.5), 5.0, "ratio torque");

        double light = MechanicalMath.nextRpm(0.0, 20.0, 0.0, 4.0, false);
        double heavy = MechanicalMath.nextRpm(0.0, 20.0, 0.0, 16.0, false);
        require(light > heavy, "flywheel inertia must slow acceleration");

        double coast = MechanicalMath.nextRpm(100.0, 0.0, 5.0, 10.0, false);
        require(coast < 100.0 && coast > 0.0, "resistance must slow a coasting shaft");

        double jammed = MechanicalMath.nextRpm(100.0, 100.0, 0.0, 1.0, true);
        require(jammed < 100.0, "jammed gear loop must lose speed");

        System.out.println("MechanicalMathTest passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void requireClose(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 0.0001)
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
}
