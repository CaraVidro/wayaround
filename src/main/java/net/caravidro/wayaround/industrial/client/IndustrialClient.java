package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.IndustrialContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class IndustrialClient {
    private IndustrialClient() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(IndustrialContent.BLASTER_MENU.get(), ReforcedBlasterScreen::new);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.WATER_WHEEL_HUB_ENTITY.get(),
                WaterWheelHubRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.MECHANICAL_TRANSMISSION_ENTITY.get(),
                MechanicalTransmissionRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.PULLEY_WHEEL_ENTITY.get(),
                PulleyWheelRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.SAWMILL_ENTITY.get(),
                SawmillRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.MECHANICAL_PRESS_ENTITY.get(),
                MechanicalPressRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.MECHANICAL_FAN_ENTITY.get(),
                MechanicalFanRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.STEAM_ENGINE_ENTITY.get(),
                SteamEngineRenderer::new
        );
        event.registerBlockEntityRenderer(
                net.caravidro.wayaround.industrial.power.PowerContent.WATER_GENERATOR_ENTITY.get(),
                WaterGeneratorRenderer::new
        );
        event.registerBlockEntityRenderer(
                IndustrialContent.BLASTER_ENTITY.get(),
                ReforcedBlasterRenderer::new
        );
    }
}
