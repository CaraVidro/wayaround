package net.caravidro.wayaround.voice.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoiceClientGameEvents {

    private VoiceClientGameEvents() {
    }

    private static boolean lastConnected;
    private static boolean lastEnabled;
    private static VoiceConfig.ActivationMode lastMode;
    private static boolean lastPttDown;

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        VoiceIntentClient.tick();

        boolean enabled =
                VoiceConfig.isEnabled();

        boolean connected =
                enabled
                        && minecraft.player != null
                        && minecraft.getConnection()
                        != null;

        VoiceConfig.ActivationMode mode =
                VoiceConfig.getActivationMode();

        if (connected != lastConnected
                || enabled != lastEnabled
                || mode != lastMode) {

            WayAround.LOGGER.info(
                    "[Voice/State] enabled={} connected={} mode={} debug={} model={}",
                    enabled,
                    connected,
                    mode,
                    VoiceConfig.isDebugSpeechEnabled(),
                    VoskSpeechRecognizer.statusText()
            );

            lastConnected =
                    connected;

            lastEnabled =
                    enabled;

            lastMode =
                    mode;

            if (connected) {
                VoskSpeechRecognizer
                        .warmUpAsync();
            }
        }

        if (!connected) {
            if (VoiceCapture.isRunning()) {
                VoiceCapture.stop();
            }

            return;
        }

        if (VoiceConfig.isVoiceActivation()) {
            if (!VoiceCapture
                    .isVoiceActivationSession()) {

                if (VoiceCapture.isRunning()) {
                    VoiceCapture.stop();

                } else {
                    VoiceCapture.startVoiceActivation();
                }
            }

            return;
        }

        if (VoiceCapture
                .isVoiceActivationSession()) {

            VoiceCapture.stop();
            return;
        }

        boolean keyDown =
                VoiceClientModEvents
                        .PUSH_TO_TALK
                        .isDown();

        if (keyDown != lastPttDown) {
            WayAround.LOGGER.info(
                    "[Voice/PTT] keyDown={}",
                    keyDown
            );

            lastPttDown =
                    keyDown;
        }

        if (keyDown
                && !VoiceCapture.isRunning()) {

            VoiceCapture.startPushToTalk();
        }

        if (!keyDown
                && VoiceCapture.isRunning()) {

            VoiceCapture.stop();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(
            RenderGuiEvent.Post event
    ) {
        if (!VoiceConfig
                .isDebugSpeechEnabled()
                || !VoiceCapture.isRunning()) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        GuiGraphics graphics =
                event.getGuiGraphics();

        int right =
                minecraft.getWindow()
                        .getGuiScaledWidth()
                        - 8;

        int top = 8;
        int iconX = right - 88;
        int iconY = top + 2;

        boolean talking =
                VoiceCapture.isTransmitting();

        graphics.fill(
                iconX - 6,
                top,
                right,
                top + 18,
                0xB0000000
        );

        int color =
                talking
                        ? 0xFFFF5555
                        : 0xFF888888;

        graphics.fill(
                iconX,
                iconY,
                iconX + 6,
                iconY + 9,
                color
        );

        graphics.fill(
                iconX + 2,
                iconY + 9,
                iconX + 4,
                iconY + 13,
                color
        );

        graphics.fill(
                iconX,
                iconY + 13,
                iconX + 6,
                iconY + 15,
                color
        );

        String label =
                VoiceConfig.isVoiceActivation()
                        ? (
                        talking
                                ? "AUTO: FALANDO"
                                : "AUTO: OUVINDO"
                )
                        : "PTT: OUVINDO";

        graphics.drawString(
                minecraft.font,
                label,
                iconX + 11,
                top + 5,
                0xFFFFFFFF,
                false
        );
    }

    @SubscribeEvent
    public static void onScreenInit(
            ScreenEvent.Init.Post event
    ) {
        if (!(event.getScreen()
                instanceof VideoSettingsScreen
                videoScreen)) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        int guiHeight =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        Button voiceButton =
                Button.builder(
                                Component.literal(
                                        "Way Around Voice..."
                                ),
                                button ->
                                        minecraft.setScreen(
                                                new VoiceSettingsScreen(
                                                        videoScreen
                                                )
                                        )
                        )
                        .bounds(
                                6,
                                guiHeight - 26,
                                145,
                                20
                        )
                        .build();

        event.addListener(
                voiceButton
        );
    }
}
