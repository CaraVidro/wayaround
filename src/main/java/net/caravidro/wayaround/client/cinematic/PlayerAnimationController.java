package net.caravidro.wayaround.client.cinematic;

import net.caravidro.wayaround.client.BlueClientEffects;

import net.caravidro.wayaround.client.sound.FugaCinematicSound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Lightweight procedural player animation system.
 *
 * Vanilla builds the normal pose first. A mixin then calls afterSetupAnim so
 * these cinematic transforms become the final pose for that frame. A snapshot
 * is restored after rendering so a pose can never leak into the next player.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class PlayerAnimationController {

    private PlayerAnimationController() {
    }

    private static final Map<UUID, AnimationState> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, ModelSnapshot> SNAPSHOTS =
            new HashMap<>();

    public static void receive(
            PlayerCinematicPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (payload.animation()
                == PlayerCinematicPayload.CLEAR) {
            ACTIVE.remove(
                    payload.player()
            );

            CinematicCameraController.clear(
                    payload.player()
            );

            return;
        }

        long now =
                minecraft.level == null
                        ? 0L
                        : minecraft.level
                                .getGameTime();

        ACTIVE.put(
                payload.player(),
                new AnimationState(
                        payload.animation(),
                        now,
                        Math.max(
                                0,
                                payload.durationTicks()
                        )
                )
        );

        if (payload.animation()
                == PlayerCinematicPayload.FUGA_RELEASE
                && minecraft.player != null
                && minecraft.player.getUUID()
                        .equals(
                                payload.player()
                        )) {

            minecraft.getSoundManager()
                    .play(
                            new FugaCinematicSound(
                                    payload.player()
                            )
                    );
        }

        CinematicCameraController.follow(
                payload.player(),
                payload.lockCamera()
        );

        if (payload.shakeStrength()
                > 0.0F) {
            CinematicCameraController.shake(
                    payload.durationTicks() > 0
                            ? Math.min(
                            payload.durationTicks(),
                            80
                    )
                            : 24,
                    payload.shakeStrength()
            );
        }
    }

    public static boolean isAnimation(
            UUID player,
            byte animation
    ) {
        AnimationState state =
                ACTIVE.get(
                        player
                );

        return state != null
                && state.animation
                        == animation;
    }

    public static void beforeSetupAnim(
            AbstractClientPlayer player,
            PlayerModel<?> model
    ) {
        /*
         * If another renderer skipped RenderPlayerEvent.Post, restore a stale
         * snapshot before taking the next one.
         */
        ModelSnapshot stale =
                SNAPSHOTS.remove(
                        player.getUUID()
                );

        if (stale != null) {
            stale.restore(
                    model
            );
        }

        if (state(
                player.getUUID()
        ) == null
                && BlueClientEffects.controllablePosition(
                player.getUUID()
        ) == null) {

            return;
        }

        SNAPSHOTS.put(
                player.getUUID(),
                ModelSnapshot.capture(
                        model
                )
        );
    }

    public static void afterSetupAnim(
            AbstractClientPlayer player,
            PlayerModel<?> model
    ) {
        AnimationState state =
                state(
                        player.getUUID()
                );

        if (state == null) {
            applyBlueControl(
                    player,
                    model
            );

            return;
        }

        float age =
                age(
                        state
                );

        switch (state.animation) {
            case PlayerCinematicPayload.FUGA_CHARGE ->
                    applyFugaCharge(
                            model,
                            age
                    );

            case PlayerCinematicPayload.FUGA_RELEASE ->
                    applyFugaRelease(
                            model,
                            age
                    );

            case PlayerCinematicPayload.IMMORTAL_REBUILD ->
                    applyImmortalRebuild(
                            model,
                            state,
                            age
                    );

            case PlayerCinematicPayload.DESMARTELAR_CHARGE ->
                    applyDesmartelarCharge(
                            model,
                            age
                    );

            case PlayerCinematicPayload.DESMARTELAR_RELEASE ->
                    applyDesmartelarRelease(
                            model,
                            age
                    );

            case PlayerCinematicPayload.BLUE_CLAP ->
                    applyBlueClap(
                            model,
                            age
                    );

            case PlayerCinematicPayload.RED_HOLD ->
                    applyRedHold(
                            model,
                            age
                    );

            case PlayerCinematicPayload.RED_RELEASE ->
                    applyRedRelease(
                            model,
                            age
                    );

            case PlayerCinematicPayload.PURPLE_FUSION ->
                    applyPurpleFusion(
                            model,
                            age
                    );

            case PlayerCinematicPayload.PURPLE_RELEASE ->
                    applyPurpleRelease(
                            model,
                            age
                    );

            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void afterPlayerRender(
            RenderPlayerEvent.Post event
    ) {
        ModelSnapshot snapshot =
                SNAPSHOTS.remove(
                        event.getEntity()
                                .getUUID()
                );

        if (snapshot != null) {
            snapshot.restore(
                    event.getRenderer()
                            .getModel()
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            ACTIVE.clear();
            SNAPSHOTS.clear();
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        ACTIVE.entrySet()
                .removeIf(
                        entry -> {
                            AnimationState state =
                                    entry.getValue();

                            if (state.duration <= 0
                                    || tick
                                    <= state.startedAt
                                    + state.duration) {
                                return false;
                            }

                            CinematicCameraController.clear(
                                    entry.getKey()
                            );

                            return true;
                        }
                );
    }

    public static boolean hasCameraMotion(
            UUID player
    ) {
        AnimationState state =
                state(
                        player
                );

        return state != null
                && state.animation
                        == PlayerCinematicPayload.FUGA_RELEASE;
    }

    public static float cameraPitchOffsetDegrees(
            UUID player
    ) {
        AnimationState state =
                state(
                        player
                );

        if (state == null
                || state.animation
                        != PlayerCinematicPayload.FUGA_RELEASE) {
            return 0.0F;
        }

        float age =
                age(
                        state
                );

        float t =
                Mth.clamp(
                        age / 34.0F,
                        0.0F,
                        1.0F
                );

        /*
         * A real camera nod rather than locking the view:
         * down -> small upward rebound -> neutral.
         */
        if (t < 0.24F) {
            return Mth.lerp(
                    ease(
                            t / 0.24F
                    ),
                    0.0F,
                    10.0F
            );
        }

        if (t < 0.62F) {
            return Mth.lerp(
                    ease(
                            (
                                    t - 0.24F
                            )
                                    / 0.38F
                    ),
                    10.0F,
                    -3.2F
            );
        }

        return Mth.lerp(
                ease(
                        (
                                t - 0.62F
                        )
                                / 0.38F
                ),
                -3.2F,
                0.0F
        );
    }

    /**
     * Approximate animated head displacement for attachments such as the
     * Immortal Wheel. It intentionally follows the pose rather than the raw
     * entity bounding box.
     */
    public static Vec3 headAnchorOffset(
            Player player
    ) {
        AnimationState state =
                state(
                        player.getUUID()
                );

        if (state == null) {
            return Vec3.ZERO;
        }

        float age =
                age(
                        state
                );

        float amount;

        if (state.animation
                == PlayerCinematicPayload.FUGA_CHARGE) {
            amount =
                    ease(
                            age / 18.0F
                    );

        } else if (state.animation
                == PlayerCinematicPayload.FUGA_RELEASE) {
            amount =
                    1.0F
                            - 0.35F
                                    * ease(
                                    age / 20.0F
                            );

        } else {
            return Vec3.ZERO;
        }

        double yaw =
                Math.toRadians(
                        player.getYRot()
                );

        Vec3 forward =
                new Vec3(
                        -Math.sin(yaw),
                        0.0,
                        Math.cos(yaw)
                );

        return forward.scale(
                0.16
                        * amount
        ).add(
                0.0,
                -0.11
                        * amount,
                0.0
        );
    }

    private static void applyFugaCharge(
            PlayerModel<?> model,
            float age
    ) {
        float p =
                ease(
                        age / 18.0F
                );

        model.body.xRot =
                Mth.lerp(
                        p,
                        model.body.xRot,
                        0.25F
                );

        model.body.yRot =
                Mth.lerp(
                        p,
                        model.body.yRot,
                        -0.08F
                );

        model.head.xRot +=
                0.23F
                        * p;

        model.leftArm.xRot =
                Mth.lerp(
                        p,
                        model.leftArm.xRot,
                        -1.43F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        p,
                        model.leftArm.yRot,
                        -0.12F
                );

        model.leftArm.zRot =
                Mth.lerp(
                        p,
                        model.leftArm.zRot,
                        -0.08F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        p,
                        model.rightArm.xRot,
                        -0.92F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        p,
                        model.rightArm.yRot,
                        0.82F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        p,
                        model.rightArm.zRot,
                        0.24F
                );

        model.leftLeg.xRot =
                Mth.lerp(
                        p,
                        model.leftLeg.xRot,
                        0.17F
                );

        model.rightLeg.xRot =
                Mth.lerp(
                        p,
                        model.rightLeg.xRot,
                        -0.21F
                );
    }

    private static void applyFugaRelease(
            PlayerModel<?> model,
            float age
    ) {
        float p =
                ease(
                        age / 20.0F
                );

        model.body.xRot =
                Mth.lerp(
                        p,
                        0.25F,
                        0.12F
                );

        model.body.yRot =
                Mth.lerp(
                        p,
                        -0.08F,
                        0.0F
                );

        model.head.xRot +=
                Mth.lerp(
                        p,
                        0.23F,
                        0.08F
                );

        // Front arm lowers only a little after the shot.
        model.leftArm.xRot =
                Mth.lerp(
                        p,
                        -1.43F,
                        -0.92F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        p,
                        -0.12F,
                        -0.03F
                );

        // String arm snaps free and falls backward/downward.
        float snap =
                ease(
                        Math.min(
                                1.0F,
                                age / 7.0F
                        )
                );

        model.rightArm.xRot =
                Mth.lerp(
                        snap,
                        -0.92F,
                        0.30F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        snap,
                        0.82F,
                        0.10F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        snap,
                        0.24F,
                        -0.08F
                );

        model.leftLeg.xRot =
                0.12F
                        * (
                        1.0F - p
                );

        model.rightLeg.xRot =
                -0.15F
                        * (
                        1.0F - p
                );
    }

    private static void applyBlueControl(
            AbstractClientPlayer player,
            PlayerModel<?> model
    ) {
        Vec3 blue =
                BlueClientEffects.controllablePosition(
                        player.getUUID()
                );

        if (blue == null) {
            return;
        }

        double distance =
                player.getEyePosition()
                        .distanceTo(
                                blue
                        );

        float far =
                Mth.clamp(
                        (float) (
                                (
                                        distance - 2.0
                                )
                                        / 46.0
                        ),
                        0.0F,
                        1.0F
                );

        /*
         * Close Blue: right hand high, almost "holding the sky".
         * Far Blue: arm progressively points out from the torso.
         */
        model.rightArm.xRot =
                Mth.lerp(
                        far,
                        -2.58F,
                        -1.48F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        far,
                        -0.18F,
                        -0.06F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        far,
                        0.18F,
                        0.03F
                );

        model.body.yRot =
                -0.035F
                        * (
                        1.0F - far
                );
    }

    private static void applyBlueClap(
            PlayerModel<?> model,
            float age
    ) {
        float close =
                ease(
                        Math.min(
                                1.0F,
                                age / 9.0F
                        )
                );

        float release =
                ease(
                        Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        (
                                                age - 11.0F
                                        )
                                                / 9.0F
                                )
                        )
                );

        float x =
                Mth.lerp(
                        release,
                        Mth.lerp(
                                close,
                                -0.45F,
                                -1.38F
                        ),
                        0.0F
                );

        float y =
                Mth.lerp(
                        release,
                        Mth.lerp(
                                close,
                                0.10F,
                                0.62F
                        ),
                        0.0F
                );

        model.leftArm.xRot =
                x;

        model.rightArm.xRot =
                x;

        model.leftArm.yRot =
                y;

        model.rightArm.yRot =
                -y;

        model.leftArm.zRot =
                Mth.lerp(
                        release,
                        -0.12F
                                * close,
                        0.0F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        release,
                        0.12F
                                * close,
                        0.0F
                );
    }

    private static void applyRedHold(
            PlayerModel<?> model,
            float age
    ) {
        float p =
                ease(
                        age / 7.0F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        p,
                        model.rightArm.xRot,
                        -1.48F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        p,
                        model.rightArm.yRot,
                        -0.10F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        p,
                        model.rightArm.zRot,
                        0.02F
                );

        model.body.xRot =
                0.06F
                        * p;

        model.body.yRot =
                -0.07F
                        * p;
    }

    private static void applyRedRelease(
            PlayerModel<?> model,
            float age
    ) {
        float snap =
                ease(
                        Math.min(
                                1.0F,
                                age / 5.0F
                        )
                );

        float settle =
                ease(
                        Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        (
                                                age - 5.0F
                                        )
                                                / 19.0F
                                )
                        )
                );

        float wobble =
                (float) Math.sin(
                        age
                                * 0.95F
                )
                        * 0.18F
                        * (
                        1.0F - settle
                );

        model.rightArm.xRot =
                Mth.lerp(
                        settle,
                        Mth.lerp(
                                snap,
                                -1.48F,
                                -0.72F
                        ),
                        0.0F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        settle,
                        -0.10F
                                + snap
                                        * 0.48F,
                        0.0F
                );

        model.body.yRot =
                wobble;

        model.head.yRot +=
                wobble
                        * 0.55F;
    }

    private static void applyPurpleFusion(
            PlayerModel<?> model,
            float age
    ) {
        float p =
                ease(
                        age / 12.0F
                );

        model.leftArm.xRot =
                Mth.lerp(
                        p,
                        model.leftArm.xRot,
                        -1.42F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        p,
                        model.rightArm.xRot,
                        -1.42F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        p,
                        model.leftArm.yRot,
                        0.58F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        p,
                        model.rightArm.yRot,
                        -0.58F
                );

        model.body.xRot =
                0.10F
                        * p;
    }

    private static void applyPurpleRelease(
            PlayerModel<?> model,
            float age
    ) {
        float push =
                ease(
                        Math.min(
                                1.0F,
                                age / 8.0F
                        )
                );

        float returnProgress =
                ease(
                        Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        (
                                                age - 20.0F
                                        )
                                                / 28.0F
                                )
                        )
                );

        float x =
                Mth.lerp(
                        returnProgress,
                        Mth.lerp(
                                push,
                                -1.42F,
                                -1.72F
                        ),
                        0.0F
                );

        float y =
                Mth.lerp(
                        returnProgress,
                        Mth.lerp(
                                push,
                                0.58F,
                                0.20F
                        ),
                        0.0F
                );

        model.leftArm.xRot =
                x;

        model.rightArm.xRot =
                x;

        model.leftArm.yRot =
                y;

        model.rightArm.yRot =
                -y;

        model.body.xRot =
                Mth.lerp(
                        returnProgress,
                        0.12F,
                        0.0F
                );
    }

    private static void applyDesmartelarCharge(
            PlayerModel<?> model,
            float age
    ) {
        float p =
                ease(
                        age / 8.0F
                );

        model.body.xRot =
                Mth.lerp(
                        p,
                        model.body.xRot,
                        0.10F
                );

        model.leftArm.xRot =
                Mth.lerp(
                        p,
                        model.leftArm.xRot,
                        -1.08F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        p,
                        model.rightArm.xRot,
                        -1.08F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        p,
                        model.leftArm.yRot,
                        0.48F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        p,
                        model.rightArm.yRot,
                        -0.48F
                );

        model.leftArm.zRot =
                Mth.lerp(
                        p,
                        model.leftArm.zRot,
                        -0.10F
                );

        model.rightArm.zRot =
                Mth.lerp(
                        p,
                        model.rightArm.zRot,
                        0.10F
                );
    }

    private static void applyDesmartelarRelease(
            PlayerModel<?> model,
            float age
    ) {
        float thrust =
                ease(
                        Math.min(
                                1.0F,
                                age / 5.0F
                        )
                );

        float settle =
                ease(
                        Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        (
                                                age - 5.0F
                                        )
                                                / 13.0F
                                )
                        )
                );

        float armX =
                Mth.lerp(
                        settle,
                        Mth.lerp(
                                thrust,
                                -1.08F,
                                -1.58F
                        ),
                        0.0F
                );

        float armY =
                Mth.lerp(
                        settle,
                        Mth.lerp(
                                thrust,
                                0.48F,
                                0.08F
                        ),
                        0.0F
                );

        model.leftArm.xRot =
                armX;

        model.rightArm.xRot =
                armX;

        model.leftArm.yRot =
                armY;

        model.rightArm.yRot =
                -armY;

        model.leftArm.zRot =
                -0.04F
                        * (
                        1.0F - settle
                );

        model.rightArm.zRot =
                0.04F
                        * (
                        1.0F - settle
                );

        model.body.xRot =
                0.12F
                        * (
                        1.0F - settle
                );
    }

    private static void applyImmortalRebuild(
            PlayerModel<?> model,
            AnimationState state,
            float age
    ) {
        float duration =
                Math.max(
                        1.0F,
                        state.duration
                );

        float p =
                Mth.clamp(
                        age / duration,
                        0.0F,
                        1.0F
                );

        /*
         * Deliberately theatrical order:
         * nothing -> head -> torso -> arms -> legs -> complete body.
         */
        boolean head =
                p >= 0.14F;

        boolean torso =
                p >= 0.30F;

        boolean leftArm =
                p >= 0.47F;

        boolean rightArm =
                p >= 0.60F;

        boolean leftLeg =
                p >= 0.75F;

        boolean rightLeg =
                p >= 0.88F;

        setHeadVisible(
                model,
                head
        );

        setTorsoVisible(
                model,
                torso
        );

        setLeftArmVisible(
                model,
                leftArm
        );

        setRightArmVisible(
                model,
                rightArm
        );

        setLeftLegVisible(
                model,
                leftLeg
        );

        setRightLegVisible(
                model,
                rightLeg
        );
    }

    private static void setHeadVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.head.visible =
                visible;

        model.hat.visible =
                visible;
    }

    private static void setTorsoVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.body.visible =
                visible;

        model.jacket.visible =
                visible;
    }

    private static void setLeftArmVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.leftArm.visible =
                visible;

        model.leftSleeve.visible =
                visible;
    }

    private static void setRightArmVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.rightArm.visible =
                visible;

        model.rightSleeve.visible =
                visible;
    }

    private static void setLeftLegVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.leftLeg.visible =
                visible;

        model.leftPants.visible =
                visible;
    }

    private static void setRightLegVisible(
            PlayerModel<?> model,
            boolean visible
    ) {
        model.rightLeg.visible =
                visible;

        model.rightPants.visible =
                visible;
    }

    private static AnimationState state(
            UUID player
    ) {
        if (player == null) {
            return null;
        }

        AnimationState state =
                ACTIVE.get(
                        player
                );

        if (state == null) {
            return null;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (state.duration > 0
                && minecraft.level != null
                && minecraft.level.getGameTime()
                        > state.startedAt
                        + state.duration) {

            ACTIVE.remove(
                    player
            );

            CinematicCameraController.clear(
                    player
            );

            return null;
        }

        return state;
    }

    private static float age(
            AnimationState state
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return 0.0F;
        }

        return Math.max(
                0.0F,
                minecraft.level
                        .getGameTime()
                        - state.startedAt
        );
    }

    private static float ease(
            float value
    ) {
        float t =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return t
                * t
                * (
                3.0F
                        - 2.0F
                                * t
        );
    }

    private record AnimationState(
            byte animation,
            long startedAt,
            int duration
    ) {
    }

    private record PartState(
            float xRot,
            float yRot,
            float zRot,
            boolean visible
    ) {

        private static PartState capture(
                ModelPart part
        ) {
            return new PartState(
                    part.xRot,
                    part.yRot,
                    part.zRot,
                    part.visible
            );
        }

        private void restore(
                ModelPart part
        ) {
            part.xRot =
                    xRot;

            part.yRot =
                    yRot;

            part.zRot =
                    zRot;

            part.visible =
                    visible;
        }
    }

    private record ModelSnapshot(
            PartState head,
            PartState hat,
            PartState body,
            PartState jacket,
            PartState leftArm,
            PartState leftSleeve,
            PartState rightArm,
            PartState rightSleeve,
            PartState leftLeg,
            PartState leftPants,
            PartState rightLeg,
            PartState rightPants
    ) {

        private static ModelSnapshot capture(
                PlayerModel<?> model
        ) {
            return new ModelSnapshot(
                    PartState.capture(
                            model.head
                    ),
                    PartState.capture(
                            model.hat
                    ),
                    PartState.capture(
                            model.body
                    ),
                    PartState.capture(
                            model.jacket
                    ),
                    PartState.capture(
                            model.leftArm
                    ),
                    PartState.capture(
                            model.leftSleeve
                    ),
                    PartState.capture(
                            model.rightArm
                    ),
                    PartState.capture(
                            model.rightSleeve
                    ),
                    PartState.capture(
                            model.leftLeg
                    ),
                    PartState.capture(
                            model.leftPants
                    ),
                    PartState.capture(
                            model.rightLeg
                    ),
                    PartState.capture(
                            model.rightPants
                    )
            );
        }

        private void restore(
                PlayerModel<?> model
        ) {
            head.restore(
                    model.head
            );

            hat.restore(
                    model.hat
            );

            body.restore(
                    model.body
            );

            jacket.restore(
                    model.jacket
            );

            leftArm.restore(
                    model.leftArm
            );

            leftSleeve.restore(
                    model.leftSleeve
            );

            rightArm.restore(
                    model.rightArm
            );

            rightSleeve.restore(
                    model.rightSleeve
            );

            leftLeg.restore(
                    model.leftLeg
            );

            leftPants.restore(
                    model.leftPants
            );

            rightLeg.restore(
                    model.rightLeg
            );

            rightPants.restore(
                    model.rightPants
            );
        }
    }
}
