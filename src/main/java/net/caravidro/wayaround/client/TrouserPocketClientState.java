package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.network.TrouserPocketAnimationS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Small procedural animation for reaching to the engineer trouser pocket.
 */
public final class TrouserPocketClientState {

    private static final Map<UUID, State> STATES =
            new HashMap<>();

    private TrouserPocketClientState() {
    }

    public static void receive(
            TrouserPocketAnimationS2CPayload payload
    ) {
        STATES.put(
                payload.player(),
                new State(
                        now(),
                        Math.max(
                                6,
                                payload.durationTicks()
                        )
                )
        );
    }

    public static void predict(
            UUID player
    ) {
        STATES.put(
                player,
                new State(
                        now(),
                        14
                )
        );
    }

    public static void applyRetrievePose(
            AbstractClientPlayer player,
            PlayerModel<?> model,
            float partialTick
    ) {
        State state =
                STATES.get(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        double age =
                now()
                        + partialTick
                        - state.startedAt;

        if (age < 0.0
                || age >= state.duration) {
            STATES.remove(
                    player.getUUID()
            );
            return;
        }

        float progress =
                Mth.clamp(
                        (float) (
                                age
                                        / state.duration
                        ),
                        0.0F,
                        1.0F
                );

        float reach =
                Mth.sin(
                        progress
                                * Mth.PI
                );

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

        /*
         * Hand dives toward the outside thigh, pauses around the pocket, then
         * naturally returns as the item appears in the hand halfway through.
         */
        arm.xRot =
                Mth.lerp(
                        reach,
                        arm.xRot,
                        0.46F
                );

        arm.yRot =
                Mth.lerp(
                        reach,
                        arm.yRot,
                        side * 0.24F
                );

        arm.zRot =
                Mth.lerp(
                        reach,
                        arm.zRot,
                        side * -0.42F
                );

        model.body.yRot =
                Mth.lerp(
                        reach * 0.35F,
                        model.body.yRot,
                        side * -0.10F
                );
    }

    private static double now() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.level == null
                ? 0.0
                : minecraft.level.getGameTime();
    }

    private record State(
            double startedAt,
            int duration
    ) {
    }
}
