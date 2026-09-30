package net.caravidro.wayaround.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Small synchronized state container for a currently playing object sequence.
 *
 * The server only needs to synchronize the sequence id and start game-time.
 * Every renderer can then sample the same deterministic timeline locally.
 */
public final class ObjectAnimationPlayback {

    private static final String ACTIVE_KEY =
            "Active";

    private static final String SEQUENCE_KEY =
            "Sequence";

    private static final String START_KEY =
            "StartTick";

    private ResourceLocation sequenceId;
    private long startTick;
    private boolean active;

    public void start(
            ObjectAnimationSequence sequence,
            long gameTime
    ) {
        sequenceId =
                sequence.id();

        startTick =
                gameTime;

        active =
                true;
    }

    public void stop() {
        active =
                false;
    }

    public boolean active() {
        return active;
    }

    public boolean active(
            ObjectAnimationSequence sequence
    ) {
        return active
                && sequenceId != null
                && sequenceId.equals(
                        sequence.id()
                );
    }

    public ResourceLocation sequenceId() {
        return sequenceId;
    }

    public float elapsed(
            long gameTime,
            float partialTick
    ) {
        if (!active) {
            return 0.0F;
        }

        return Math.max(
                0.0F,
                gameTime
                        - startTick
                        + partialTick
        );
    }

    public boolean finishIfComplete(
            ObjectAnimationSequence sequence,
            long gameTime
    ) {
        if (!active(
                sequence
        )) {
            return false;
        }

        if (!sequence.finished(
                elapsed(
                        gameTime,
                        0.0F
                )
        )) {
            return false;
        }

        stop();

        return true;
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putBoolean(
                ACTIVE_KEY,
                active
        );

        tag.putLong(
                START_KEY,
                startTick
        );

        if (sequenceId != null) {
            tag.putString(
                    SEQUENCE_KEY,
                    sequenceId.toString()
            );
        }

        return tag;
    }

    public void load(
            CompoundTag tag
    ) {
        active =
                tag.getBoolean(
                        ACTIVE_KEY
                );

        startTick =
                tag.getLong(
                        START_KEY
                );

        sequenceId =
                ResourceLocation.tryParse(
                        tag.getString(
                                SEQUENCE_KEY
                        )
                );

        if (sequenceId == null) {
            active =
                    false;
        }
    }
}
