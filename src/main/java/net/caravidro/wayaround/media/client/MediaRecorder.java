package net.caravidro.wayaround.media.client;

import java.nio.file.Path;
import java.util.Comparator;

import com.mojang.blaze3d.platform.NativeImage;

import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.network.RecordingFinishedC2SPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MediaRecorder {

    private MediaRecorder() {
    }

    private static final long DROPPED_TAIL_NANOS =
            2_500_000_000L;

    private static RecordingWriter writer;

    private static long nextCaptureNanos;
    private static long cameraMissingSinceNanos;

    private static Vec3 detachedCameraPosition;
    private static float detachedCameraYaw;
    private static float detachedCameraPitch;

    private static CameraType previousCameraType;

    public static boolean isRecording() {
        return writer != null;
    }

    public static boolean shouldHideHands() {
        return isRecording();
    }

    public static boolean useArmCameraOffset() {
        return isRecording()
                && detachedCameraPosition == null
                && playerHoldingCamera();
    }

    public static Vec3 detachedCameraPosition() {
        return detachedCameraPosition;
    }

    public static float detachedCameraYaw() {
        return detachedCameraYaw;
    }

    public static float detachedCameraPitch() {
        return detachedCameraPitch;
    }

    public static void toggle() {
        if (isRecording()) {
            finishRecording(
                    false
            );
        } else {
            start();
        }
    }

    public static void start() {
        if (isRecording()) {
            return;
        }

        try {
            RecordingStore.Target target =
                    RecordingStore
                            .createTarget();

            writer =
                    new RecordingWriter(
                            target.id(),
                            target.path()
                    );

            nextCaptureNanos = 0L;
            cameraMissingSinceNanos = 0L;
            detachedCameraPosition = null;

            Minecraft minecraft =
                    Minecraft.getInstance();

            previousCameraType =
                    minecraft.options
                            .getCameraType();

            minecraft.options
                    .setCameraType(
                            CameraType.FIRST_PERSON
                    );

            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_started"
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );

        } catch (Exception exception) {
            writer = null;

            restoreCameraType();

            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );
        }
    }

    public static void tick() {
        if (!isRecording()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            finishRecording(
                    false
            );
            return;
        }

        if (playerHoldingCamera()) {
            cameraMissingSinceNanos = 0L;
            detachedCameraPosition = null;
            return;
        }

        if (playerHasCameraInInventory()) {
            clientMessage(
                    Component.translatable(
                            "message.wayaround.media.camera_stowed"
                    )
            );

            finishRecording(
                    false
            );
            return;
        }

        long now =
                System.nanoTime();

        if (cameraMissingSinceNanos == 0L) {
            cameraMissingSinceNanos =
                    now;

            var camera =
                    minecraft.gameRenderer
                            .getMainCamera();

            detachedCameraYaw =
                    camera.getYRot();

            detachedCameraPitch =
                    camera.getXRot();

            updateDroppedCameraPosition();

            if (detachedCameraPosition == null) {
                detachedCameraPosition =
                        camera.getPosition();
            }

            clientMessage(
                    Component.translatable(
                            "message.wayaround.media.camera_dropped"
                    )
            );
        } else {
            updateDroppedCameraPosition();
        }

        if (now
                - cameraMissingSinceNanos
                >= DROPPED_TAIL_NANOS) {

            finishRecording(
                    true
            );
        }
    }

    public static void captureDueFrame() {
        RecordingWriter active =
                writer;

        if (active == null) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {

            return;
        }

        if (active.frameCount()
                >= RecordingFormat.MAX_FRAMES) {

            finishRecording(
                    false
            );
            return;
        }

        long now =
                System.nanoTime();

        if (now
                < nextCaptureNanos) {

            return;
        }

        nextCaptureNanos =
                now
                        + 1_000_000_000L
                        / RecordingFormat.FPS;

        try (
                NativeImage full =
                        Screenshot.takeScreenshot(
                                minecraft
                                        .getMainRenderTarget()
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

            active.writeFrame(
                    scaled
            );

        } catch (Exception exception) {
            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );

            finishRecording(
                    false
            );
        }
    }

    public static void mixVoiceFrame(
            byte[] pcm,
            boolean localTrack
    ) {
        RecordingWriter active =
                writer;

        if (active != null) {
            active.mixVoiceFrame(
                    pcm,
                    localTrack
            );
        }
    }

    public static void recordAmbientSound(
            String soundId,
            String source,
            float volume,
            float pitch
    ) {
        RecordingWriter active =
                writer;

        if (active != null) {
            active.addAmbientSound(
                    soundId,
                    source,
                    volume,
                    pitch
            );
        }
    }

    public static Vec3 recordingPosition() {
        if (!isRecording()) {
            return null;
        }

        if (detachedCameraPosition != null) {
            return detachedCameraPosition;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.gameRenderer == null) {
            return null;
        }

        return minecraft.gameRenderer
                .getMainCamera()
                .getPosition();
    }

    private static void finishRecording(
            boolean dropTape
    ) {
        RecordingWriter active =
                writer;

        writer = null;

        cameraMissingSinceNanos = 0L;
        detachedCameraPosition = null;

        restoreCameraType();

        if (active == null) {
            return;
        }

        try {
            RecordingWriter.Summary summary =
                    active.finish();

            if (summary.frameCount() <= 0
                    || summary.durationMillis()
                    <= 0L) {

                return;
            }

            PacketDistributor.sendToServer(
                    new RecordingFinishedC2SPayload(
                            summary.recordingId(),
                            summary.durationMillis(),
                            summary.startedAtMillis(),
                            dropTape
                    )
            );

            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_stopped"
                            )
                            .withStyle(
                                    ChatFormatting.GREEN
                            )
            );

        } catch (Exception exception) {
            clientMessage(
                    Component.translatable(
                                    "message.wayaround.media.recording_failed",
                                    exception.getMessage()
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );
        }
    }

    private static boolean playerHoldingCamera() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return minecraft.player
                .getMainHandItem()
                .is(
                        MediaContent.CAMERA.get()
                )
                || minecraft.player
                .getOffhandItem()
                .is(
                        MediaContent.CAMERA.get()
                );
    }

    private static boolean playerHasCameraInInventory() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        var inventory =
                minecraft.player
                        .getInventory();

        for (int slot = 0;
             slot < inventory
                     .getContainerSize();
             slot++) {

            if (inventory.getItem(slot)
                    .is(
                            MediaContent.CAMERA.get()
                    )) {

                return true;
            }
        }

        return false;
    }

    private static void updateDroppedCameraPosition() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            return;
        }

        ItemEntity nearest =
                minecraft.level
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                minecraft.player
                                        .getBoundingBox()
                                        .inflate(6.0),
                                entity ->
                                        entity.getItem()
                                                .is(
                                                        MediaContent.CAMERA.get()
                                                )
                        )
                        .stream()
                        .min(
                                Comparator.comparingDouble(
                                        entity ->
                                                entity.distanceToSqr(
                                                        minecraft.player
                                                )
                                )
                        )
                        .orElse(null);

        if (nearest != null) {
            detachedCameraPosition =
                    nearest.position()
                            .add(
                                    0.0,
                                    0.16,
                                    0.0
                            );
        }
    }

    private static void restoreCameraType() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (previousCameraType != null) {
            minecraft.options
                    .setCameraType(
                            previousCameraType
                    );

            previousCameraType = null;
        }
    }

    private static void clientMessage(
            Component component
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player
                    .displayClientMessage(
                            component,
                            false
                    );
        }
    }
}
