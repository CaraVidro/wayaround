package net.caravidro.wayaround.media;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;

public final class MediaClientBridge {

    private MediaClientBridge() {
    }

    private static Runnable cameraShortPress =
            () -> {
            };

    private static Runnable cameraLongPress =
            () -> {
            };

    private static Consumer<ItemStack>
            openPhoto =
            stack -> {
            };

    private static Consumer<TelevisionBlockEntity>
            televisionTick =
            television -> {
            };

    public static void install(
            Runnable shortPress,
            Runnable longPress,
            Consumer<ItemStack> photoOpen,
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

        openPhoto =
                photoOpen == null
                        ? stack -> {
                        }
                        : photoOpen;

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

    public static void openPhoto(
            ItemStack stack
    ) {
        openPhoto.accept(
                stack
        );
    }

    public static void televisionTick(
            TelevisionBlockEntity television
    ) {
        televisionTick.accept(
                television
        );
    }
}
