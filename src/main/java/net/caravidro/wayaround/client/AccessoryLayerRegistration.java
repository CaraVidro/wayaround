package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Installs the accessory layer on both default and slim player renderers. */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class AccessoryLayerRegistration {

    private AccessoryLayerRegistration() {
    }

    @SubscribeEvent
    public static void addPlayerLayers(
            EntityRenderersEvent.AddLayers event
    ) {
        for (PlayerSkin.Model skin :
                event.getSkins()) {

            PlayerRenderer renderer =
                    event.getSkin(
                            skin
                    );

            if (renderer != null) {
                renderer.addLayer(
                        new AccessoryPlayerLayer(
                                renderer
                        )
                );
            }
        }
    }
}
