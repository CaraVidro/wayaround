package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.EcologyContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EcologyClientModEvents {

    private EcologyClientModEvents() {
    }

    @SubscribeEvent
    public static void renderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                EcologyContent.SUNFISH.get(),
                SunfishRenderer::new
        );
    }
}
