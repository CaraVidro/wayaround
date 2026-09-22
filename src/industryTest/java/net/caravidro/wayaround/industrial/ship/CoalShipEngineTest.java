package net.caravidro.wayaround.industrial.ship;

public final class CoalShipEngineTest {
    public static void main(String[] args) {
        int fuel = 0;
        int coal = 2;
        int powered = 0;
        for (int tick = 0; tick < 4000; tick++) {
            CoalShipEngine.Step step = CoalShipEngine.tick(fuel, coal > 0, true, true);
            if (step.consumeCoal()) coal--;
            if (step.running()) powered++;
            fuel = step.remainingTicks();
        }
        require(coal == 0 && fuel == 0 && powered == 3200, "Two coal must give exactly 160 seconds, then rowing only");
        CoalShipEngine.Step paused = CoalShipEngine.tick(371, true, false, true);
        require(paused.remainingTicks() == 371 && !paused.running() && !paused.consumeCoal(), "Idle must conserve partially burned coal");
        CoalShipEngine.Step land = CoalShipEngine.tick(0, true, true, false);
        require(!land.running() && !land.consumeCoal(), "Land must not ignite coal");
        CoalShipEngine.Step empty = CoalShipEngine.tick(-1, false, true, true);
        require(empty.remainingTicks() == 0 && !empty.running(), "Invalid/empty saved fuel cannot power the motor");
        // Same recurrence as vanilla's water drag and forward oar acceleration, with motor before drag.
        double speed = 0;
        for (int tick = 0; tick < 200; tick++) {
            speed = Math.min(CoalShipEngine.MAX_HORIZONTAL_SPEED, speed + CoalShipEngine.ACCELERATION) * 0.9 + 0.04;
        }
        require(speed > 0.78 && speed < 0.86, "Cruising speed must stay about twice vanilla, with a stable upper bound");
        System.out.println("Coal ship fuel/idle/land/speed checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
