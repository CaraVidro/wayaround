package net.caravidro.wayaround.industrial.assembly;

import java.util.Optional;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Converts localized graph stress plus load character into a generic failure.
 *
 * This class deliberately knows nothing about furnaces, dams, saws or turbines.
 * It only answers: which constraint is currently failing, how, and with what
 * release impulse.
 */
public final class AssemblyFailureModel {

    private AssemblyFailureModel() {
    }

    public static Optional<AssemblyFailureEvent> evaluate(
            AssemblyGraph graph,
            AssemblyLoadCase loadCase
    ) {
        if (graph == null
                || loadCase == null
                || loadCase.magnitude() <= 0.0F) {
            return Optional.empty();
        }

        AssemblyLoadDistribution distribution =
                graph.solve(
                        loadCase.magnitude()
                );

        boolean connectionTarget =
                distribution.maxConnectionStress()
                        >= distribution.maxPartStress()
                                * 0.92F
                        && !distribution.hottestConnection()
                                .isBlank();

        if (connectionTarget) {
            return evaluateConnection(
                    graph,
                    distribution,
                    loadCase
            );
        }

        return evaluatePart(
                graph,
                distribution,
                loadCase
        );
    }

    private static Optional<AssemblyFailureEvent> evaluatePart(
            AssemblyGraph graph,
            AssemblyLoadDistribution distribution,
            AssemblyLoadCase loadCase
    ) {
        String id =
                distribution.hottestPart();

        if (id.isBlank()) {
            return Optional.empty();
        }

        AssemblyPartNode part =
                graph.part(
                        id
                );

        if (part == null) {
            return Optional.empty();
        }

        AssemblyPartProfile profile =
                part.profile();

        float rawStress =
                distribution.partStress(
                        id
                );

        float sensitivity =
                partSensitivity(
                        profile,
                        loadCase.kind()
                );

        float effectiveStress =
                rawStress
                        * sensitivity;

        float threshold =
                partThreshold(
                        profile,
                        loadCase
                );

        if (effectiveStress
                <= threshold) {
            return Optional.empty();
        }

        float severity =
                severity(
                        effectiveStress,
                        threshold
                );

        AssemblyFailureMode mode =
                partMode(
                        profile,
                        loadCase,
                        severity
                );

        return Optional.of(
                new AssemblyFailureEvent(
                        AssemblyFailureEvent.Target.PART,
                        id,
                        id,
                        "",
                        mode,
                        loadCase.kind(),
                        effectiveStress,
                        threshold,
                        severity,
                        releaseImpulse(
                                loadCase,
                                severity,
                                mode
                        )
                )
        );
    }

    private static Optional<AssemblyFailureEvent> evaluateConnection(
            AssemblyGraph graph,
            AssemblyLoadDistribution distribution,
            AssemblyLoadCase loadCase
    ) {
        String id =
                distribution.hottestConnection();

        if (id.isBlank()) {
            return Optional.empty();
        }

        AssemblyConnection connection =
                graph.connection(
                        id
                );

        if (connection == null) {
            return Optional.empty();
        }

        float rawStress =
                distribution.connectionStress(
                        id
                );

        float sensitivity =
                connectionSensitivity(
                        connection,
                        loadCase.kind()
                );

        float effectiveStress =
                rawStress
                        * sensitivity;

        float threshold =
                connectionThreshold(
                        connection,
                        loadCase
                );

        if (effectiveStress
                <= threshold) {
            return Optional.empty();
        }

        float severity =
                severity(
                        effectiveStress,
                        threshold
                );

        AssemblyFailureMode mode =
                connectionMode(
                        connection,
                        loadCase,
                        severity
                );

        return Optional.of(
                new AssemblyFailureEvent(
                        AssemblyFailureEvent.Target.CONNECTION,
                        id,
                        connection.first(),
                        connection.second(),
                        mode,
                        loadCase.kind(),
                        effectiveStress,
                        threshold,
                        severity,
                        releaseImpulse(
                                loadCase,
                                severity,
                                mode
                        )
                )
        );
    }

    private static float partThreshold(
            AssemblyPartProfile profile,
            AssemblyLoadCase loadCase
    ) {
        float threshold =
                0.82F
                        + profile.durabilityScore()
                                * 0.34F
                        + profile.assemblyScore()
                                * 0.16F;

        threshold -=
                loadCase.cyclicity()
                        * (
                        0.08F
                                + profile.fatigue()
                                        * 0.28F
                );

        return Mth.clamp(
                threshold,
                0.52F,
                1.38F
        );
    }

    private static float connectionThreshold(
            AssemblyConnection connection,
            AssemblyLoadCase loadCase
    ) {
        float threshold =
                0.72F
                        + connection.condition()
                                * 0.40F;

        float cyclicPenalty =
                loadCase.cyclicity()
                        * switch (connection.type()) {
                    case FASTENED,
                         BELT,
                         BEARING ->
                            0.16F;

                    default ->
                            0.08F;
                };

        threshold -=
                cyclicPenalty;

        return Mth.clamp(
                threshold,
                0.46F,
                1.20F
        );
    }

    private static float partSensitivity(
            AssemblyPartProfile profile,
            AssemblyLoadCase.Kind kind
    ) {
        float factor =
                1.0F;

        switch (profile.kind()) {
            case SHAFT ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.TORSION
                                    || kind == AssemblyLoadCase.Kind.MECHANICAL
                                    ? 1.22F
                                    : 0.96F;

            case FRAME ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.COMPRESSION
                                    || kind == AssemblyLoadCase.Kind.BENDING
                                    || kind == AssemblyLoadCase.Kind.PRESSURE
                                    ? 1.14F
                                    : 1.0F;

            case BOARD,
                 BLADE ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.BENDING
                                    || kind == AssemblyLoadCase.Kind.IMPACT
                                    || kind == AssemblyLoadCase.Kind.HYDRAULIC
                                    ? 1.20F
                                    : 1.0F;

            case FASTENER,
                 BINDING ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.TENSION
                                    || kind == AssemblyLoadCase.Kind.SHEAR
                                    ? 1.26F
                                    : 1.0F;

            default -> {
            }
        }

        switch (profile.material()) {
            case WOOD ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.BENDING
                                    || kind == AssemblyLoadCase.Kind.TENSION
                                    ? 1.18F
                                    : 1.0F;

            case FIBER ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.TENSION
                                    || kind == AssemblyLoadCase.Kind.SHEAR
                                    ? 1.25F
                                    : 0.94F;

            case STONE,
                 DIAMOND ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.IMPACT
                                    || kind == AssemblyLoadCase.Kind.BENDING
                                    ? 1.12F
                                    : 0.94F;

            case COPPER,
                 BRONZE ->
                    factor *=
                            kind == AssemblyLoadCase.Kind.BENDING
                                    || kind == AssemblyLoadCase.Kind.PRESSURE
                                    ? 1.06F
                                    : 0.98F;

            case IRON,
                 STEEL -> {
            }
        }

        return factor;
    }

    private static float connectionSensitivity(
            AssemblyConnection connection,
            AssemblyLoadCase.Kind kind
    ) {
        return switch (connection.type()) {
            case FASTENED ->
                    kind == AssemblyLoadCase.Kind.TENSION
                            || kind == AssemblyLoadCase.Kind.PRESSURE
                            || kind == AssemblyLoadCase.Kind.HYDRAULIC
                            ? 1.34F
                            : kind == AssemblyLoadCase.Kind.SHEAR
                            || kind == AssemblyLoadCase.Kind.IMPACT
                            ? 1.20F
                            : 1.0F;

            case BEARING ->
                    kind == AssemblyLoadCase.Kind.TORSION
                            || kind == AssemblyLoadCase.Kind.MECHANICAL
                            || kind == AssemblyLoadCase.Kind.VIBRATION
                            ? 1.22F
                            : 0.96F;

            case SHAFT ->
                    kind == AssemblyLoadCase.Kind.TORSION
                            || kind == AssemblyLoadCase.Kind.MECHANICAL
                            ? 1.26F
                            : 1.0F;

            case GEAR ->
                    kind == AssemblyLoadCase.Kind.TORSION
                            || kind == AssemblyLoadCase.Kind.MECHANICAL
                            ? 1.20F
                            : 0.98F;

            case BELT ->
                    kind == AssemblyLoadCase.Kind.TENSION
                            || kind == AssemblyLoadCase.Kind.MECHANICAL
                            ? 1.30F
                            : 1.0F;

            case SUPPORT ->
                    kind == AssemblyLoadCase.Kind.COMPRESSION
                            || kind == AssemblyLoadCase.Kind.TENSION
                            || kind == AssemblyLoadCase.Kind.PRESSURE
                            ? 1.20F
                            : 1.0F;

            case CONTACT ->
                    kind == AssemblyLoadCase.Kind.SHEAR
                            || kind == AssemblyLoadCase.Kind.VIBRATION
                            ? 1.24F
                            : 1.0F;
        };
    }

    private static AssemblyFailureMode partMode(
            AssemblyPartProfile profile,
            AssemblyLoadCase loadCase,
            float severity
    ) {
        if (profile.kind()
                == AssemblyPartProfile.Kind.SHAFT) {
            if (loadCase.kind()
                    == AssemblyLoadCase.Kind.TORSION
                    || loadCase.kind()
                    == AssemblyLoadCase.Kind.MECHANICAL) {
                return severity > 1.05F
                        ? AssemblyFailureMode.SNAP
                        : AssemblyFailureMode.TWIST;
            }
        }

        if (profile.kind()
                == AssemblyPartProfile.Kind.BINDING
                || profile.material()
                == AssemblyPartProfile.Material.FIBER) {
            return AssemblyFailureMode.TEAR;
        }

        return switch (profile.material()) {
            case WOOD ->
                    switch (loadCase.kind()) {
                        case COMPRESSION ->
                                AssemblyFailureMode.BUCKLE;

                        case SHEAR ->
                                AssemblyFailureMode.SHEAR;

                        case TORSION ->
                                severity > 0.85F
                                        ? AssemblyFailureMode.SNAP
                                        : AssemblyFailureMode.TWIST;

                        default ->
                                AssemblyFailureMode.SPLIT;
                    };

            case STONE,
                 DIAMOND ->
                    AssemblyFailureMode.FRACTURE;

            case COPPER,
                 BRONZE ->
                    switch (loadCase.kind()) {
                        case PRESSURE,
                             HYDRAULIC ->
                                severity > 1.15F
                                        ? AssemblyFailureMode.RUPTURE
                                        : AssemblyFailureMode.BEND;

                        case TORSION ->
                                AssemblyFailureMode.TWIST;

                        default ->
                                AssemblyFailureMode.BEND;
                    };

            case IRON,
                 STEEL ->
                    switch (loadCase.kind()) {
                        case COMPRESSION ->
                                AssemblyFailureMode.BUCKLE;

                        case PRESSURE,
                             HYDRAULIC ->
                                severity > 1.35F
                                        ? AssemblyFailureMode.RUPTURE
                                        : AssemblyFailureMode.BEND;

                        case TORSION ->
                                severity > 1.20F
                                        ? AssemblyFailureMode.SNAP
                                        : AssemblyFailureMode.TWIST;

                        case IMPACT ->
                                profile.fatigue() > 0.72F
                                        ? AssemblyFailureMode.FRACTURE
                                        : AssemblyFailureMode.BEND;

                        default ->
                                AssemblyFailureMode.BEND;
                    };

            case FIBER ->
                    AssemblyFailureMode.TEAR;
        };
    }

    private static AssemblyFailureMode connectionMode(
            AssemblyConnection connection,
            AssemblyLoadCase loadCase,
            float severity
    ) {
        return switch (connection.type()) {
            case FASTENED ->
                    switch (loadCase.kind()) {
                        case PRESSURE,
                             HYDRAULIC,
                             TENSION ->
                                AssemblyFailureMode.PULL_OUT;

                        case VIBRATION ->
                                severity > 0.80F
                                        ? AssemblyFailureMode.DETACH
                                        : AssemblyFailureMode.SLIP;

                        default ->
                                AssemblyFailureMode.SHEAR;
                    };

            case BEARING ->
                    AssemblyFailureMode.SEIZE;

            case SHAFT ->
                    severity > 1.0F
                            ? AssemblyFailureMode.SNAP
                            : AssemblyFailureMode.TWIST;

            case GEAR ->
                    AssemblyFailureMode.SHEAR;

            case BELT ->
                    severity > 0.70F
                            ? AssemblyFailureMode.TEAR
                            : AssemblyFailureMode.SLIP;

            case SUPPORT ->
                    switch (loadCase.kind()) {
                        case COMPRESSION ->
                                AssemblyFailureMode.BUCKLE;

                        case PRESSURE,
                             HYDRAULIC,
                             TENSION ->
                                AssemblyFailureMode.PULL_OUT;

                        default ->
                                AssemblyFailureMode.SHEAR;
                    };

            case CONTACT ->
                    severity > 0.75F
                            ? AssemblyFailureMode.DETACH
                            : AssemblyFailureMode.SLIP;
        };
    }

    private static float severity(
            float stress,
            float threshold
    ) {
        return Mth.clamp(
                (
                        stress
                                - threshold
                )
                        / Math.max(
                        0.10F,
                        threshold
                ),
                0.0F,
                4.0F
        );
    }

    private static Vec3 releaseImpulse(
            AssemblyLoadCase loadCase,
            float severity,
            AssemblyFailureMode mode
    ) {
        if (loadCase.direction()
                .lengthSqr() < 1.0E-8
                || mode == AssemblyFailureMode.SEIZE) {
            return Vec3.ZERO;
        }

        double releaseFactor =
                switch (mode) {
                    case PULL_OUT,
                         DETACH,
                         RUPTURE ->
                            1.0;

                    case SHEAR,
                         SNAP,
                         FRACTURE,
                         SPLIT ->
                            0.72;

                    case TEAR,
                         SLIP ->
                            0.48;

                    case BEND,
                         BUCKLE,
                         TWIST ->
                            0.26;

                    case SEIZE ->
                            0.0;
                };

        double speed =
                Mth.clamp(
                        0.10
                                + loadCase.magnitude()
                                        * 0.075
                                + severity
                                        * 0.42,
                        0.0,
                        4.5
                )
                        * releaseFactor;

        return loadCase.direction()
                .scale(
                        speed
                );
    }
}
