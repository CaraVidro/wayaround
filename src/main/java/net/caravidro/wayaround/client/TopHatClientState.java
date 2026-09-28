package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.network.TopHatStateS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class TopHatClientState {

    private static final Map<UUID, State> STATES =
            new HashMap<>();

    private TopHatClientState() {
    }

    public static void receive(
            TopHatStateS2CPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        double now =
                minecraft.level == null
                        ? 0.0
                        : minecraft.level.getGameTime();

        State previous =
                STATES.get(
                        payload.player()
                );

        double adjustStarted =
                previous == null
                        || payload.adjustUntil()
                        > previous.adjustUntil
                        ? now
                        : previous.adjustStarted;

        STATES.put(
                payload.player(),
                new State(
                        payload.warningUntil(),
                        payload.adjustUntil(),
                        adjustStarted,
                        Mth.clamp(
                                payload.instability(),
                                0.0F,
                                1.0F
                        ),
                        now
                )
        );
    }

    public static boolean warningActive(
            UUID player
    ) {
        State state =
                STATES.get(
                        player
                );

        return state != null
                && now()
                < state.warningUntil;
    }

    public static float instability(
            UUID player
    ) {
        State state =
                STATES.get(
                        player
                );

        if (state == null) {
            return 0.0F;
        }

        return state.instability;
    }

    public static boolean adjusting(
            UUID player
    ) {
        State state =
                STATES.get(
                        player
                );

        return state != null
                && now()
                < state.adjustUntil;
    }

    public static float adjustStrength(
            UUID player,
            float partialTick
    ) {
        State state =
                STATES.get(
                        player
                );

        if (state == null) {
            return 0.0F;
        }

        double now =
                now()
                        + partialTick;

        if (now >= state.adjustUntil
                || state.adjustUntil
                <= state.adjustStarted) {
            return 0.0F;
        }

        float progress =
                Mth.clamp(
                        (float) (
                                (
                                        now
                                                - state.adjustStarted
                                )
                                        / (
                                        state.adjustUntil
                                                - state.adjustStarted
                                )
                        ),
                        0.0F,
                        1.0F
                );

        return Mth.sin(
                progress
                        * Mth.PI
        );
    }

    public static void predictAdjust(
            UUID player
    ) {
        double now =
                now();

        State old =
                STATES.get(
                        player
                );

        float instability =
                old == null
                        ? 0.0F
                        : old.instability;

        STATES.put(
                player,
                new State(
                        0L,
                        (long) now + 24L,
                        now,
                        instability,
                        now
                )
        );
    }

    public static void applyAdjustmentPose(
            AbstractClientPlayer player,
            PlayerModel<?> model,
            float partialTick
    ) {
        float strength =
                adjustStrength(
                        player.getUUID(),
                        partialTick
                );

        if (strength <= 0.001F) {
            return;
        }

        boolean right =
                player.getMainArm()
                        == HumanoidArm.RIGHT;

        var arm =
                right
                        ? model.rightArm
                        : model.leftArm;

        float side =
                right
                        ? -1.0F
                        : 1.0F;

        arm.xRot =
                Mth.lerp(
                        strength,
                        arm.xRot,
                        -2.02F
                );

        arm.yRot =
                Mth.lerp(
                        strength,
                        arm.yRot,
                        side * -0.34F
                );

        arm.zRot =
                Mth.lerp(
                        strength,
                        arm.zRot,
                        side * 0.22F
                );

        /*
         * Do not accumulate head roll here. HumanoidModel reliably rebuilds
         * head pitch/yaw every frame, but head.zRot is not guaranteed to be
         * reset by vanilla. Using += made this tiny tilt stack render after
         * render until the player's neck could end up ~90 degrees sideways.
         *
         * Keep the nod, but make the roll an absolute, bounded pose that also
         * eases back toward neutral as the adjustment animation finishes.
         */
        model.head.xRot +=
                0.08F
                        * strength;

        model.head.zRot =
                Mth.lerp(
                        strength,
                        0.0F,
                        side * 0.035F
                );
    }

    private static double now() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.level == null
                ? 0.0
                : minecraft.level.getGameTime();
    }

    private static final class State {

        private final long warningUntil;
        private final long adjustUntil;
        private final double adjustStarted;
        private final float instability;
        private final double seenAt;

        private State(
                long warningUntil,
                long adjustUntil,
                double adjustStarted,
                float instability,
                double seenAt
        ) {
            this.warningUntil =
                    warningUntil;

            this.adjustUntil =
                    adjustUntil;

            this.adjustStarted =
                    adjustStarted;

            this.instability =
                    instability;

            this.seenAt =
                    seenAt;
        }
    }
}
