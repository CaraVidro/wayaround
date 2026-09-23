package net.caravidro.wayaround.industrial.power.thermal;

/**
 * Gameplay-oriented thermal units. HU is not FE and is not defined as a real joule.
 * Temperature comes from stored HU and the component's thermal mass.
 */
public final class ThermalUnits {
    public static final int AMBIENT_TEMPERATURE_C = 20;

    public static final int FIREBOX_HEAT_PER_TICK = 24;
    public static final int COAL_BURN_TICKS = 80 * 20;

    public static final int BOILER_THERMAL_MASS_HU_PER_C = 120;
    public static final int BOILER_MAX_TEMPERATURE_C = 800;
    public static final int BOILER_WATER_CAPACITY_MB = 8_000;
    public static final int BOILING_TEMPERATURE_C = 100;
    public static final int BOILER_EVAPORATION_MB_PER_SECOND = 2;
    public static final int BOILER_EVAPORATION_HEAT_HU_PER_SECOND = 120;

    public static final int PASSIVE_COOLING_DIVISOR = 80;

    private ThermalUnits() {}
}
