import net.caravidro.wayaround.ecology.KrakenAnimationClock;
import net.caravidro.wayaround.client.VegetationLodMath;

public final class VisualTimingTest {
    public static void main(String[] args) {
        KrakenAnimationClock clock = new KrakenAnimationClock();
        clock.reset(100, 0);
        for (int tick = 1; tick <= 160; tick++) {
            if (tick == 20) clock.synchronize(116, tick); // Delayed packet: four ticks behind.
            if (tick == 70) clock.synchronize(175, tick); // Forward correction.
            double before = clock.age();
            clock.advance(tick);
            double delta = clock.age() - before;
            check(delta >= .849999 && delta <= 1.150001, "bounded monotonic correction");
            double last = clock.sample(0);
            for (int frame = 1; frame <= 12; frame++) {
                double age = clock.sample(frame / 12.0);
                check(age > last, "animation advances each frame");
                last = age;
            }
        }
        check(Math.abs(clock.age() - 265) < .00001, "converges to authoritative time");
        double paused = clock.age();
        clock.advance(160);
        check(clock.age() == paused, "duplicate tick does not advance");
        clock.reset(3, 200);
        check(clock.sample(.5) == 3, "new event resets old interpolation");

        check(VegetationLodMath.tier(30*30, -1) == 0, "near full detail");
        check(VegetationLodMath.tier(65*65, -1) == 1, "medium core canopy");
        check(VegetationLodMath.tier(100*100, -1) == 2, "far plants omitted");
        int tier = 0, changes = 0;
        for (int i=0; i<200; i++) {
            double d = i % 2 == 0 ? 47 : 49;
            int next = VegetationLodMath.tier(d*d, tier);
            if (next != tier) changes++;
            tier = next;
        }
        check(changes == 0, "boundary jitter does not rebuild sections");
        check(VegetationLodMath.tier(57*57, 0) == 1, "outward detail threshold");
        check(VegetationLodMath.tier(39*39, 1) == 0, "return restores details");
        check(VegetationLodMath.tier(75*75, 2) == 2, "far boundary hysteresis");
        check(VegetationLodMath.tier(71*71, 2) == 1, "far detail restored on approach");
        System.out.println("VisualTiming: packet continuity, per-frame motion and LOD hysteresis passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
