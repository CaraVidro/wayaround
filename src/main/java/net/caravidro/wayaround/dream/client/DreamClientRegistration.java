package net.caravidro.wayaround.dream.client;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.dream.DreamContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class DreamClientRegistration {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(DreamContent.PLAYER.get(),DreamPlayerRenderer::new);
    }
}
