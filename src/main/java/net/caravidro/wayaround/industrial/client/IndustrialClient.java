package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.IndustrialContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class IndustrialClient {
    private IndustrialClient() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(IndustrialContent.BLASTER_MENU.get(), ReforcedBlasterScreen::new);
    }
}
