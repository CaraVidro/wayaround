package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.world.phys.Vec3;

/**
 * Result of a physical constraint failing.
 *
 * The impulse is the release impulse available to a component that becomes
 * free. It is not an instruction to spawn debris; the owning machine decides
 * what a detached/split/ruptured component actually is.
 */
public record AssemblyFailureEvent(
        Target target,
        String targetId,
        String firstPart,
        String secondPart,
        AssemblyFailureMode mode,
        AssemblyLoadCase.Kind loadKind,
        float localStress,
        float threshold,
        float severity,
        Vec3 releaseImpulse
) {

    public enum Target {
        PART,
        CONNECTION
    }

    public AssemblyFailureEvent {
        targetId =
                targetId == null
                        ? ""
                        : targetId;

        firstPart =
                firstPart == null
                        ? ""
                        : firstPart;

        secondPart =
                secondPart == null
                        ? ""
                        : secondPart;

        releaseImpulse =
                releaseImpulse == null
                        ? Vec3.ZERO
                        : releaseImpulse;
    }
}
