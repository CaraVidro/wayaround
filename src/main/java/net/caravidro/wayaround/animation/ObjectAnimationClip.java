package net.caravidro.wayaround.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.util.Mth;

/**
 * One reusable animation performed by one actor.
 */
public final class ObjectAnimationClip {

    public enum Easing {
        LINEAR,
        SMOOTH,
        EASE_IN,
        EASE_OUT;

        float apply(
                float value
        ) {
            float t =
                    Mth.clamp(
                            value,
                            0.0F,
                            1.0F
                    );

            return switch (this) {
                case LINEAR -> t;
                case SMOOTH -> t * t * (3.0F - 2.0F * t);
                case EASE_IN -> t * t;
                case EASE_OUT -> 1.0F - (1.0F - t) * (1.0F - t);
            };
        }
    }

    public record Keyframe(
            float tick,
            ObjectAnimationPose pose,
            Easing easing
    ) {
        public Keyframe {
            if (tick < 0.0F) {
                throw new IllegalArgumentException(
                        "Animation keyframe tick must be >= 0"
                );
            }

            if (pose == null) {
                throw new IllegalArgumentException(
                        "Animation keyframe pose cannot be null"
                );
            }

            if (easing == null) {
                easing =
                        Easing.SMOOTH;
            }
        }
    }

    private final float durationTicks;
    private final List<Keyframe> keyframes;

    private ObjectAnimationClip(
            float durationTicks,
            List<Keyframe> keyframes
    ) {
        if (durationTicks <= 0.0F) {
            throw new IllegalArgumentException(
                    "Animation duration must be > 0"
            );
        }

        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Animation clip needs at least one keyframe"
            );
        }

        ArrayList<Keyframe> sorted =
                new ArrayList<>(
                        keyframes
                );

        sorted.sort(
                Comparator.comparingDouble(
                        Keyframe::tick
                )
        );

        if (sorted.get(
                sorted.size() - 1
        ).tick() > durationTicks) {
            throw new IllegalArgumentException(
                    "Keyframe extends past clip duration"
            );
        }

        this.durationTicks =
                durationTicks;

        this.keyframes =
                List.copyOf(
                        sorted
                );
    }

    public static Builder builder(
            float durationTicks
    ) {
        return new Builder(
                durationTicks
        );
    }

    public float durationTicks() {
        return durationTicks;
    }

    public ObjectAnimationPose sample(
            float tick
    ) {
        if (tick <= keyframes.get(0).tick()) {
            return keyframes.get(0).pose();
        }

        Keyframe previous =
                keyframes.get(0);

        for (int index =
                     1;
             index < keyframes.size();
             index++) {

            Keyframe next =
                    keyframes.get(index);

            if (tick <= next.tick()) {
                float span =
                        Math.max(
                                0.0001F,
                                next.tick()
                                        - previous.tick()
                        );

                float progress =
                        (
                                tick
                                        - previous.tick()
                        )
                                / span;

                return ObjectAnimationPose.lerp(
                        previous.pose(),
                        next.pose(),
                        next.easing()
                                .apply(
                                        progress
                                )
                );
            }

            previous =
                    next;
        }

        return previous.pose();
    }

    public ObjectAnimationPose endPose() {
        return keyframes.get(
                keyframes.size() - 1
        ).pose();
    }

    public static final class Builder {
        private final float durationTicks;
        private final List<Keyframe> keyframes =
                new ArrayList<>();

        private Builder(
                float durationTicks
        ) {
            this.durationTicks =
                    durationTicks;
        }

        public Builder keyframe(
                float tick,
                ObjectAnimationPose pose
        ) {
            return keyframe(
                    tick,
                    pose,
                    Easing.SMOOTH
            );
        }

        public Builder keyframe(
                float tick,
                ObjectAnimationPose pose,
                Easing easing
        ) {
            keyframes.add(
                    new Keyframe(
                            tick,
                            pose,
                            easing
                    )
            );

            return this;
        }

        public ObjectAnimationClip build() {
            return new ObjectAnimationClip(
                    durationTicks,
                    keyframes
            );
        }
    }
}
