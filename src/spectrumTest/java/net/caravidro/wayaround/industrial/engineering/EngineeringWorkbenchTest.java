package net.caravidro.wayaround.industrial.engineering;

import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.Node;
import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.NodeType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

public final class EngineeringWorkbenchTest {

    public static void main(
            String[] args
    ) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        graphRecalculatesThroughConnections();
        cyclesAreRejected();
        beamFormulaMatchesReference();
        materialProfilesSeparateWoodAndMetal();

        System.out.println(
                "Engineering workbench regression tests passed"
        );
    }

    private static void graphRecalculatesThroughConnections() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node fiftyA =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "50",
                        50.0,
                        "m"
                );

        Node fiftyB =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "50",
                        50.0,
                        "m"
                );

        Node add =
                graph.add(
                        NodeType.ADD,
                        0,
                        0
                );

        require(
                graph.connect(
                        fiftyA.id(),
                        add.id(),
                        0
                ),
                "First connection should succeed"
        );

        require(
                graph.connect(
                        fiftyB.id(),
                        add.id(),
                        1
                ),
                "Second connection should succeed"
        );

        requireNear(
                100.0,
                add.result(),
                "50 + 50 must recalculate to 100"
        );

        fiftyA.literal(
                75.0
        );

        graph.recalculate();

        requireNear(
                125.0,
                add.result(),
                "Changing a source must propagate"
        );
    }

    private static void cyclesAreRejected() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node a =
                graph.add(
                        NodeType.ADD,
                        0,
                        0
                );

        Node b =
                graph.add(
                        NodeType.ADD,
                        0,
                        0
                );

        require(
                graph.connect(
                        a.id(),
                        b.id(),
                        0
                ),
                "Initial acyclic connection should succeed"
        );

        require(
                !graph.connect(
                        b.id(),
                        a.id(),
                        0
                ),
                "Graph must reject cyclic engineering dependencies"
        );
    }

    private static void beamFormulaMatchesReference() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node load =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "q",
                        10.0,
                        "kN/m"
                );

        Node length =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "L",
                        4.0,
                        "m"
                );

        Node moment =
                graph.add(
                        NodeType.BEAM_UDL_MOMENT,
                        0,
                        0
                );

        graph.connect(
                load.id(),
                moment.id(),
                0
        );

        graph.connect(
                length.id(),
                moment.id(),
                1
        );

        requireNear(
                20.0,
                moment.result(),
                "qL²/8 reference value must match"
        );
    }

    private static void materialProfilesSeparateWoodAndMetal() {
        EngineeringBlockProfile wood =
                EngineeringBlockProfile.inspect(
                        Blocks.OAK_PLANKS
                );

        EngineeringBlockProfile iron =
                EngineeringBlockProfile.inspect(
                        Blocks.IRON_BLOCK
                );

        require(
                iron.massKg()
                        > wood.massKg(),
                "Iron should be estimated heavier than wood for equal volume"
        );

        require(
                iron.compressiveStrengthMpa()
                        > wood.compressiveStrengthMpa(),
                "Iron should have greater compression estimate than wood"
        );

        EngineeringBlockProfile.Property woodFire =
                wood.property(
                        "flammability"
                );

        require(
                woodFire != null
                        && woodFire.numericValue()
                                > 0.0,
                "Wood should expose flammability"
        );

        require(
                iron.property(
                        "flammability"
                ) == null,
                "Iron should not expose flammability"
        );
    }

    private static void requireNear(
            double expected,
            double actual,
            String message
    ) {
        if (Math.abs(
                expected
                        - actual
        ) > 0.0001) {
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
