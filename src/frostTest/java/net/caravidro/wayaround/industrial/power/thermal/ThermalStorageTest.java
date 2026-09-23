package net.caravidro.wayaround.industrial.power.thermal;

public final class ThermalStorageTest {
    private ThermalStorageTest() {}

    public static void main(String[] args) {
        ThermalStorage storage = new ThermalStorage(
            ThermalUnits.BOILER_THERMAL_MASS_HU_PER_C, ThermalUnits.BOILER_MAX_TEMPERATURE_C);

        require(storage.temperatureC() == ThermalUnits.AMBIENT_TEMPERATURE_C, "ambient temperature");
        require(storage.receive(1_200, false) == 1_200, "accept heat");
        require(Math.abs(storage.temperatureC() - 30.0) < 0.001, "thermal mass conversion");

        int before = storage.storedHu();
        require(storage.receive(500, true) == 500 && storage.storedHu() == before, "simulate must not mutate");

        storage.load(Integer.MAX_VALUE);
        require(storage.temperatureC() <= ThermalUnits.BOILER_MAX_TEMPERATURE_C, "clamp maximum");
        int hot = storage.storedHu();
        require(storage.coolOneTick() > 0 && storage.storedHu() < hot, "passive cooling");

        System.out.println("ThermalStorageTest passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
