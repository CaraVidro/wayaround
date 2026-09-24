package net.caravidro.wayaround.voice.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
