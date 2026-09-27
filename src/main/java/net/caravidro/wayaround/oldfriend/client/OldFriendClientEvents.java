package net.caravidro.wayaround.oldfriend.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.oldfriend.OldFriendContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class OldFriendClientEvents {

    private OldFriendClientEvents() {
    }

    @SubscribeEvent
    public static void renderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                OldFriendContent.HEROBRINE.get(),
                HerobrineRenderer::new
        );
    }
}
