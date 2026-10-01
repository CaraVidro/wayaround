package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
import net.caravidro.wayaround.network.DeepSeaSubmarineControlC2SPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Submarine controls:
 * W/S drive, A/D steer, Space rises, Sprint descends, Shift dismounts.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class DeepSeaSubmarineClientControls {

    private static byte lastThrottle;
    private static byte lastSteering;
    private static byte lastVertical;
    private static int resend;

    private DeepSeaSubmarineClientControls() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || !(minecraft.player.getVehicle()
                instanceof DeepSeaSubmarineEntity)
                || minecraft.screen != null) {

            sendIfChanged(
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    true
            );

            return;
        }

        byte throttle =
                0;

        byte steering =
                0;

        byte vertical =
                0;

        if (minecraft.options.keyUp.isDown()) {
            throttle++;
        }

        if (minecraft.options.keyDown.isDown()) {
            throttle--;
        }

        if (minecraft.options.keyLeft.isDown()) {
            steering--;
        }

        if (minecraft.options.keyRight.isDown()) {
            steering++;
        }

        if (minecraft.options.keyJump.isDown()) {
            vertical++;
        }

        /*
         * Sneak is Minecraft's vehicle dismount key, so descend uses Sprint
         * instead of fighting vanilla passenger logic.
         */
        if (minecraft.options.keySprint.isDown()) {
            vertical--;
        }

        sendIfChanged(
                throttle,
                steering,
                vertical,
                false
        );
    }

    private static void sendIfChanged(
            byte throttle,
            byte steering,
            byte vertical,
            boolean forceZero
    ) {
        boolean changed =
                throttle != lastThrottle
                        || steering != lastSteering
                        || vertical != lastVertical;

        if (!changed
                && !forceZero
                && resend-- > 0) {
            return;
        }

        if (!changed
                && forceZero
                && lastThrottle == 0
                && lastSteering == 0
                && lastVertical == 0) {
            return;
        }

        lastThrottle =
                throttle;

        lastSteering =
                steering;

        lastVertical =
                vertical;

        resend =
                3;

        PacketDistributor.sendToServer(
                new DeepSeaSubmarineControlC2SPayload(
                        throttle,
                        steering,
                        vertical
                )
        );
    }
}
