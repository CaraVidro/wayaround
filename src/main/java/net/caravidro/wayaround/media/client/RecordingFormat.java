package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class RecordingFormat {

    private RecordingFormat() {
    }

    public static final int MAGIC =
            0x57415652; // WAVR

    public static final int VERSION = 2;

    public static final int WIDTH = 160;
    public static final int HEIGHT = 90;
    public static final int FPS = 10;

    public static final int BYTES_PER_PIXEL = 2;

    public static final int FRAME_BYTES =
            WIDTH
                    * HEIGHT
                    * BYTES_PER_PIXEL;

    public static final int AUDIO_SAMPLE_RATE =
            (int) VoiceConstants.SAMPLE_RATE;

    public static final int AUDIO_BYTES_PER_SAMPLE = 2;

    public static final int HEADER_BYTES =
            Integer.BYTES * 8
                    + Long.BYTES;

    public static final int FRAME_COUNT_OFFSET =
            Integer.BYTES * 5;

    public static final int AUDIO_SAMPLES_OFFSET =
            Integer.BYTES * 7;

    public static final int MAX_SECONDS = 180;

    public static final int MAX_FRAMES =
            FPS * MAX_SECONDS;

    public static final int MAX_AUDIO_SAMPLES =
            AUDIO_SAMPLE_RATE
                    * MAX_SECONDS;
}
