package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.particle.AirBubbleParticle;
import net.caravidro.wayaround.particle.BlizzardCloudParticle;
import net.caravidro.wayaround.particle.LivingCloudParticle;
import net.caravidro.wayaround.particle.PrioriteBubbleParticle;
import net.caravidro.wayaround.particle.WindLeafParticle;
import net.caravidro.wayaround.particle.WayAroundParticles;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class WayAroundClientParticles {

    private WayAroundClientParticles() {
    }

    @SubscribeEvent
    public static void registerParticles(
            RegisterParticleProvidersEvent event
    ) {

        event.registerSpriteSet(WayAroundParticles.PRIORITE_BUBBLE.get(), PrioriteBubbleParticle.Provider::new);

        event.registerSpriteSet(
                WayAroundParticles
                        .BLIZZARD_CLOUD
                        .get(),

                BlizzardCloudParticle
                        .Provider::new
        );

        event.registerSpriteSet(
                WayAroundParticles.LIVING_CLOUD.get(),
                LivingCloudParticle.Provider::new
        );

        event.registerSpriteSet(
                WayAroundParticles.WIND_LEAF.get(),
                WindLeafParticle.Provider::new
        );

        event.registerSpriteSet(
                WayAroundParticles.AIR_BUBBLE.get(),
                AirBubbleParticle.Provider::new
        );
    }
}
