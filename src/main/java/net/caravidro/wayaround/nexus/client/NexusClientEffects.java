package net.caravidro.wayaround.nexus.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class NexusClientEffects {

    private NexusClientEffects() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        NexusClientState.tick();

        Minecraft minecraft =
                Minecraft.getInstance();

        if (!NexusClientState.active()
                || minecraft.level == null
                || minecraft.player == null
                || minecraft.isPaused()) {
            return;
        }

        if (NexusClientState.progress() >= 0.4F
                && minecraft.player.tickCount % 2 == 0) {
            int count =
                    3
                            + Math.round(
                            NexusClientState.progress()
                                    * 5.0F
                    );

            for (int i = 0;
                 i < count;
                 i++) {
                double x =
                        minecraft.player.getX()
                                + (
                                minecraft.level.random.nextDouble()
                                        - 0.5
                        ) * 34.0;

                double y =
                        minecraft.player.getY()
                                + 15.0
                                + minecraft.level.random.nextDouble()
                                * 12.0;

                double z =
                        minecraft.player.getZ()
                                + (
                                minecraft.level.random.nextDouble()
                                        - 0.5
                        ) * 34.0;

                minecraft.level.addParticle(
                        DustParticleOptions.REDSTONE,
                        x,
                        y,
                        z,
                        0.0,
                        -0.44
                                - minecraft.level.random.nextDouble()
                                * 0.35,
                        0.0
                );
            }
        }
    }

    @SubscribeEvent
    public static void fogColor(
            ViewportEvent.ComputeFogColor event
    ) {
        float strength =
                NexusClientState.strength();

        if (strength <= 0.001F) {
            return;
        }

        event.setRed(
                Mth.lerp(
                        strength,
                        event.getRed(),
                        0.38F
                                + NexusClientState.progress()
                                * 0.32F
                )
        );

        event.setGreen(
                event.getGreen()
                        * (
                        1.0F
                                - strength
                                * 0.82F
                )
        );

        event.setBlue(
                event.getBlue()
                        * (
                        1.0F
                                - strength
                                * 0.88F
                )
        );
    }

    @SubscribeEvent
    public static void camera(
            ViewportEvent.ComputeCameraAngles event
    ) {
        float strength =
                NexusClientState.strength();

        if (strength < 0.35F) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        double time =
                minecraft.player.tickCount
                        + event.getPartialTick();

        float amplitude =
                (
                        0.025F
                                + NexusClientState.progress()
                                * 0.075F
                )
                        * strength;

        event.setRoll(
                event.getRoll()
                        + (float) Math.sin(
                        time * 1.83
                ) * amplitude
        );

        event.setPitch(
                event.getPitch()
                        + (float) Math.sin(
                        time * 2.37 + 0.8
                ) * amplitude
        );
    }

    @SubscribeEvent
    public static void overlay(
            RenderGuiEvent.Post event
    ) {
        float strength =
                NexusClientState.strength();

        if (strength <= 0.01F) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        int alpha =
                Mth.clamp(
                        Math.round(
                                strength * 24.0F
                        ),
                        0,
                        28
                );

        event.getGuiGraphics()
                .fill(
                        0,
                        0,
                        minecraft.getWindow()
                                .getGuiScaledWidth(),
                        minecraft.getWindow()
                                .getGuiScaledHeight(),
                        alpha << 24
                                | 0x00A00000
                );
    }
}
