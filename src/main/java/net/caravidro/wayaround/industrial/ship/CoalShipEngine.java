package net.caravidro.wayaround.industrial.ship;

/** Fuel accounting is independent of client movement and stops when the pilot lets go of forward. */
public final class CoalShipEngine {
    public static final int TICKS_PER_COAL = 1600;
    public static final double ACCELERATION = 0.045;
    public static final double MAX_HORIZONTAL_SPEED = 0.85;

    private CoalShipEngine() {}

    public static Step tick(int remaining, boolean hasCoal, boolean forward, boolean onWater) {
        int fuel = Math.clamp(remaining, 0, TICKS_PER_COAL);
        if (!forward || !onWater) {
            return new Step(fuel, false, false);
        }
        boolean ignite = fuel == 0 && hasCoal;
        if (ignite) {
            fuel = TICKS_PER_COAL;
        }
        return fuel > 0 ? new Step(fuel - 1, ignite, true) : new Step(0, false, false);
    }

    public record Step(int remainingTicks, boolean consumeCoal, boolean running) {}
}
