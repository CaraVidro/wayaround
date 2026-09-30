package net.caravidro.wayaround.animation;

import net.caravidro.wayaround.industrial.animation.PressAnimations;
import net.minecraft.resources.ResourceLocation;

public final class ObjectAnimationSequenceTest {

    public static void main(
            String[] args
    ) {
        parallelActionsShareStart();
        dependencyWaitsForSpecificActions();
        completedActorPoseIsHeld();
        cyclesAreRejected();
        pressTimelineMatchesContract();
        smoothRotationAdvancesBetweenTicks();
        smoothRotationCorrectsGradually();
        angularWrapUsesShortestPath();

        System.out.println(
                "Object animation sequence regression tests passed"
        );
    }

    private static void parallelActionsShareStart() {
        ObjectAnimationClip clip =
                linearClip(
                        10.0F,
                        1.0F
                );

        ObjectAnimationSequence sequence =
                ObjectAnimationSequence.builder(
                        id(
                                "parallel"
                        )
                )
                        .action(
                                "left",
                                "left_actor",
                                clip
                        )
                        .action(
                                "right",
                                "right_actor",
                                clip
                        )
                        .build();

        requireNear(
                sequence.timing(
                        "left"
                ).startTick(),
                sequence.timing(
                        "right"
                ).startTick(),
                "Parallel actions must share their start tick"
        );
    }

    private static void dependencyWaitsForSpecificActions() {
        ObjectAnimationSequence sequence =
                ObjectAnimationSequence.builder(
                        id(
                                "dependency"
                        )
                )
                        .action(
                                "a",
                                "a",
                                linearClip(
                                        6.0F,
                                        1.0F
                                )
                        )
                        .action(
                                "b",
                                "b",
                                linearClip(
                                        10.0F,
                                        1.0F
                                )
                        )
                        .actionDelayed(
                                "c",
                                "c",
                                linearClip(
                                        4.0F,
                                        1.0F
                                ),
                                2.0F,
                                "a",
                                "b"
                        )
                        .build();

        requireNear(
                12.0F,
                sequence.timing(
                        "c"
                ).startTick(),
                "Dependent action must wait for the latest dependency plus delay"
        );
    }

    private static void completedActorPoseIsHeld() {
        ObjectAnimationSequence sequence =
                ObjectAnimationSequence.builder(
                        id(
                                "hold"
                        )
                )
                        .action(
                                "move",
                                "actor",
                                linearClip(
                                        10.0F,
                                        1.0F
                                )
                        )
                        .actionDelayed(
                                "return",
                                "actor",
                                ObjectAnimationClip.builder(
                                        4.0F
                                )
                                        .keyframe(
                                                0.0F,
                                                ObjectAnimationPose.translated(
                                                        1.0F,
                                                        0.0F,
                                                        0.0F
                                                )
                                        )
                                        .keyframe(
                                                4.0F,
                                                ObjectAnimationPose.IDENTITY
                                        )
                                        .build(),
                                5.0F,
                                "move"
                        )
                        .build();

        requireNear(
                1.0F,
                sequence.pose(
                        "actor",
                        12.0F
                ).x(),
                "Finished actor pose must remain held while the next action waits"
        );
    }

    private static void cyclesAreRejected() {
        boolean rejected =
                false;

        try {
            ObjectAnimationSequence.builder(
                    id(
                            "cycle"
                    )
            )
                    .action(
                            "a",
                            "a",
                            linearClip(
                                    2.0F,
                                    1.0F
                            ),
                            "b"
                    )
                    .action(
                            "b",
                            "b",
                            linearClip(
                                    2.0F,
                                    1.0F
                            ),
                            "a"
                    )
                    .build();

        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        require(
                rejected,
                "Animation dependency cycles must be rejected"
        );
    }

    private static void pressTimelineMatchesContract() {
        ObjectAnimationSequence sequence =
                PressAnimations.CYCLE;

        float leftEnd =
                sequence.timing(
                        "left_clamp_close"
                ).endTick();

        float rightEnd =
                sequence.timing(
                        "right_clamp_close"
                ).endTick();

        float ramStart =
                sequence.timing(
                        "ram_down"
                ).startTick();

        require(
                ramStart >= leftEnd
                        && ramStart >= rightEnd,
                "Press ram must wait for both clamps"
        );

        requireNear(
                sequence.timing(
                        "platen_compress"
                ).startTick(),
                sequence.timing(
                        "flywheel_impact"
                ).startTick(),
                "Press impact actors must begin together"
        );

        require(
                sequence.timing(
                        "left_clamp_open"
                ).startTick()
                        >= sequence.timing(
                        "ram_up"
                ).endTick(),
                "Press clamps must wait for ram recovery"
        );

        require(
                sequence.durationTicks()
                        > sequence.timing(
                        "ram_up"
                ).endTick(),
                "Total animation must include final release actions"
        );
    }

    private static void smoothRotationAdvancesBetweenTicks() {
        SmoothObjectAnimation.Rotation rotation =
                new SmoothObjectAnimation.Rotation(
                        0.0F,
                        8.0F,
                        0.0F,
                        180.0F
                );

        rotation.update(
                0.0,
                60.0F
        );

        float halfTick =
                rotation.update(
                        0.5,
                        60.0F
                );

        require(
                halfTick > 0.0F
                        && halfTick < 18.0F,
                "Continuous rotation must advance during partial ticks"
        );
    }

    private static void smoothRotationCorrectsGradually() {
        SmoothObjectAnimation.Rotation rotation =
                new SmoothObjectAnimation.Rotation(
                        0.0F,
                        8.0F,
                        0.50F,
                        180.0F
                );

        rotation.reset(
                0.0,
                0.0F,
                0.0F
        );

        float corrected =
                rotation.update(
                        1.0,
                        0.0F,
                        10.0F
                );

        requireNear(
                5.0F,
                corrected,
                "Authoritative correction should be gradual below snap threshold"
        );
    }

    private static void angularWrapUsesShortestPath() {
        requireNear(
                2.0F,
                SmoothObjectAnimation.shortestDelta(
                        359.0F,
                        1.0F
                ),
                "Angular correction must cross zero using the shortest path"
        );

        requireNear(
                359.0F,
                SmoothObjectAnimation.wrap(
                        -1.0F
                ),
                "Angular wrap must normalize negative values"
        );
    }

    private static ObjectAnimationClip linearClip(
            float duration,
            float endX
    ) {
        return ObjectAnimationClip.builder(
                duration
        )
                .keyframe(
                        0.0F,
                        ObjectAnimationPose.IDENTITY,
                        ObjectAnimationClip.Easing.LINEAR
                )
                .keyframe(
                        duration,
                        ObjectAnimationPose.translated(
                                endX,
                                0.0F,
                                0.0F
                        ),
                        ObjectAnimationClip.Easing.LINEAR
                )
                .build();
    }

    private static ResourceLocation id(
            String path
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "test_"
                        + path
        );
    }

    private static void requireNear(
            float expected,
            float actual,
            String message
    ) {
        if (Math.abs(
                expected
                        - actual
        ) > 0.0001F) {
            throw new AssertionError(
                    message
                            + " (expected "
                            + expected
                            + ", got "
                            + actual
                            + ")"
            );
        }
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(
                    message
            );
        }
    }
}
