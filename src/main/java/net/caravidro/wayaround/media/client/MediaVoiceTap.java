package net.caravidro.wayaround.media.client;

public final class MediaVoiceTap {

    private MediaVoiceTap() {
    }

    public static void capture(
            byte[] pcm
    ) {
        MediaRecorder.mixVoiceFrame(
                pcm
        );
    }
}
