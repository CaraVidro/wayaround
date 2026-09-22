package net.caravidro.wayaround.industrial.power.thermal;

/** Deterministic heat storage above ambient. Steam is intentionally not modeled here. */
public final class ThermalStorage {
    private final int thermalMassHuPerC;
    private final int maxTemperatureC;
    private final int capacityHu;
    private int storedHu;

    public ThermalStorage(int thermalMassHuPerC, int maxTemperatureC) {
        if (thermalMassHuPerC <= 0) throw new IllegalArgumentException("Thermal mass must be positive");
        if (maxTemperatureC <= ThermalUnits.AMBIENT_TEMPERATURE_C)
            throw new IllegalArgumentException("Maximum temperature must be above ambient");
        this.thermalMassHuPerC = thermalMassHuPerC;
        this.maxTemperatureC = maxTemperatureC;
        this.capacityHu = Math.multiplyExact(
            maxTemperatureC - ThermalUnits.AMBIENT_TEMPERATURE_C, thermalMassHuPerC);
    }

    public int storedHu() { return storedHu; }
    public int capacityHu() { return capacityHu; }

    public double temperatureC() {
        return ThermalUnits.AMBIENT_TEMPERATURE_C + storedHu / (double) thermalMassHuPerC;
    }

    public int receive(int offeredHu, boolean simulate) {
        int accepted = Math.min(capacityHu - storedHu, Math.max(0, offeredHu));
        if (!simulate) storedHu += accepted;
        return accepted;
    }

    public int coolOneTick() {
        if (storedHu <= 0) return 0;
        double aboveAmbient = temperatureC() - ThermalUnits.AMBIENT_TEMPERATURE_C;
        int loss = Math.max(1, (int) Math.floor(aboveAmbient / ThermalUnits.PASSIVE_COOLING_DIVISOR));
        loss = Math.min(loss, storedHu);
        storedHu -= loss;
        return loss;
    }

    public void load(int savedHu) {
        storedHu = Math.max(0, Math.min(capacityHu, savedHu));
    }

    public int maxTemperatureC() { return maxTemperatureC; }
}
