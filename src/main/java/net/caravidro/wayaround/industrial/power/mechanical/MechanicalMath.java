package net.caravidro.wayaround.industrial.power.mechanical;

public final class MechanicalMath {
    private MechanicalMath() {}

    public static double equivalentInertia(double inertia, double speedFactor) {
        return Math.max(0.0, inertia) * speedFactor * speedFactor;
    }

    public static double equivalentTorque(double torque, double speedFactor) {
        return torque * speedFactor;
    }

    public static double nextRpm(double rpm, double driveTorque, double resistanceTorque,
            double inertia, boolean jammed) {
        if (jammed) return Math.abs(rpm) < 0.5 ? 0.0 : rpm * 0.72;
        if (inertia <= 0.0001) return 0.0;

        double direction = Math.abs(rpm) > 0.1
            ? Math.signum(rpm)
            : Math.signum(driveTorque);

        double resistance = Math.max(0.0, resistanceTorque);
        double net = driveTorque;

        if (direction != 0.0) {
            if (Math.abs(driveTorque) <= resistance && Math.abs(rpm) <= 0.1) {
                net = 0.0;
            } else {
                net -= direction * resistance;
            }
        }

        double next = rpm + (net / inertia) * MechanicalUnits.TORQUE_TO_RPM_PER_TICK;
        if (Math.signum(rpm) != 0.0 && Math.signum(next) != Math.signum(rpm)
                && Math.abs(driveTorque) <= resistance) {
            next = 0.0;
        }

        return Math.max(-MechanicalUnits.MAX_ROOT_RPM,
            Math.min(MechanicalUnits.MAX_ROOT_RPM, next));
    }
}
