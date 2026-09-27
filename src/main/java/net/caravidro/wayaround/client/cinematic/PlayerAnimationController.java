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
import net.neoforged.bus.api.EventPriority;
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

        // Every transfer starts a fresh bow, even if another cinematic was locked.
        if (payload.animation() == PlayerCinematicPayload.TUKUNA_TAKEOVER
                || payload.animation() == PlayerCinematicPayload.TUKUNA_RETURN) {
            CinematicCameraController.clear(payload.player());
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

    public static float animationAge(UUID player, byte animation) {
        AnimationState state = ACTIVE.get(player);
        return state != null && state.animation == animation ? age(state) : -1.0F;
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

            case PlayerCinematicPayload.DESMARTELAR_CHARGE, PlayerCinematicPayload.DESMARTELAR_FIRE_CHARGE ->
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

            case PlayerCinematicPayload.TUKUNA_DOMAIN_PREVIEW -> {
                float seal = ease(age / 20.0F);
                model.leftArm.xRot = -1.25F * seal;
                model.rightArm.xRot = -1.25F * seal;
                model.leftArm.yRot = -.60F * seal;
                model.rightArm.yRot = .60F * seal;
                model.head.xRot += .22F * seal;
            }
            case PlayerCinematicPayload.TUKUNA_TAKEOVER, PlayerCinematicPayload.TUKUNA_RETURN ->
                    applyTukunaTakeover(model, age, tukunaBowWeight(state));

            case PlayerCinematicPayload.TUKUNA_FINGER_REACTION ->
                    applyTukunaFingerReaction(model, age);

            case PlayerCinematicPayload.TUKUNA_FORCE_FEED ->
                    applyTukunaForceFeed(model, age);

            case PlayerCinematicPayload.TUKUNA_FORCED_EAT ->
                    applyTukunaForcedEat(model, age);

            case PlayerCinematicPayload.MELEE_PUNCH ->
                    applyMeleePunch(
                            model,
                            age,
                            Math.floorMod(
                                    player.getUUID().hashCode()
                                            ^ (int)state.startedAt,
                                    4
                            )
                    );

            case PlayerCinematicPayload.MELEE_BLOCK ->
                    applyMeleeBlock(
                            model,
                            age
                    );

            case PlayerCinematicPayload.MELEE_CATCH_ATTACKER ->
                    applyMeleeCatchAttacker(
                            model,
                            age
                    );

            case PlayerCinematicPayload.MELEE_CATCH_DEFENDER ->
                    applyMeleeCatchDefender(
                            model,
                            age
                    );

            case PlayerCinematicPayload.MELEE_LAUNCH ->
                    applyMeleeLaunch(
                            model,
                            age
                    );

            case PlayerCinematicPayload.MELEE_DOWNSLAM ->
                    applyMeleeDownslam(
                            model,
                            age
                    );

            case PlayerCinematicPayload.MELEE_UPPERCUT ->
                    applyMeleeUppercut(
                            model,
                            age
                    );

            case PlayerCinematicPayload.BLACK_FLASH_HEAVY ->
                    applyBlackFlashHeavy(
                            model,
                            age,
                            false
                    );

            case PlayerCinematicPayload.BLACK_FLASH_ULTIMATE ->
                    applyBlackFlashHeavy(
                            model,
                            age,
                            true
                    );

            default -> {
            }
        }
        if (state.animation == PlayerCinematicPayload.DESMARTELAR_CHARGE
                || state.animation == PlayerCinematicPayload.DESMARTELAR_FIRE_CHARGE) {
            float variation = ((player.getUUID().hashCode() ^ (int)state.startedAt) & 1) == 0 ? 1 : -1;
            model.body.yRot += variation * .18F;
            model.rightArm.zRot += variation * .12F;
            if (state.animation == PlayerCinematicPayload.DESMARTELAR_FIRE_CHARGE) model.leftArm.xRot = -1.1F;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
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
                && (state.animation == PlayerCinematicPayload.FUGA_RELEASE
                    || state.animation == PlayerCinematicPayload.TUKUNA_TAKEOVER
                    || state.animation == PlayerCinematicPayload.TUKUNA_RETURN
                    || state.animation == PlayerCinematicPayload.TUKUNA_FINGER_REACTION
                    || state.animation == PlayerCinematicPayload.TUKUNA_FORCED_EAT
                    || state.animation == PlayerCinematicPayload.BLACK_FLASH_HEAVY
                    || state.animation == PlayerCinematicPayload.BLACK_FLASH_ULTIMATE);
    }

    public static float tukunaBowWeight(UUID player) {
        AnimationState state = state(player);
        return state == null ? 0.0F : tukunaBowWeight(state);
    }

    private static float tukunaBowWeight(AnimationState state) {
        if (state.animation != PlayerCinematicPayload.TUKUNA_TAKEOVER
                && state.animation != PlayerCinematicPayload.TUKUNA_RETURN) return 0.0F;
        float progress = age(state) / Math.max(1, state.duration);
        return ease(Mth.clamp(progress / 0.25F, 0.0F, 1.0F))
                * (1.0F - ease(Mth.clamp((progress - 0.65F) / 0.35F, 0.0F, 1.0F)));
    }

    public static float cameraPitchOffsetDegrees(
            UUID player
    ) {
        AnimationState state =
                state(
                        player
                );

        if (state == null) {
            return 0.0F;
        }

        float age =
                age(
                        state
                );

        if (state.animation == PlayerCinematicPayload.TUKUNA_FINGER_REACTION) {
            float enter = ease(age / 10.0F);
            float leave = ease(Mth.clamp((age - 42.0F) / 18.0F, 0.0F, 1.0F));
            float weight = enter * (1.0F - leave);
            return 44.7F * weight;
        }

        if (state.animation == PlayerCinematicPayload.TUKUNA_FORCED_EAT) {
            float up = ease(Mth.clamp(age / 8.0F, 0.0F, 1.0F))
                    * (1.0F - ease(Mth.clamp((age - 24.0F) / 8.0F, 0.0F, 1.0F)));
            float down = ease(Mth.clamp((age - 28.0F) / 7.0F, 0.0F, 1.0F))
                    * (1.0F - ease(Mth.clamp((age - 43.0F) / 10.0F, 0.0F, 1.0F)));
            return (float)Math.toDegrees(-0.88F * up + 0.82F * down);
        }

        if (state.animation == PlayerCinematicPayload.BLACK_FLASH_HEAVY) {
            float punch = ease(Mth.clamp(age / 5.0F, 0.0F, 1.0F));
            float recover = ease(Mth.clamp((age - 10.0F) / 9.0F, 0.0F, 1.0F));
            return Mth.lerp(recover, -8.0F * punch, 0.0F);
        }

        if (state.animation == PlayerCinematicPayload.BLACK_FLASH_ULTIMATE) {
            float wind = ease(Mth.clamp(age / 8.0F, 0.0F, 1.0F));
            float impact = ease(Mth.clamp((age - 8.0F) / 4.0F, 0.0F, 1.0F));
            float recover = ease(Mth.clamp((age - 17.0F) / 10.0F, 0.0F, 1.0F));
            float value = Mth.lerp(impact, 7.0F * wind, -15.0F);
            return Mth.lerp(recover, value, 0.0F);
        }

        if (state.animation
                != PlayerCinematicPayload.FUGA_RELEASE) {
            return 0.0F;
        }

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

    private static void applyFugaCharge(PlayerModel<?> model, float age) {
        float raise = ease(age / 50.0F);
        float clap = ease((age - 48.0F) / 8.0F);
        float draw = ease((age - 60.0F) / 42.0F);
        float breath = Mth.sin(age * .10F) * .025F * draw;
        model.body.yRot = -.25F * draw;
        model.body.xRot = .12F * draw;
        model.leftArm.xRot = -1.8F * raise + .35F * draw + breath;
        model.rightArm.xRot = -1.8F * raise + .85F * draw - breath;
        model.leftArm.yRot = -.65F * clap * (1-draw) - .10F * draw;
        model.rightArm.yRot = .65F * clap * (1-draw) + .92F * draw;
        model.leftArm.zRot = -.25F * raise * (1-clap);
        model.rightArm.zRot = .25F * raise * (1-clap) + .20F * draw;
        model.leftLeg.xRot = .17F * draw;
        model.rightLeg.xRot = -.21F * draw;
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

    private static void applyTukunaFingerReaction(
            PlayerModel<?> model,
            float age
    ) {
        float enter = ease(age / 10.0F);
        float leave = ease(Mth.clamp((age - 42.0F) / 18.0F, 0.0F, 1.0F));
        float weight = enter * (1.0F - leave);

        model.body.xRot += 0.38F * weight;
        model.head.xRot += 0.78F * weight;
        model.leftArm.xRot += 0.18F * weight;
        model.rightArm.xRot += 0.18F * weight;
    }

    private static void applyTukunaForceFeed(
            PlayerModel<?> model,
            float age
    ) {
        float enter = ease(age / 8.0F);
        float leave = ease(Mth.clamp((age - 34.0F) / 14.0F, 0.0F, 1.0F));
        float weight = enter * (1.0F - leave);

        model.body.xRot += 0.20F * weight;
        model.body.yRot -= 0.18F * weight;
        model.rightArm.xRot = Mth.lerp(weight, model.rightArm.xRot, -1.78F);
        model.rightArm.yRot = Mth.lerp(weight, model.rightArm.yRot, -0.18F);
        model.leftArm.xRot = Mth.lerp(weight, model.leftArm.xRot, -0.44F);
        model.head.xRot -= 0.16F * weight;
    }

    private static void applyTukunaForcedEat(
            PlayerModel<?> model,
            float age
    ) {
        float up = ease(Mth.clamp(age / 8.0F, 0.0F, 1.0F))
                * (1.0F - ease(Mth.clamp((age - 24.0F) / 8.0F, 0.0F, 1.0F)));

        float down = ease(Mth.clamp((age - 28.0F) / 7.0F, 0.0F, 1.0F))
                * (1.0F - ease(Mth.clamp((age - 43.0F) / 10.0F, 0.0F, 1.0F)));

        model.head.xRot += -0.88F * up + 0.82F * down;
        model.body.xRot += 0.16F * down;
        model.leftArm.xRot += 0.10F * down;
        model.rightArm.xRot += 0.10F * down;
    }

    private static void applyMeleePunch(
            PlayerModel<?> model,
            float age,
            int variant
    ) {
        float strike =
                ease(
                        Mth.clamp(
                                age / 3.0F,
                                0.0F,
                                1.0F
                        )
                );

        float recover =
                ease(
                        Mth.clamp(
                                (age - 4.0F) / 5.0F,
                                0.0F,
                                1.0F
                        )
                );

        float weight =
                strike
                        * (1.0F - recover);

        float side =
                (variant & 1) == 0
                        ? 1.0F
                        : -1.0F;

        boolean right =
                variant == 0
                        || variant == 3;

        ModelPart arm =
                right
                        ? model.rightArm
                        : model.leftArm;

        arm.xRot =
                Mth.lerp(
                        weight,
                        arm.xRot,
                        variant >= 2
                                ? -1.68F
                                : -1.42F
                );

        arm.yRot +=
                side
                        * 0.24F
                        * weight;

        arm.zRot +=
                side
                        * (
                        variant == 1
                                ? 0.24F
                                : 0.10F
                )
                        * weight;

        model.body.yRot +=
                side
                        * 0.34F
                        * weight;

        model.body.xRot +=
                (variant == 2
                        ? 0.18F
                        : 0.06F)
                        * weight;

        model.head.yRot -=
                side
                        * 0.14F
                        * weight;
    }

    private static void applyMeleeBlock(
            PlayerModel<?> model,
            float age
    ) {
        float weight =
                ease(
                        age / 5.0F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -1.18F
                );

        model.leftArm.xRot =
                Mth.lerp(
                        weight,
                        model.leftArm.xRot,
                        -1.02F
                );

        model.rightArm.yRot =
                Mth.lerp(
                        weight,
                        model.rightArm.yRot,
                        -0.62F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        weight,
                        model.leftArm.yRot,
                        0.46F
                );

        model.body.yRot +=
                0.10F
                        * weight;
    }

    private static void applyMeleeCatchAttacker(
            PlayerModel<?> model,
            float age
    ) {
        float snap =
                ease(
                        age / 2.0F
                );

        float release =
                ease(
                        Mth.clamp(
                                (age - 6.0F) / 3.0F,
                                0.0F,
                                1.0F
                        )
                );

        float weight =
                snap
                        * (1.0F - release);

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -1.58F
                );

        model.rightArm.yRot +=
                0.16F
                        * weight;

        model.body.yRot -=
                0.16F
                        * weight;
    }

    private static void applyMeleeCatchDefender(
            PlayerModel<?> model,
            float age
    ) {
        float snap =
                ease(
                        age / 2.0F
                );

        float release =
                ease(
                        Mth.clamp(
                                (age - 6.0F) / 3.0F,
                                0.0F,
                                1.0F
                        )
                );

        float weight =
                snap
                        * (1.0F - release);

        model.leftArm.xRot =
                Mth.lerp(
                        weight,
                        model.leftArm.xRot,
                        -1.48F
                );

        model.leftArm.yRot =
                Mth.lerp(
                        weight,
                        model.leftArm.yRot,
                        0.84F
                );

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -0.72F
                );

        model.body.yRot +=
                0.22F
                        * weight;
    }

    private static void applyMeleeLaunch(
            PlayerModel<?> model,
            float age
    ) {
        float strike =
                ease(
                        age / 4.0F
                );

        float recover =
                ease(
                        Mth.clamp(
                                (age - 7.0F) / 7.0F,
                                0.0F,
                                1.0F
                        )
                );

        float weight =
                strike
                        * (1.0F - recover);

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -1.78F
                );

        model.body.yRot -=
                0.48F
                        * weight;

        model.body.xRot +=
                0.16F
                        * weight;

        model.leftArm.zRot -=
                0.18F
                        * weight;
    }

    private static void applyMeleeDownslam(
            PlayerModel<?> model,
            float age
    ) {
        float weight =
                ease(
                        Mth.clamp(
                                age / 5.0F,
                                0.0F,
                                1.0F
                        )
                )
                        * (
                        1.0F
                                - ease(
                                Mth.clamp(
                                        (age - 9.0F) / 7.0F,
                                        0.0F,
                                        1.0F
                                )
                        )
                );

        model.body.xRot +=
                0.62F
                        * weight;

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -2.52F
                );

        model.leftArm.xRot =
                Mth.lerp(
                        weight,
                        model.leftArm.xRot,
                        -2.16F
                );

        model.head.xRot +=
                0.42F
                        * weight;
    }

    private static void applyMeleeUppercut(
            PlayerModel<?> model,
            float age
    ) {
        float weight =
                ease(
                        Mth.clamp(
                                age / 4.0F,
                                0.0F,
                                1.0F
                        )
                )
                        * (
                        1.0F
                                - ease(
                                Mth.clamp(
                                        (age - 8.0F) / 7.0F,
                                        0.0F,
                                        1.0F
                                )
                        )
                );

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        -2.82F
                );

        model.rightArm.yRot -=
                0.18F
                        * weight;

        model.body.xRot -=
                0.32F
                        * weight;

        model.body.yRot -=
                0.24F
                        * weight;

        model.head.xRot -=
                0.24F
                        * weight;
    }

    private static void applyBlackFlashHeavy(
            PlayerModel<?> model,
            float age,
            boolean ultimate
    ) {
        float wind =
                ease(
                        Mth.clamp(
                                age / (
                                        ultimate
                                                ? 8.0F
                                                : 5.0F
                                ),
                                0.0F,
                                1.0F
                        )
                );

        float release =
                ease(
                        Mth.clamp(
                                (
                                        age
                                                - (
                                                ultimate
                                                        ? 10.0F
                                                        : 7.0F
                                        )
                                )
                                        / (
                                        ultimate
                                                ? 14.0F
                                                : 9.0F
                                ),
                                0.0F,
                                1.0F
                        )
                );

        float weight =
                wind
                        * (1.0F - release);

        model.body.yRot -=
                (
                        ultimate
                                ? 0.72F
                                : 0.52F
                )
                        * weight;

        model.body.xRot +=
                (
                        ultimate
                                ? 0.26F
                                : 0.16F
                )
                        * weight;

        model.rightArm.xRot =
                Mth.lerp(
                        weight,
                        model.rightArm.xRot,
                        ultimate
                                ? -2.32F
                                : -1.94F
                );

        model.rightArm.yRot +=
                0.24F
                        * weight;

        model.leftArm.xRot =
                Mth.lerp(
                        weight,
                        model.leftArm.xRot,
                        -0.48F
                );

        model.head.yRot +=
                0.22F
                        * weight;
    }

    private static void applyTukunaTakeover(PlayerModel<?> model, float age, float weight) {
        model.body.xRot += 0.98F * weight;
        model.body.yRot += Mth.sin(age * 0.16F) * 0.035F * weight;
        model.head.xRot += 0.70F * weight;
        model.leftArm.xRot += 0.34F * weight;
        model.rightArm.xRot += 0.34F * weight;
        model.leftArm.zRot -= 0.18F * weight;
        model.rightArm.zRot += 0.18F * weight;
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

