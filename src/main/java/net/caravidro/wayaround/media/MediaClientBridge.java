package net.caravidro.wayaround.media;

import java.util.function.Consumer;

public final class MediaClientBridge {

    private MediaClientBridge() {
    }

    private static Runnable toggleRecording =
            () -> {
            };

    private static Consumer<TelevisionBlockEntity>
            televisionTick =
            television -> {
            };

    public static void install(
            Runnable recordingToggle,
            Consumer<TelevisionBlockEntity> tvTick
    ) {
        toggleRecording =
                recordingToggle == null
                        ? () -> {
                        }
                        : recordingToggle;

        televisionTick =
                tvTick == null
                        ? television -> {
                        }
                        : tvTick;
    }

    public static void toggleRecording() {
        toggleRecording.run();
    }

    public static void televisionTick(
            TelevisionBlockEntity television
    ) {
        televisionTick.accept(
                television
        );
    }
}
