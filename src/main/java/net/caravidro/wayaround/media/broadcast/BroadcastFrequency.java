package net.caravidro.wayaround.media.broadcast;

import java.util.Locale;

public final class BroadcastFrequency {
    public static final int MIN_KHZ = 88_000;
    public static final int MAX_KHZ = 108_000;
    public static final int STEP_KHZ = 200;
    public static final int DEFAULT_KHZ = 98_500;

    private BroadcastFrequency() {}

    public static int step(int current, int direction) {
        int next = current + STEP_KHZ * Integer.signum(direction);
        if (next > MAX_KHZ) return MIN_KHZ;
        if (next < MIN_KHZ) return MAX_KHZ;
        return next;
    }

    public static int clamp(int value) {
        return Math.max(MIN_KHZ, Math.min(MAX_KHZ, value));
    }

    public static String display(int khz) {
        return String.format(Locale.ROOT, "%.1f MHz", clamp(khz) / 1000.0);
    }
}
