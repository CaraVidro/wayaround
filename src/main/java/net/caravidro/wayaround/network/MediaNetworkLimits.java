package net.caravidro.wayaround.network;

/**
 * Hard network limits shared by codecs and server transfer logic.
 *
 * Keep these intentionally conservative: Minecraft world/chunk traffic gets
 * priority over WayAround media on slow/VPN links.
 */
public final class MediaNetworkLimits {

    public static final long MAX_RECORDING_BYTES =
            96L * 1024L * 1024L;

    public static final int MAX_UPLOAD_CHUNK =
            24 * 1024;

    public static final int DOWNLOAD_CHUNK =
            32 * 1024;

    public static final int DOWNLOAD_CHUNKS_PER_TICK =
            1;

    public static final int BROADCAST_WIDTH =
            64;

    public static final int BROADCAST_HEIGHT =
            36;

    public static final int BROADCAST_RGB_BYTES =
            BROADCAST_WIDTH
                    * BROADCAST_HEIGHT
                    * 3;

    private MediaNetworkLimits() {
    }
}
