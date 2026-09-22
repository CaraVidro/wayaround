package net.caravidro.wayaround.industrial.ship.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.ship.CoalShipContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class CoalShipClient {
    private CoalShipClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CoalShipContent.COAL_SHIP_ENTITY.get(), CoalShipRenderer::new);
        event.registerEntityRenderer(CoalShipContent.CARAVEL_ENTITY.get(), CaravelRenderer::new);
        event.registerEntityRenderer(CoalShipContent.GREAT_SHIP_ENTITY.get(), GreatShipRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CoalShipContent.SHIP_MENU.get(), ShipScreen::new);
    }
}
