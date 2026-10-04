package net.caravidro.wayaround.littleleaf.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.littleleaf.LittleLeafContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class LittleLeafClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){for(int i=0;i<4;i++)e.registerEntityRenderer(LittleLeafContent.type(i),ColonyInsectRenderer::new);}
}
