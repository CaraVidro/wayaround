package net.caravidro.wayaround.animation;

import java.util.List;
import java.util.Objects;

/**
 * Applies one clip to one named actor. Dependencies refer to other action ids
 * and mean "start only after all of these actions have finished".
 */
public record ObjectAnimationAction(
        String id,
        String actor,
        ObjectAnimationClip clip,
        float delayTicks,
        List<String> after
) {
    public ObjectAnimationAction {
        id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        actor =
                Objects.requireNonNull(
                        actor,
                        "actor"
                );

        clip =
                Objects.requireNonNull(
                        clip,
                        "clip"
                );

        delayTicks =
                Math.max(
                        0.0F,
                        delayTicks
                );

        after =
                after == null
                        ? List.of()
                        : List.copyOf(
                                after
                        );
    }
}
