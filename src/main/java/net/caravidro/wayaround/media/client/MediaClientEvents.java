package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaClientBridge;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class MediaClientEvents {

    private MediaClientEvents() {
    }

    private static boolean bridgeInstalled;

    private static void installBridge() {
        if (bridgeInstalled) {
            return;
        }

        bridgeInstalled = true;

        MediaClientBridge.install(
                MediaRecorder::toggle,
                TvPlaybackScreen::openLatest
        );
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        installBridge();

        if (!MediaRecorder.isRecording()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            MediaRecorder.stop();
            return;
        }

        if (!MediaRecorder.playerStillHasCamera()) {
            MediaRecorder.stopBecauseCameraGone();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(
            RenderGuiEvent.Pre event
    ) {
        installBridge();
        MediaRecorder.captureDueFrame();
    }
}
