package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class MediaClient {

    private MediaClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                MediaContent.TELEVISION_ENTITY.get(),
                TelevisionRenderer::new
        );

        event.registerBlockEntityRenderer(
                MediaContent.PLACED_PHOTO_ENTITY.get(),
                PlacedPhotoRenderer::new
        );
    }
}
