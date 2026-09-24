package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaClientBridge;
import net.minecraft.client.Minecraft;
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
                MediaRecorder::toggle,
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
}
