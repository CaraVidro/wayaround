package net.caravidro.wayaround.industrial.power;

/** One second of boiler operation. All resource accounting remains on the server. */
public final class SteamCycle {
    public static final int CAPACITY = 32000;
    public static final int WATER_CAPACITY = 8000;
    public static final int COAL_SECONDS = 80;
    public record Step(int water, int fuel, int heat, int generated, boolean consumeCoal) {}
    private SteamCycle() {}
    public static Step tick(int water, int fuel, int heat, boolean coal, int room) {
        boolean demand = water >= 10 && room >= 800;
        boolean ignite = demand && fuel == 0 && coal;
        if (ignite) fuel = COAL_SECONDS;
        if (fuel > 0) {
            fuel--;
            heat = Math.min(100, heat + 5);
        } else {
            heat = Math.max(0, heat - 2);
        }
        int generated = demand && heat == 100 ? 800 : 0;
        if (generated > 0) water -= 10;
        return new Step(water, fuel, heat, generated, ignite);
    }
}
