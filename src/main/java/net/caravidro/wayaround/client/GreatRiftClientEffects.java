package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.GreatRiftField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class GreatRiftClientEffects {

    private GreatRiftClientEffects() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.GREAT_RIFTS
        )) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.isPaused()) {
            return;
        }

        double influence =
                GreatRiftField.influence(
                        minecraft.player
                                .blockPosition()
                                .getX(),
                        minecraft.player
                                .blockPosition()
                                .getZ()
                );

        if (influence
                < 0.25) {
            return;
        }

        var random =
                minecraft.level.random;

        if (random.nextDouble()
                > 0.18
                + influence * 0.34) {
            return;
        }

        double px =
                minecraft.player.getX()
                        + (
                        random.nextDouble()
                                - 0.5
                )
                        * 18.0;

        double py =
                minecraft.player.getY()
                        + 0.3
                        + random.nextDouble()
                        * 4.5;

        double pz =
                minecraft.player.getZ()
                        + (
                        random.nextDouble()
                                - 0.5
                )
                        * 18.0;

        minecraft.level.addParticle(
                ParticleTypes.POOF,
                px,
                py,
                pz,
                (
                        random.nextDouble()
                                - 0.5
                )
                        * 0.045,
                0.012
                        + random.nextDouble()
                        * 0.016,
                (
                        random.nextDouble()
                                - 0.5
                )
                        * 0.045
        );
    }
}
