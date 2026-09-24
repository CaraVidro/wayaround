package net.caravidro.wayaround.media;

import java.util.function.Consumer;

public final class MediaClientBridge {

    private MediaClientBridge() {
    }

    private static Runnable cameraShortPress =
            () -> {
            };

    private static Runnable cameraLongPress =
            () -> {
            };

    private static Consumer<TelevisionBlockEntity>
            televisionTick =
            television -> {
            };

    public static void install(
            Runnable shortPress,
            Runnable longPress,
            Consumer<TelevisionBlockEntity> tvTick
    ) {
        cameraShortPress =
                shortPress == null
                        ? () -> {
                        }
                        : shortPress;

        cameraLongPress =
                longPress == null
                        ? () -> {
                        }
                        : longPress;

        televisionTick =
                tvTick == null
                        ? television -> {
                        }
                        : tvTick;
    }

    public static void cameraShortPress() {
        cameraShortPress.run();
    }

    public static void cameraLongPress() {
        cameraLongPress.run();
    }

    public static void televisionTick(
            TelevisionBlockEntity television
    ) {
        televisionTick.accept(
                television
        );
    }
}
