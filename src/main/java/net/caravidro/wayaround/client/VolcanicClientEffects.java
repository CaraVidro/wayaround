package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Cheap local ash ambience. No persistent entities are created.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VolcanicClientEffects {

    private VolcanicClientEffects() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.VOLCANIC_REGIONS
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

        int x =
                minecraft.player
                        .blockPosition()
                        .getX();

        int z =
                minecraft.player
                        .blockPosition()
                        .getZ();

        double influence =
                VolcanicField.influence(
                        x,
                        z
                );

        if (influence
                < 0.18) {
            return;
        }

        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        x,
                        z
                );

        double activity =
                volcano == null
                        ? 0.0
                        : volcano.activity();

        int particles =
                1
                        + (
                        influence > 0.62
                                ? 1
                                : 0
                )
                        + (
                        activity > 0.76
                                ? 1
                                : 0
                );

        var random =
                minecraft.level.random;

        for (int i = 0;
             i < particles;
             i++) {

            double px =
                    minecraft.player.getX()
                            + (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 24.0;

            double py =
                    minecraft.player.getY()
                            + 7.0
                            + random.nextDouble()
                            * 10.0;

            double pz =
                    minecraft.player.getZ()
                            + (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 24.0;

            minecraft.level.addParticle(
                    ParticleTypes.ASH,
                    px,
                    py,
                    pz,
                    (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 0.012,
                    -0.018
                            - random.nextDouble()
                            * 0.012,
                    (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 0.012
            );
        }
    }
}
