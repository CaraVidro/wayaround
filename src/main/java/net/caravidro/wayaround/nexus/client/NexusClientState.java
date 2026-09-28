package net.caravidro.wayaround.nexus.client;

import net.caravidro.wayaround.network.NexusStateS2CPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class NexusClientState {

    private static boolean active;
    private static boolean complete;
    private static float progress;
    private static int wave;
    private static BlockPos reactor = BlockPos.ZERO;
    private static float visualStrength;

    private NexusClientState() {
    }

    public static void receive(
            NexusStateS2CPayload payload
    ) {
        active = payload.active();
        complete = payload.complete();
        progress = Mth.clamp(
                payload.progress(),
                0.0F,
                1.0F
        );
        wave = payload.wave();
        reactor = payload.reactor();
    }

    public static void tick() {
        /*
         * The Nexus event now owns the actual sky colour through a client
         * mixin. Reach full strength quickly after activation, then fade out
         * slowly when the portal is complete.
         */
        float target =
                active
                        ? 1.0F
                        : 0.0F;

        visualStrength =
                Mth.lerp(
                        active
                                ? 0.055F
                                : 0.018F,
                        visualStrength,
                        target
                );
    }

    public static boolean active() { return active; }
    public static boolean complete() { return complete; }
    public static float progress() { return progress; }
    public static int wave() { return wave; }
    public static BlockPos reactor() { return reactor; }
    public static float strength() { return visualStrength; }

    public static float cloudVisibility() {
        return Mth.clamp(
                1.0F
                        - visualStrength
                        * 1.12F,
                0.0F,
                1.0F
        );
    }
}
