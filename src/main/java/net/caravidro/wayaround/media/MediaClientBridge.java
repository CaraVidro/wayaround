package net.caravidro.wayaround.media;

public final class MediaClientBridge {

    private MediaClientBridge() {
    }

    private static Runnable toggleRecording = () -> {};
    private static Runnable openTelevision = () -> {};

    public static void install(
            Runnable recordingToggle,
            Runnable televisionOpen
    ) {
        toggleRecording = recordingToggle == null ? () -> {} : recordingToggle;
        openTelevision = televisionOpen == null ? () -> {} : televisionOpen;
    }

    public static void toggleRecording() {
        toggleRecording.run();
    }

    public static void openTelevision() {
        openTelevision.run();
    }
}
