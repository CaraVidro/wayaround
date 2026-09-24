package net.caravidro.wayaround.media.client;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

public final class MediaRecorder {

    private MediaRecorder() {
    }

    private static RecordingWriter writer;
    private static long nextCaptureNanos;

    public static boolean isRecording() {
        return writer != null;
    }

    public static int frameCount() {
        return writer == null ? 0 : writer.frameCount();
    }

    public static void toggle() {
        if (isRecording()) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        if (isRecording()) {
            return;
        }

        try {
            Path path = RecordingStore.createPath();
            writer = new RecordingWriter(path);
            nextCaptureNanos = 0L;

            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_started"
                            )
                            .withStyle(ChatFormatting.RED)
            );
        } catch (Exception exception) {
            writer = null;

            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(ChatFormatting.RED)
            );
        }
    }

    public static void stop() {
        RecordingWriter active = writer;
        writer = null;

        if (active == null) {
            return;
        }

        try {
            Path path = active.finish();

            if (active.frameCount() > 0) {
                clientMessage(
                        Component.translatable(
                                        "message.wayaround.media.recording_stopped",
                                        path.getFileName().toString()
                                )
                                .withStyle(ChatFormatting.GREEN)
                );
            }
        } catch (Exception exception) {
            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(ChatFormatting.RED)
            );
        }
    }

    public static void stopBecauseCameraGone() {
        if (!isRecording()) {
            return;
        }

        clientMessage(
                Component.translatable(
                        "message.wayaround.media.camera_required"
                )
        );

        stop();
    }

    public static void captureDueFrame() {
        RecordingWriter active = writer;

        if (active == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {
            return;
        }

        if (active.frameCount() >= RecordingFormat.MAX_FRAMES) {
            stop();
            return;
        }

        long now = System.nanoTime();

        if (now < nextCaptureNanos) {
            return;
        }

        nextCaptureNanos =
                now
                        + 1_000_000_000L
                        / RecordingFormat.FPS;

        try (
                NativeImage full =
                        Screenshot.takeScreenshot(
                                minecraft.getMainRenderTarget()
                        );
                NativeImage scaled =
                        new NativeImage(
                                RecordingFormat.WIDTH,
                                RecordingFormat.HEIGHT,
                                false
                        )
        ) {
            full.resizeSubRectTo(
                    0,
                    0,
                    full.getWidth(),
                    full.getHeight(),
                    scaled
            );

            active.writeFrame(scaled);
        } catch (Exception exception) {
            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(ChatFormatting.RED)
            );

            stop();
        }
    }

    public static boolean playerStillHasCamera() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return minecraft.player
                .getMainHandItem()
                .is(MediaContent.CAMERA.get())
                || minecraft.player
                .getOffhandItem()
                .is(MediaContent.CAMERA.get());
    }

    private static void clientMessage(Component component) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    component,
                    false
            );
        }
    }
}
