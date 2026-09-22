package net.caravidro.wayaround.client.calving;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.CalvingNetwork;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;


@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = WayAround.MODID
)
public final class ClientCalvingEffects {

    private static double centerX;
    private static double centerY;
    private static double centerZ;


    private static float intensity;


    private static int remainingTicks;

    private static int totalTicks;


    private ClientCalvingEffects() {
    }


    public static void receive(
            CalvingNetwork.CalvingShakePayload payload
    ) {

        /*
         * Um impacto novo mais forte
         * domina o shake anterior.
         */

        if (
                remainingTicks <= 0

                        ||

                payload.intensity()
                        >=
                        intensity
        ) {

            centerX =
                    payload.x();

            centerY =
                    payload.y();

            centerZ =
                    payload.z();


            intensity =
                    payload.intensity();


            totalTicks =
                    Math.max(
                            1,
                            payload.ticks()
                    );


            remainingTicks =
                    totalTicks;

        }

        else {

            remainingTicks =
                    Math.max(

                            remainingTicks,

                            payload.ticks()

                    );
        }
    }


    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {

        if (
                remainingTicks > 0
        ) {

            remainingTicks--;
        }
    }


    @SubscribeEvent
    public static void camera(
            ViewportEvent.ComputeCameraAngles event
    ) {

        if (
                remainingTicks <= 0
        ) {

            return;
        }


        Minecraft minecraft =
                Minecraft.getInstance();


        if (
                minecraft.player == null
        ) {

            return;
        }


        double dx =
                minecraft.player.getX()
                        -
                        centerX;


        double dy =
                minecraft.player.getY()
                        -
                        centerY;


        double dz =
                minecraft.player.getZ()
                        -
                        centerZ;


        double distance =
                Math.sqrt(

                        dx * dx

                                +

                        dy * dy

                                +

                        dz * dz

                );


        /*
         * 0 blocos:
         * 100%
         *
         * 96 blocos:
         * 0%
         */

        double distanceFactor =
                clamp01(

                        1.0

                                -

                        distance
                                /
                                96.0

                );


        if (
                distanceFactor <= 0.0
        ) {

            return;
        }


        double life =
                remainingTicks

                        /

                (double)
                        Math.max(
                                1,
                                totalTicks
                        );


        /*
         * Tremor ainda começa forte,
         * mas vai morrendo.
         */

        double envelope =
                0.20
                        +
                        life * 0.80;


        double time =
                minecraft.player.tickCount

                        +

                        event.getPartialTick();


        double amplitude =
                intensity

                        *

                        distanceFactor

                        *

                        envelope;


        float yaw =
                (float) (

                        Math.sin(
                                time * 2.71
                        )

                                *

                        amplitude
                                *
                                1.15

                );


        float pitch =
                (float) (

                        Math.sin(

                                time * 3.97
                                        +
                                        1.3

                        )

                                *

                        amplitude
                                *
                                0.80

                );


        float roll =
                (float) (

                        Math.sin(

                                time * 2.13
                                        +
                                        2.8

                        )

                                *

                        amplitude
                                *
                                0.55

                );


        event.setYaw(

                event.getYaw()
                        +
                        yaw

        );


        event.setPitch(

                event.getPitch()
                        +
                        pitch

        );


        event.setRoll(

                event.getRoll()
                        +
                        roll

        );
    }


    private static double clamp01(
            double value
    ) {

        return Math.max(

                0.0,

                Math.min(
                        1.0,
                        value
                )

        );
    }
}