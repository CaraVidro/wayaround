package net.caravidro.wayaround.industrial.power.steam;

public final class SteamUnits {
    public static final double MAX_PRESSURE_BAR = 12.0;
    public static final int BOILER_STEAM_CAPACITY = 4_000;
    public static final int PIPE_STEAM_CAPACITY = 600;
    public static final int VALVE_STEAM_CAPACITY = 300;
    public static final int STEAM_PER_WATER_MB = 20;
    public static final int BOILER_WATER_PER_SECOND_MB = 2;
    public static final int BOILER_HEAT_COST_PER_WATER_MB_HU = 60;
    public static final int TRANSFER_LIMIT_PER_TICK = 80;
    public static final double SAFETY_VALVE_OPEN_BAR = 6.5;
    public static final int SAFETY_VALVE_VENT_PER_TICK = 4;
    public static final double PIPE_WARNING_BAR = 8.0;
    public static final double PIPE_STRESS_BAR = 9.0;
    public static final double PIPE_RUPTURE_BAR = 10.5;
    public static final int PIPE_RUPTURE_TICKS = 80;
    public static final double BOILER_WARNING_BAR = 8.0;
    public static final double BOILER_STRESS_BAR = 9.5;
    public static final double BOILER_RUPTURE_BAR = 11.2;
    public static final int BOILER_RUPTURE_TICKS = 160;
    private SteamUnits() {}
}
