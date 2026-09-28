package net.caravidro.wayaround.nexus.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.nexus.NexusContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class NexusClientModEvents {

    private NexusClientModEvents() {
    }

    @SubscribeEvent
    public static void renderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                NexusContent.NEXUS_SLUDGE.get(),
                NexusSludgeRenderer::new
        );

        event.registerBlockEntityRenderer(
                NexusContent.NEXUSTOR_BASE_ENTITY.get(),
                NexustorRenderer::new
        );
    }
}
