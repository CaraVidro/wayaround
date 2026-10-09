package net.caravidro.wayaround.client.worldconfig;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class WorldFeatureClientEvents {

    private WorldFeatureClientEvents() {}

    @SubscribeEvent
    public static void logout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        net.caravidro.wayaround.client.water.WaterSurfaceRenderer.clearCache();
        net.caravidro.wayaround.client.water.WaterEffectsClient.clearCache();
        WorldFeatureRuntime.resetClient();
        net.caravidro.wayaround.worldgen.weather.local.CloudRainOverrides.clear();
        net.caravidro.wayaround.appearance.SurfaceAppearanceClientCache.clear();
        net.caravidro.wayaround.appearance.PuddleDebugClientCache.clear();
        WorldFeatureCreationFlow.clearApproval();
    }
}
