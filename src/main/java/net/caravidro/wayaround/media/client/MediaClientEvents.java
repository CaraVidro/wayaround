package net.caravidro.wayaround.media.client;

import java.util.Locale;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaClientBridge;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class MediaClientEvents {

    private MediaClientEvents() {
    }

    private static boolean bridgeInstalled;
    private static boolean ambientListenerInstalled;

    private static void installBridge() {
        if (bridgeInstalled) {
            return;
        }

        bridgeInstalled = true;

        MediaClientBridge.install(
                MediaRecorder::handleShortPress,
                MediaRecorder::handleLongPress,
                PhotoViewerScreen::open,
                TvVoiceEmitter::tick
        );
    }

    private static void installAmbientListener() {
        if (ambientListenerInstalled) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.getSoundManager()
                .addListener(
                        MediaAmbientRecorder.INSTANCE
                );

        ambientListenerInstalled =
                true;
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        installBridge();
        installAmbientListener();

        MediaRecorder.tick();
        TvVoiceEmitter.advanceTick();
        TvVoiceEmitter.cleanup();
    }

    @SubscribeEvent
    public static void onRenderGuiPre(
            RenderGuiEvent.Pre event
    ) {
        installBridge();

        MediaRecorder
                .captureDueFrame();
    }

    @SubscribeEvent
    public static void onRenderGuiPost(
            RenderGuiEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || !MediaRecorder
                .isCameraHeld()) {

            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int x = 8;
        int y = 8;
        int width = 154;
        int height = 48;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xB5101010
        );

        Component status;

        int statusColor;

        if (MediaRecorder.isRecording()) {
            status =
                    Component.translatable(
                            "hud.wayaround.camera.rec",
                            formatTime(
                                    MediaRecorder
                                            .elapsedMillis()
                            )
                    );

            statusColor =
                    (
                            System.currentTimeMillis()
                                    / 400L
                    ) % 2L == 0L
                            ? 0xFFFF4D4D
                            : 0xFF9A2626;

        } else {
            status =
                    Component.translatable(
                            "hud.wayaround.camera.idle"
                    );

            statusColor =
                    MediaRecorder.isStartPending()
                            ? 0xFFFFD166
                            : 0xFFB9B9B9;
        }

        graphics.drawString(
                minecraft.font,
                status,
                x + 7,
                y + 6,
                statusColor,
                false
        );

        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        MediaRecorder.isRecording()
                                ? "hud.wayaround.camera.stop"
                                : "hud.wayaround.camera.photo"
                ),
                x + 7,
                y + 20,
                0xFFD0D0D0,
                false
        );

        if (!MediaRecorder.isRecording()) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.wayaround.camera.video"
                    ),
                    x + 73,
                    y + 20,
                    0xFFD0D0D0,
                    false
            );
        }

        int paper =
                MediaInventory.count(
                        minecraft.player,
                        MediaContent.PHOTO_PAPER.get()
                );

        int film =
                MediaInventory.count(
                        minecraft.player,
                        MediaContent.FILM_ROLL.get()
                );

        graphics.drawString(
                minecraft.font,
                Component.translatable(
                        "hud.wayaround.camera.media",
                        paper,
                        film
                ),
                x + 7,
                y + 34,
                0xFF838383,
                false
        );
    }

    @SubscribeEvent
    public static void onRenderHand(
            RenderHandEvent event
    ) {
        if (MediaRecorder
                .shouldHideHands()) {

            event.setCanceled(
                    true
            );
        }
    }

    private static String formatTime(
            long millis
    ) {
        long totalSeconds =
                Math.max(
                        0L,
                        millis / 1000L
                );

        return String.format(
                Locale.ROOT,
                "%02d:%02d",
                totalSeconds / 60L,
                totalSeconds % 60L
        );
    }
}
