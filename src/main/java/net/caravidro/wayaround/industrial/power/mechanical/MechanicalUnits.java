package net.caravidro.wayaround.industrial.power.mechanical;

public final class MechanicalUnits {
    public static final int MAX_NETWORK_NODES = 192;
    public static final double TORQUE_TO_RPM_PER_TICK = 0.35;
    public static final double MAX_ROOT_RPM = 600.0;
    public static final double MIN_RUNNING_RPM = 3.0;

    public static final int PISTON_STEAM_CAPACITY = 400;
    public static final double PISTON_MIN_PRESSURE_BAR = 1.2;
    public static final double PISTON_NO_LOAD_RPM = 220.0;

    public static final double SHAFT_SAFE_RPM = 320.0;
    public static final double FLYWHEEL_SAFE_RPM = 230.0;
    public static final double GEARBOX_SAFE_RPM = 280.0;
    public static final double PISTON_SAFE_RPM = 240.0;

    private MechanicalUnits() {}
}
