package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ImmortalWheelClientRegistration {

    private ImmortalWheelClientRegistration() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                WayAroundContent.IMMORTAL_WHEEL_REMNANT.get(),
                ImmortalWheelRemnantRenderer::new
        );
    }
}
