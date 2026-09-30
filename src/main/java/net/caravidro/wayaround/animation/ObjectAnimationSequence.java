package net.caravidro.wayaround.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/**
 * A complete multi-object animation.
 *
 * Actions with no dependencies, or with the same completed dependencies, can
 * run in parallel. An action can explicitly wait for one or more other action
 * ids, making sequences deterministic without hard-coded absolute timings.
 */
public final class ObjectAnimationSequence {

    public record Timing(
            float startTick,
            float endTick
    ) {
    }

    private final ResourceLocation id;
    private final List<ObjectAnimationAction> actions;
    private final Map<String, ObjectAnimationAction> byId;
    private final Map<String, Timing> timings;
    private final float durationTicks;

    private ObjectAnimationSequence(
            ResourceLocation id,
            List<ObjectAnimationAction> actions
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Animation sequence id cannot be null"
            );
        }

        if (actions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Animation sequence needs at least one action"
            );
        }

        this.id =
                id;

        this.actions =
                List.copyOf(
                        actions
                );

        this.byId =
                new HashMap<>();

        for (ObjectAnimationAction action :
                actions) {

            if (byId.put(
                    action.id(),
                    action
            ) != null) {
                throw new IllegalArgumentException(
                        "Duplicate animation action id: "
                                + action.id()
                );
            }
        }

        for (ObjectAnimationAction action :
                actions) {

            for (String dependency :
                    action.after()) {

                if (!byId.containsKey(
                        dependency
                )) {
                    throw new IllegalArgumentException(
                            "Unknown animation dependency "
                                    + dependency
                                    + " required by "
                                    + action.id()
                    );
                }
            }
        }

        this.timings =
                new HashMap<>();

        Set<String> visiting =
                new HashSet<>();

        float duration =
                0.0F;

        for (ObjectAnimationAction action :
                actions) {

            Timing timing =
                    resolveTiming(
                            action,
                            visiting
                    );

            duration =
                    Math.max(
                            duration,
                            timing.endTick()
                    );
        }

        validateActorOverlap();

        this.durationTicks =
                duration;
    }

    public static Builder builder(
            ResourceLocation id
    ) {
        return new Builder(
                id
        );
    }

    public ResourceLocation id() {
        return id;
    }

    public float durationTicks() {
        return durationTicks;
    }

    public Timing timing(
            String actionId
    ) {
        Timing timing =
                timings.get(
                        actionId
                );

        if (timing == null) {
            throw new IllegalArgumentException(
                    "Unknown animation action: "
                            + actionId
            );
        }

        return timing;
    }

    public boolean finished(
            float elapsedTicks
    ) {
        return elapsedTicks
                >= durationTicks;
    }

    public ObjectAnimationPose pose(
            String actor,
            float elapsedTicks
    ) {
        ObjectAnimationAction selected =
                null;

        Timing selectedTiming =
                null;

        for (ObjectAnimationAction action :
                actions) {

            if (!action.actor()
                    .equals(
                            actor
                    )) {
                continue;
            }

            Timing timing =
                    timings.get(
                            action.id()
                    );

            if (elapsedTicks < timing.startTick()) {
                continue;
            }

            if (selectedTiming == null
                    || timing.startTick()
                            > selectedTiming.startTick()) {

                selected =
                        action;

                selectedTiming =
                        timing;
            }
        }

        if (selected == null
                || selectedTiming == null) {
            return ObjectAnimationPose.IDENTITY;
        }

        float localTick =
                elapsedTicks
                        - selectedTiming.startTick();

        return selected.clip()
                .sample(
                        Math.min(
                                localTick,
                                selected.clip()
                                        .durationTicks()
                        )
                );
    }

    private Timing resolveTiming(
            ObjectAnimationAction action,
            Set<String> visiting
    ) {
        Timing cached =
                timings.get(
                        action.id()
                );

        if (cached != null) {
            return cached;
        }

        if (!visiting.add(
                action.id()
        )) {
            throw new IllegalArgumentException(
                    "Animation dependency cycle contains "
                            + action.id()
            );
        }

        float start =
                0.0F;

        for (String dependencyId :
                action.after()) {

            ObjectAnimationAction dependency =
                    byId.get(
                            dependencyId
                    );

            Timing dependencyTiming =
                    resolveTiming(
                            dependency,
                            visiting
                    );

            start =
                    Math.max(
                            start,
                            dependencyTiming.endTick()
                    );
        }

        start +=
                action.delayTicks();

        Timing result =
                new Timing(
                        start,
                        start
                                + action.clip()
                                        .durationTicks()
                );

        timings.put(
                action.id(),
                result
        );

        visiting.remove(
                action.id()
        );

        return result;
    }

    private void validateActorOverlap() {
        for (int firstIndex =
                     0;
             firstIndex < actions.size();
             firstIndex++) {

            ObjectAnimationAction first =
                    actions.get(
                            firstIndex
                    );

            Timing firstTiming =
                    timings.get(
                            first.id()
                    );

            for (int secondIndex =
                         firstIndex + 1;
                 secondIndex < actions.size();
                 secondIndex++) {

                ObjectAnimationAction second =
                        actions.get(
                                secondIndex
                        );

                if (!first.actor()
                        .equals(
                                second.actor()
                        )) {
                    continue;
                }

                Timing secondTiming =
                        timings.get(
                                second.id()
                        );

                boolean overlap =
                        firstTiming.startTick()
                                < secondTiming.endTick()
                                && secondTiming.startTick()
                                        < firstTiming.endTick();

                if (overlap) {
                    throw new IllegalArgumentException(
                            "Actor "
                                    + first.actor()
                                    + " has overlapping actions "
                                    + first.id()
                                    + " and "
                                    + second.id()
                    );
                }
            }
        }
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final List<ObjectAnimationAction> actions =
                new ArrayList<>();

        private Builder(
                ResourceLocation id
        ) {
            this.id =
                    id;
        }

        public Builder action(
                String actionId,
                String actor,
                ObjectAnimationClip clip,
                String... after
        ) {
            return actionDelayed(
                    actionId,
                    actor,
                    clip,
                    0.0F,
                    after
            );
        }

        public Builder actionDelayed(
                String actionId,
                String actor,
                ObjectAnimationClip clip,
                float delayTicks,
                String... after
        ) {
            actions.add(
                    new ObjectAnimationAction(
                            actionId,
                            actor,
                            clip,
                            delayTicks,
                            after == null
                                    ? List.of()
                                    : List.of(
                                            after
                                    )
                    )
            );

            return this;
        }

        public ObjectAnimationSequence build() {
            return new ObjectAnimationSequence(
                    id,
                    actions
            );
        }
    }
}
