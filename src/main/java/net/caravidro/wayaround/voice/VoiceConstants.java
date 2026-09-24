package net.caravidro.wayaround.voice;

import javax.sound.sampled.AudioFormat;

public final class VoiceConstants {

    private VoiceConstants() {
    }

    public static final float SAMPLE_RATE = 48_000.0f;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = false;

    public static final int FRAME_MILLIS = 40;
    public static final int BYTES_PER_SAMPLE = SAMPLE_SIZE_BITS / 8;

    public static final int FRAME_BYTES =
            (int) (SAMPLE_RATE
                    * (FRAME_MILLIS / 1000.0f)
                    * CHANNELS
                    * BYTES_PER_SAMPLE);

    public static final int MAX_PACKET_BYTES = 4096;
    public static final double HEARING_RANGE_BLOCKS = 48.0;

    public static AudioFormat audioFormat() {
        return new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
        );
    }
}
