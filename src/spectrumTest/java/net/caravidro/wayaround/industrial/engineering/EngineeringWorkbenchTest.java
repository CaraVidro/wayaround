package net.caravidro.wayaround.industrial.engineering;

import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.Node;
import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.NodeType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class EngineeringWorkbenchTest {

    public static void main(
            String[] args
    ) {
        graphRecalculatesThroughConnections();
        cyclesAreRejected();
        beamFormulaMatchesReference();
        blueprintProjectIsBounded();

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

    private static void blueprintProjectIsBounded() {
        CompoundTag project =
                new CompoundTag();

        ListTag nodes =
                new ListTag();

        for (int index =
                     0;
             index < 300;
             index++) {

            CompoundTag node =
                    new CompoundTag();

            node.putInt(
                    "Id",
                    index
            );

            nodes.add(
                    node
            );
        }

        project.put(
                "Nodes",
                nodes
        );

        project.putDouble(
                "Zoom",
                99.0
        );

        project.putDouble(
                "PanX",
                999999.0
        );

        CompoundTag sanitized =
                EngineeringBlueprintData.sanitizeProject(
                        project
                );

        require(
                sanitized.getList(
                        "Nodes",
                        Tag.TAG_COMPOUND
                ).size()
                        == 256,
                "Blueprint sanitization must cap node count"
        );

        requireNear(
                2.40,
                sanitized.getDouble(
                        "Zoom"
                ),
                "Blueprint zoom must be bounded"
        );

        requireNear(
                100000.0,
                sanitized.getDouble(
                        "PanX"
                ),
                "Blueprint pan must be bounded"
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
