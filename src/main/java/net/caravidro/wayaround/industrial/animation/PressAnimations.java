package net.caravidro.wayaround.industrial.animation;

import net.caravidro.wayaround.animation.ObjectAnimationClip;
import net.caravidro.wayaround.animation.ObjectAnimationPose;
import net.caravidro.wayaround.animation.ObjectAnimationSequence;
import net.minecraft.resources.ResourceLocation;

/** Animation definitions for the mechanical press test machine. */
public final class PressAnimations {

    public static final String LEFT_CLAMP =
            "left_clamp";

    public static final String RIGHT_CLAMP =
            "right_clamp";

    public static final String RAM =
            "ram";

    public static final String PLATEN =
            "platen";

    public static final String FLYWHEEL =
            "flywheel";

    public static final String LEVER =
            "lever";

    private static final ObjectAnimationClip CLAMP_LEFT_CLOSE =
            ObjectAnimationClip.builder(
                    10.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    10.0F,
                    ObjectAnimationPose.translated(
                            0.12F,
                            0.0F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_OUT
            ).build();

    private static final ObjectAnimationClip CLAMP_RIGHT_CLOSE =
            ObjectAnimationClip.builder(
                    10.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    10.0F,
                    ObjectAnimationPose.translated(
                            -0.12F,
                            0.0F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_OUT
            ).build();

    private static final ObjectAnimationClip LEVER_ENGAGE =
            ObjectAnimationClip.builder(
                    8.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    8.0F,
                    ObjectAnimationPose.rotated(
                            0.0F,
                            0.0F,
                            -34.0F
                    )
            ).build();

    private static final ObjectAnimationClip FLYWHEEL_CHARGE =
            ObjectAnimationClip.builder(
                    18.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    18.0F,
                    ObjectAnimationPose.rotated(
                            220.0F,
                            0.0F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_IN
            ).build();

    private static final ObjectAnimationClip RAM_DOWN =
            ObjectAnimationClip.builder(
                    16.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    11.0F,
                    ObjectAnimationPose.translated(
                            0.0F,
                            -0.41F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_IN
            ).keyframe(
                    16.0F,
                    ObjectAnimationPose.translated(
                            0.0F,
                            -0.46F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_IN
            ).build();

    private static final ObjectAnimationPose RAM_DOWN_POSE =
            ObjectAnimationPose.translated(
                    0.0F,
                    -0.46F,
                    0.0F
            );

    private static final ObjectAnimationClip RAM_HOLD =
            ObjectAnimationClip.builder(
                    6.0F
            ).keyframe(
                    0.0F,
                    RAM_DOWN_POSE
            ).keyframe(
                    6.0F,
                    RAM_DOWN_POSE
            ).build();

    private static final ObjectAnimationClip PLATEN_COMPRESS =
            ObjectAnimationClip.builder(
                    6.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.IDENTITY
            ).keyframe(
                    3.0F,
                    new ObjectAnimationPose(
                            0.0F,
                            -0.035F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            1.05F,
                            0.78F,
                            1.05F
                    ),
                    ObjectAnimationClip.Easing.EASE_IN
            ).keyframe(
                    6.0F,
                    new ObjectAnimationPose(
                            0.0F,
                            -0.025F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            1.03F,
                            0.84F,
                            1.03F
                    )
            ).build();

    private static final ObjectAnimationClip FLYWHEEL_IMPACT =
            ObjectAnimationClip.builder(
                    6.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.rotated(
                            220.0F,
                            0.0F,
                            0.0F
                    )
            ).keyframe(
                    6.0F,
                    ObjectAnimationPose.rotated(
                            420.0F,
                            0.0F,
                            0.0F
                    ),
                    ObjectAnimationClip.Easing.EASE_OUT
            ).build();

    private static final ObjectAnimationClip PLATEN_RELEASE =
            ObjectAnimationClip.builder(
                    8.0F
            ).keyframe(
                    0.0F,
                    new ObjectAnimationPose(
                            0.0F,
                            -0.025F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            1.03F,
                            0.84F,
                            1.03F
                    )
            ).keyframe(
                    8.0F,
                    ObjectAnimationPose.IDENTITY
            ).build();

    private static final ObjectAnimationClip RAM_UP =
            ObjectAnimationClip.builder(
                    14.0F
            ).keyframe(
                    0.0F,
                    RAM_DOWN_POSE
            ).keyframe(
                    14.0F,
                    ObjectAnimationPose.IDENTITY,
                    ObjectAnimationClip.Easing.EASE_OUT
            ).build();

    private static final ObjectAnimationClip CLAMP_LEFT_OPEN =
            ObjectAnimationClip.builder(
                    10.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.translated(
                            0.12F,
                            0.0F,
                            0.0F
                    )
            ).keyframe(
                    10.0F,
                    ObjectAnimationPose.IDENTITY
            ).build();

    private static final ObjectAnimationClip CLAMP_RIGHT_OPEN =
            ObjectAnimationClip.builder(
                    10.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.translated(
                            -0.12F,
                            0.0F,
                            0.0F
                    )
            ).keyframe(
                    10.0F,
                    ObjectAnimationPose.IDENTITY
            ).build();

    private static final ObjectAnimationClip LEVER_RELEASE =
            ObjectAnimationClip.builder(
                    8.0F
            ).keyframe(
                    0.0F,
                    ObjectAnimationPose.rotated(
                            0.0F,
                            0.0F,
                            -34.0F
                    )
            ).keyframe(
                    8.0F,
                    ObjectAnimationPose.IDENTITY
            ).build();

    public static final ObjectAnimationSequence CYCLE =
            ObjectAnimationSequence.builder(
                    ResourceLocation.fromNamespaceAndPath(
                            "wayaround",
                            "mechanical_press_cycle"
                    )
            )
                    /*
                     * No dependencies = synchronized start.
                     */
                    .action(
                            "left_clamp_close",
                            LEFT_CLAMP,
                            CLAMP_LEFT_CLOSE
                    )
                    .action(
                            "right_clamp_close",
                            RIGHT_CLAMP,
                            CLAMP_RIGHT_CLOSE
                    )
                    .action(
                            "lever_engage",
                            LEVER,
                            LEVER_ENGAGE
                    )
                    .action(
                            "flywheel_charge",
                            FLYWHEEL,
                            FLYWHEEL_CHARGE
                    )
                    /*
                     * The ram explicitly waits for both clamps. It does not
                     * care whether the lever/flywheel have finished.
                     */
                    .actionDelayed(
                            "ram_down",
                            RAM,
                            RAM_DOWN,
                            2.0F,
                            "left_clamp_close",
                            "right_clamp_close"
                    )
                    /*
                     * Three different objects now react to one completed
                     * action and therefore begin together.
                     */
                    .action(
                            "ram_hold",
                            RAM,
                            RAM_HOLD,
                            "ram_down"
                    )
                    .action(
                            "platen_compress",
                            PLATEN,
                            PLATEN_COMPRESS,
                            "ram_down"
                    )
                    .action(
                            "flywheel_impact",
                            FLYWHEEL,
                            FLYWHEEL_IMPACT,
                            "flywheel_charge",
                            "ram_down"
                    )
                    .action(
                            "platen_release",
                            PLATEN,
                            PLATEN_RELEASE,
                            "platen_compress"
                    )
                    /*
                     * Recovery waits for the complete impact group.
                     */
                    .action(
                            "ram_up",
                            RAM,
                            RAM_UP,
                            "ram_hold",
                            "platen_compress",
                            "flywheel_impact"
                    )
                    .action(
                            "left_clamp_open",
                            LEFT_CLAMP,
                            CLAMP_LEFT_OPEN,
                            "ram_up"
                    )
                    .action(
                            "right_clamp_open",
                            RIGHT_CLAMP,
                            CLAMP_RIGHT_OPEN,
                            "ram_up"
                    )
                    .action(
                            "lever_release",
                            LEVER,
                            LEVER_RELEASE,
                            "left_clamp_open",
                            "right_clamp_open"
                    )
                    .build();

    public static final float IMPACT_TICK =
            CYCLE.timing(
                    "platen_compress"
            ).startTick();

    private PressAnimations() {
    }
}
