package net.caravidro.wayaround.industrial.assembly;

import java.util.Collection;

import net.minecraft.util.Mth;

/**
 * Generic V1 evaluation engine for assembled machines.
 */
public final class AssemblyEngine {

    private AssemblyEngine() {
    }

    public static AssemblySnapshot inspect(
            AssemblyMachine machine
    ) {
        Collection<AssemblyPartNode> parts =
                machine.assemblyParts();

        Collection<AssemblyConnection> connections =
                machine.assemblyConnections();

        if (parts.isEmpty()) {
            return new AssemblySnapshot(
                    machine.assemblyType(),
                    0,
                    connections.size(),
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    Math.max(
                            0.0F,
                            machine.currentAssemblyLoad()
                    ),
                    Float.POSITIVE_INFINITY,
                    "",
                    false,
                    true
            );
        }

        float workmanshipSum =
                0.0F;

        float integrityWeighted =
                0.0F;

        float weightTotal =
                0.0F;

        int supported =
                0;

        String weakest =
                "";

        float weakestScore =
                Float.MAX_VALUE;

        for (AssemblyPartNode part :
                parts) {
            AssemblyPartProfile profile =
                    part.profile();

            float weight =
                    Math.max(
                            0.05F,
                            part.loadShare()
                    );

            float score =
                    profile.assemblyScore();

            workmanshipSum +=
                    score;

            integrityWeighted +=
                    profile.durabilityScore()
                            * weight;

            weightTotal +=
                    weight;

            if (part.supported()) {
                supported++;
            }

            if (profile.durabilityScore()
                    < weakestScore) {
                weakestScore =
                        profile.durabilityScore();

                weakest =
                        part.id();
            }
        }

        float connectionCondition =
                1.0F;

        if (!connections.isEmpty()) {
            float sum =
                    0.0F;

            for (AssemblyConnection connection :
                    connections) {
                float condition =
                        connection.condition();

                sum +=
                        condition;

                if (condition < weakestScore) {
                    weakestScore =
                            condition;

                    weakest =
                            connection.first()
                                    + "<->"
                                    + connection.second();
                }
            }

            connectionCondition =
                    sum
                            / connections.size();
        }

        float workmanship =
                workmanshipSum
                        / parts.size();

        float partIntegrity =
                weightTotal <= 0.0F
                        ? 0.0F
                        : integrityWeighted
                                / weightTotal;

        float supportRatio =
                supported
                        / (float) parts.size();

        float structuralIntegrity =
                Mth.clamp(
                        partIntegrity * 0.68F
                                + connectionCondition * 0.24F
                                + supportRatio * 0.08F,
                        0.0F,
                        1.0F
                );

        float loadCapacity =
                Math.max(
                        0.05F,
                        structuralIntegrity
                                * (
                                0.70F
                                        + workmanship
                                                * 0.60F
                        )
                                * Math.max(
                                1.0F,
                                parts.size()
                                        * 0.55F
                        )
                );

        float load =
                Math.max(
                        0.0F,
                        machine.currentAssemblyLoad()
                );

        float stressRatio =
                load
                        / loadCapacity;

        boolean valid =
                supportRatio >= 0.50F
                        && structuralIntegrity > 0.08F;

        boolean critical =
                !valid
                        || structuralIntegrity < 0.18F
                        || stressRatio > 1.15F;

        return new AssemblySnapshot(
                machine.assemblyType(),
                parts.size(),
                connections.size(),
                workmanship,
                structuralIntegrity,
                supportRatio,
                loadCapacity,
                load,
                stressRatio,
                weakest,
                valid,
                critical
        );
    }

    public static float externalWearFraction(
            AssemblyMachine machine,
            float normalizedImpulse
    ) {
        AssemblySnapshot snapshot =
                inspect(
                        machine
                );

        float impulse =
                Math.max(
                        0.0F,
                        normalizedImpulse
                );

        float vulnerability =
                1.15F
                        - snapshot.structuralIntegrity()
                                * 0.75F;

        float overload =
                Math.max(
                        0.0F,
                        snapshot.stressRatio()
                                - 0.75F
                );

        return Mth.clamp(
                impulse
                        * 0.018F
                        * vulnerability
                        * (
                        1.0F
                                + overload
                                        * 0.65F
                ),
                0.0F,
                0.35F
        );
    }
}
