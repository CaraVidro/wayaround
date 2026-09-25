package net.caravidro.wayaround.client.cinematic;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Reusable cinematic camera lock.
 *
 * When locked, mouse look is ignored and the camera uses the orientation that
 * existed when the cinematic began plus the animated head rotation. Shake is
 * independent and can remain active even when the camera is not locked.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class CinematicCameraController {

    private CinematicCameraController() {
    }

    private static UUID lockedPlayer;
    private static float baseYaw;
    private static float basePitch;

    private static int shakeTicks;
    private static int shakeDuration;
    private static float shakeStrength;

    public static void follow(
            UUID player,
            boolean lock
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!lock
                || minecraft.player == null
                || !minecraft.player.getUUID()
                        .equals(player)) {
            return;
        }

        if (!player.equals(
                lockedPlayer
        )) {
            lockedPlayer =
                    player;

            baseYaw =
                    minecraft.player
                            .getYRot();

            basePitch =
                    minecraft.player
                            .getXRot();
        }
    }

    public static void clear(
            UUID player
    ) {
        if (player != null
                && player.equals(
                        lockedPlayer
                )) {
            lockedPlayer =
                    null;
        }
    }

    public static void shake(
            int ticks,
            float strength
    ) {
        if (ticks <= 0
                || strength <= 0.0F) {
            return;
        }

        if (ticks >= shakeTicks
                || strength > shakeStrength) {
            shakeTicks =
                    ticks;

            shakeDuration =
                    ticks;

            shakeStrength =
                    Math.max(
                            shakeStrength,
                            strength
                    );
        }
    }

    public static boolean isLocked() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.player != null
                && lockedPlayer != null
                && lockedPlayer.equals(
                        minecraft.player
                                .getUUID()
                );
    }

    public static boolean isActiveFor(
            Entity entity
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.player != null
                && entity == minecraft.player
                && (
                isLocked()
                        || shakeTicks > 0
        );
    }

    public static float cameraYaw(
            Entity entity,
            float partialTick
    ) {
        float yaw =
                isLocked()
                        ? baseYaw
                        : entity.getViewYRot(
                                partialTick
                        );

        return yaw
                + shakeYaw(
                        partialTick
                );
    }

    public static float cameraPitch(
            Entity entity,
            float partialTick
    ) {
        float pitch =
                isLocked()
                        ? basePitch
                        + PlayerAnimationController
                                .cameraPitchOffsetDegrees(
                                        lockedPlayer
                                )
                        : entity.getViewXRot(
                                partialTick
                        );

        return pitch
                + shakePitch(
                        partialTick
                );
    }

    private static float shakeYaw(
            float partialTick
    ) {
        if (shakeTicks <= 0
                || shakeDuration <= 0) {
            return 0.0F;
        }

        float life =
                shakeTicks
                        / (float) shakeDuration;

        double time =
                (
                        shakeDuration
                                - shakeTicks
                                + partialTick
                )
                        * 2.83;

        return (float) (
                (
                        Math.sin(time * 1.37)
                                + Math.sin(
                                time * 2.11
                        )
                                * 0.45
                )
                        * 3.8
                        * shakeStrength
                        * life
        );
    }

    private static float shakePitch(
            float partialTick
    ) {
        if (shakeTicks <= 0
                || shakeDuration <= 0) {
            return 0.0F;
        }

        float life =
                shakeTicks
                        / (float) shakeDuration;

        double time =
                (
                        shakeDuration
                                - shakeTicks
                                + partialTick
                )
                        * 3.21;

        return (float) (
                (
                        Math.cos(time * 1.53)
                                + Math.sin(
                                time * 2.67
                        )
                                * 0.38
                )
                        * 4.6
                        * shakeStrength
                        * life
        );
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            lockedPlayer =
                    null;

            shakeTicks =
                    0;

            shakeDuration =
                    0;

            shakeStrength =
                    0.0F;

            return;
        }

        if (lockedPlayer != null
                && !lockedPlayer.equals(
                minecraft.player
                        .getUUID()
        )) {
            lockedPlayer =
                    null;
        }

        if (isLocked()) {
            minecraft.player
                    .setYRot(
                            baseYaw
                    );

            minecraft.player
                    .setXRot(
                            basePitch
                    );
        }

        if (shakeTicks > 0) {
            shakeTicks--;

            if (shakeTicks == 0) {
                shakeDuration =
                        0;

                shakeStrength =
                        0.0F;
            }
        }
    }
}
