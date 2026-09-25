package net.caravidro.wayaround.client.weather;

/**
 * Projected cloud shadows are intentionally disabled.
 *
 * The previous terrain-following translucent quad mesh could intersect the
 * camera projection and create large serrated polygons on screen. Keeping this
 * class as a placeholder preserves a clear home for a future depth-safe shadow
 * implementation without registering a per-frame render listener that only
 * returns immediately.
 */
public final class CloudShadowRenderer {

    private CloudShadowRenderer() {
    }
}
