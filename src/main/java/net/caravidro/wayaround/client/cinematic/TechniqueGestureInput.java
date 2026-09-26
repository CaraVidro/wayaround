package net.caravidro.wayaround.client.cinematic;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.BetaTechniqueClientEffects;
import net.caravidro.wayaround.network.TechniqueGestureC2SPayload;
import net.caravidro.wayaround.network.VoiceIntentC2SPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Empty-hand physical technique input.
 *
 * Client only decides when the button was held/released; the server validates
 * Spectrum ownership, empty hands, cooldown and minimum charge time.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class TechniqueGestureInput {

    private TechniqueGestureInput() {
    }

    private static boolean desmartelarCharging;
    private static int chargeTicks;
    private static boolean purpleUseDown;

    @SubscribeEvent
    public static void suppressVanillaUse(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isUseItem()
                || (!eligible() && !BetaTechniqueClientEffects.hasLocalHeldPurple())) {
            return;
        }

        event.setSwingHand(
                false
        );

        event.setCanceled(
                true
        );
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null) {

            cancelLocal();
            return;
        }

        boolean eligible =
                eligible() && !net.caravidro.wayaround.client.SpectrumMenu.isOpen();

        boolean pressed =
                minecraft.options
                        .keyUse
                        .isDown();

        boolean heldPurple = BetaTechniqueClientEffects.hasLocalHeldPurple();
        if (heldPurple && pressed && !purpleUseDown) {
            PacketDistributor.sendToServer(new VoiceIntentC2SPayload(
                    VoiceIntentC2SPayload.PURPLE_VOID, 1.0F));
        }
        purpleUseDown = pressed;
        if (heldPurple) return;

        if (eligible
                && pressed) {

            if (!desmartelarCharging) {
                desmartelarCharging =
                        true;

                chargeTicks =
                        0;

                PacketDistributor.sendToServer(
                        new TechniqueGestureC2SPayload(
                                TechniqueGestureC2SPayload.DESMARTELAR_START
                        )
                );
            }

            chargeTicks =
                    Math.min(
                            60,
                            chargeTicks + 1
                    );

            return;
        }

        if (!desmartelarCharging) {
            return;
        }

        PacketDistributor.sendToServer(
                new TechniqueGestureC2SPayload(
                        chargeTicks >= 8
                                ? TechniqueGestureC2SPayload.DESMARTELAR_RELEASE
                                : TechniqueGestureC2SPayload.DESMARTELAR_CANCEL
                )
        );

        desmartelarCharging =
                false;

        chargeTicks =
                0;
    }

    private static boolean eligible() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return !net.caravidro.wayaround.client.SpectrumMenu.isOpen() && minecraft.player != null
                && minecraft.screen == null
                && minecraft.player
                        .getMainHandItem()
                        .isEmpty()
                && minecraft.player
                        .getOffhandItem()
                        .isEmpty()
                && SpectrumAccess.has(
                        minecraft.player,
                        SpectrumType.TUKUNA
                );
    }

    private static void cancelLocal() {
        if (!desmartelarCharging) {
            chargeTicks =
                    0;

            return;
        }

        PacketDistributor.sendToServer(
                new TechniqueGestureC2SPayload(
                        TechniqueGestureC2SPayload.DESMARTELAR_CANCEL
                )
        );

        desmartelarCharging =
                false;

        chargeTicks =
                0;
    }
}

