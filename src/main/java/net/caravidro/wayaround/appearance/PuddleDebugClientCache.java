package net.caravidro.wayaround.appearance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common-safe mailbox between S2C network handling and the physical client's
 * surface renderer. Updates are drained only on the client render/tick thread.
 */
public final class PuddleDebugClientCache {
    private static final int MAX_UPDATES = 64;
    private static final LinkedHashMap<Long, Integer> UPDATES = new LinkedHashMap<>();

    private PuddleDebugClientCache() {}

    public static synchronized void receive(long position, int durationTicks) {
        UPDATES.put(position, Math.max(0, Math.min(durationTicks, 2400)));
        while (UPDATES.size() > MAX_UPDATES) {
            UPDATES.remove(UPDATES.keySet().iterator().next());
        }
    }

    public static synchronized Map<Long, Integer> drain() {
        Map<Long, Integer> snapshot = Map.copyOf(UPDATES);
        UPDATES.clear();
        return snapshot;
    }

    public static synchronized void clear() {
        UPDATES.clear();
    }
}
