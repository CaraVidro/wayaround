package net.caravidro.wayaround.ecology;

/** Continuous tick clock. Packet corrections converge without rewinding or jumping. */
public final class KrakenAnimationClock {
    private double previous, current, target;
    private long sampledAt, targetAt;
    public void reset(double age, long tick) {
        previous = current = target = age;
        sampledAt = targetAt = tick;
    }
    public void synchronize(double age, long tick) {
        target = age;
        targetAt = tick;
    }
    public void advance(long tick) {
        long elapsed = Math.max(0, tick - sampledAt);
        if (elapsed == 0) return;
        previous = current;
        double error = target + Math.max(0, tick - targetAt) - (current + elapsed);
        current += elapsed + Math.max(-.15 * elapsed, Math.min(.15 * elapsed, error));
        sampledAt = tick;
    }
    public double sample(double partial) {
        return previous + (current - previous) * Math.max(0, Math.min(1, partial));
    }
    public double age() { return current; }
}
