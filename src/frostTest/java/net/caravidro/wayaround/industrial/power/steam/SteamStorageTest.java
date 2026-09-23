package net.caravidro.wayaround.industrial.power.steam;

public final class SteamStorageTest {
    private SteamStorageTest() {}

    public static void main(String[] args) {
        SteamStorage storage = new SteamStorage(1_000);
        require(storage.pressureBar() == 0.0, "empty vessel pressure");
        require(storage.receive(500, false) == 500, "accept steam");
        require(Math.abs(storage.pressureBar() - 6.0) < 0.0001, "half-full pressure");
        require(storage.receive(800, false) == 500, "clamp to capacity");
        require(Math.abs(storage.pressureBar() - SteamUnits.MAX_PRESSURE_BAR) < 0.0001,
            "full vessel pressure");
        int before = storage.stored();
        require(storage.extract(250, true) == 250 && storage.stored() == before,
            "simulated extraction must not mutate");
        require(storage.extract(250, false) == 250 && storage.stored() == 750,
            "real extraction");
        storage.load(Integer.MAX_VALUE);
        require(storage.stored() == storage.capacity(), "load clamp");
        System.out.println("SteamStorageTest passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
