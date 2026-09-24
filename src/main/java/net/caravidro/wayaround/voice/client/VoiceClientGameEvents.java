package net.caravidro.wayaround.voice.client;

import net.caravidro.wayaround.WayAround;
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

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        boolean canTalk =
                VoiceConfig.isEnabled()
                        && minecraft.player != null
                        && minecraft.getConnection() != null
                        && VoiceClientModEvents
                        .PUSH_TO_TALK
                        .isDown();

        if (canTalk
                && !VoiceCapture.isRunning()) {

            VoiceCapture.start();
        }

        if (!canTalk
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
        int iconX = right - 68;
        int iconY = top + 2;

        graphics.fill(
                iconX - 6,
                top,
                right,
                top + 18,
                0xB0000000
        );

        // Corpo do microfone.
        graphics.fill(
                iconX,
                iconY,
                iconX + 6,
                iconY + 9,
                0xFFFF5555
        );

        // Haste.
        graphics.fill(
                iconX + 2,
                iconY + 9,
                iconX + 4,
                iconY + 13,
                0xFFFF5555
        );

        // Base.
        graphics.fill(
                iconX,
                iconY + 13,
                iconX + 6,
                iconY + 15,
                0xFFFF5555
        );

        graphics.drawString(
                minecraft.font,
                "OUVINDO",
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

        event.addListener(voiceButton);
    }
}
