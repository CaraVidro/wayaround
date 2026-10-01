package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class TitleScreenUpdateLogEvents {

    private TitleScreenUpdateLogEvents() {
    }

    @SubscribeEvent
    public static void onInit(
            ScreenEvent.Init.Post event
    ) {
        if (!(event.getScreen() instanceof TitleScreen titleScreen)) {
            return;
        }

        int width =
                Minecraft.getInstance()
                        .getWindow()
                        .getGuiScaledWidth();

        event.addListener(
                Button.builder(
                                Component.translatable(
                                        "screen.wayaround.update_log.button"
                                ),
                                button -> Minecraft.getInstance()
                                        .setScreen(
                                                new WayAroundUpdateLogScreen(
                                                        titleScreen
                                                )
                                        )
                        )
                        .bounds(
                                width / 2 - 55,
                                128,
                                110,
                                18
                        )
                        .build()
        );
    }
}
