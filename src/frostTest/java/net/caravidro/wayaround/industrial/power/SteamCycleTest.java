package net.caravidro.wayaround.industrial.power;

public final class SteamCycleTest {
    public static void main(String[] args) {
        var dry = SteamCycle.tick(0, 0, 0, true, 32000);
        require(!dry.consumeCoal() && dry.generated() == 0, "Dry boiler must not ignite");
        var full = SteamCycle.tick(1000, 0, 100, true, 0);
        require(!full.consumeCoal() && full.water() == 1000, "Full buffer must not consume resources");
        int water = 1000;
        int fuel = 0;
        int heat = 0;
        int generated = 0;
        int coal = 1;
        for (int second = 0; second < 100; second++) {
            var step = SteamCycle.tick(water, fuel, heat, coal > 0, 32000);
            if (step.consumeCoal()) coal--;
            if (second < 19) require(step.generated() == 0, "Must heat before generating");
            water = step.water();
            fuel = step.fuel();
            heat = step.heat();
            generated += step.generated();
        }
        require(coal == 0 && fuel == 0, "Exactly one coal must be burned");
        require(generated > 0 && generated == (1000 - water) * 80, "Water and energy must balance");
        require(heat < 100, "Unfueled boiler must cool down");
        var insufficient = SteamCycle.tick(9, 0, 100, true, 32000);
        require(insufficient.generated() == 0 && !insufficient.consumeCoal(), "No generation with insufficient water");
        EnergyBudget buffer = new EnergyBudget(32000);
        buffer.add(800);
        require(buffer.transferTo(1280, offered -> 120) == 120 && buffer.stored() == 680,
                "Only accepted energy may leave the buffer");
        require(buffer.extract(100, true) == 100 && buffer.stored() == 680, "Simulation must preserve energy");
        System.out.println("Steam cycle passed: warmup, water/fuel accounting, full buffer, cooldown and energy transfer.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
