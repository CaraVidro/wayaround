package net.caravidro.wayaround.oldfriend.client;

/**
 * Client mirror of the server-controlled photographic anomaly switch.
 *
 * The actual apparition is physical and world-anchored; this class no longer
 * paints anything directly into screenshots.
 */
public final class HerobrinePhotoState {

    private static volatile boolean enabled;

    private HerobrinePhotoState() {
    }

    public static void setEnabled(
            boolean value
    ) {
        enabled =
                value;
    }

    public static boolean enabled() {
        return enabled;
    }
}
