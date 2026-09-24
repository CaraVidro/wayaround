package net.caravidro.wayaround.media.client;

public final class MediaVoiceTap {

    private MediaVoiceTap() {
    }

    public static void captureLocal(
            byte[] pcm
    ) {
        MediaRecorder.mixVoiceFrame(
                pcm,
                true
        );
    }

    public static void captureRemote(
            byte[] pcm
    ) {
        MediaRecorder.mixVoiceFrame(
                pcm,
                false
        );
    }
}
