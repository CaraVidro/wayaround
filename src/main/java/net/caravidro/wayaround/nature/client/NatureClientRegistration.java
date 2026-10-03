package net.caravidro.wayaround.nature.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.nature.NatureContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class NatureClientRegistration {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(NatureContent.HUMMINGBIRD.get(),WoodlandBirdRenderer::new);e.registerEntityRenderer(NatureContent.THRUSH.get(),WoodlandBirdRenderer::new);e.registerEntityRenderer(NatureContent.PARROT.get(),WoodlandBirdRenderer::new);e.registerEntityRenderer(NatureContent.CROW.get(),WoodlandBirdRenderer::new);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{ItemBlockRenderTypes.setRenderLayer(NatureContent.APPLE_SAPLING.get(),RenderType.cutout());ItemBlockRenderTypes.setRenderLayer(NatureContent.APPLE_LEAVES.get(),RenderType.cutoutMipped());});}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block e){e.register((s,l,p,i)->i!=0?0xffffff:l==null||p==null?0x6FA64B:BiomeColors.getAverageFoliageColor(l,p),NatureContent.APPLE_LEAVES.get());}
}
