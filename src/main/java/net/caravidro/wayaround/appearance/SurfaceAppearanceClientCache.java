package net.caravidro.wayaround.appearance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dedicated-safe mirror of server-owned, persistent corrosion. Only the
 * physical client receives packet updates; the class itself uses no client API.
 */
public final class SurfaceAppearanceClientCache {
    private static final int MAX_ENTRIES = 1024;
    private static final LinkedHashMap<Long, Float> CORROSION = new LinkedHashMap<>();

    private SurfaceAppearanceClientCache() {}

    public static synchronized void receive(long[] positions, byte[] amount) {
        if (positions == null || amount == null || positions.length != amount.length) return;
        for (int i = 0; i < positions.length; i++) {
            long key = positions[i];
            float value = (amount[i] & 255) / 255.0F;
            CORROSION.remove(key);
            if (value > 0.005F) CORROSION.put(key, value);
        }
        while (CORROSION.size() > MAX_ENTRIES) {
            CORROSION.remove(CORROSION.keySet().iterator().next());
        }
    }

    public static synchronized Map<Long, Float> snapshot() {
        return Map.copyOf(CORROSION);
    }

    public static synchronized void clear() {
        CORROSION.clear();
    }
}
