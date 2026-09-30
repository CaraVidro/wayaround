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
        blockOutputsRemainIndependent();
        attributeFallsBackToLocalValue();
        materialConversionRoundsUp();
        whatIfChainPredictsMotion();
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

    private static void blockOutputsRemainIndependent() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node block =
                graph.add(
                        NodeType.BLOCK_PROPERTY,
                        0,
                        0,
                        "Block",
                        9.0,
                        "kg"
                );

        block.output(
                0,
                9.0,
                "M",
                "kg"
        );

        block.output(
                1,
                2.0,
                "W",
                "m"
        );

        block.output(
                2,
                3.0,
                "H",
                "m"
        );

        block.output(
                3,
                4.0,
                "D",
                "m"
        );

        Node area =
                graph.add(
                        NodeType.RECTANGLE_AREA,
                        0,
                        0
                );

        require(
                graph.connect(
                        block.id(),
                        1,
                        area.id(),
                        0
                ),
                "Width output should connect"
        );

        require(
                graph.connect(
                        block.id(),
                        2,
                        area.id(),
                        1
                ),
                "Height output should connect"
        );

        requireNear(
                6.0,
                area.result(),
                "W and H outputs must remain independent"
        );
    }

    private static void attributeFallsBackToLocalValue() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node attribute =
                graph.add(
                        NodeType.ATTRIBUTE,
                        0,
                        0,
                        "Force",
                        12.0,
                        "N"
                );

        graph.recalculate();

        requireNear(
                12.0,
                attribute.result(),
                "Attribute must use local value without an input"
        );

        Node external =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "External",
                        5.0,
                        "N"
                );

        graph.connect(
                external.id(),
                attribute.id(),
                0
        );

        requireNear(
                5.0,
                attribute.result(),
                "Connected value must override the attribute fallback"
        );
    }

    private static void materialConversionRoundsUp() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node area =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "Area",
                        2.4,
                        "m²"
                );

        Node material =
                graph.add(
                        NodeType.MATERIAL_CONVERT,
                        0,
                        0,
                        "To planks",
                        1.0,
                        "blocks"
                );

        graph.connect(
                area.id(),
                material.id(),
                0
        );

        requireNear(
                3.0,
                material.result(),
                "Material conversion must round required block count upward"
        );
    }

    private static void whatIfChainPredictsMotion() {
        EngineeringCalculationGraph graph =
                new EngineeringCalculationGraph();

        Node speed =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "Wind speed",
                        10.0,
                        "m/s"
                );

        Node area =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "Area",
                        2.0,
                        "m²"
                );

        Node wind =
                graph.add(
                        NodeType.WIND_FORCE,
                        0,
                        0
                );

        graph.connect(
                speed.id(),
                wind.id(),
                0
        );

        graph.connect(
                area.id(),
                wind.id(),
                1
        );

        requireNear(
                147.0,
                wind.result(),
                "Wind force reference calculation must match"
        );

        Node mass =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "Mass",
                        49.0,
                        "kg"
                );

        Node acceleration =
                graph.add(
                        NodeType.ACCELERATION,
                        0,
                        0
                );

        graph.connect(
                wind.id(),
                acceleration.id(),
                0
        );

        graph.connect(
                mass.id(),
                acceleration.id(),
                1
        );

        requireNear(
                3.0,
                acceleration.result(),
                "Force divided by mass must produce acceleration"
        );

        Node time =
                graph.add(
                        NodeType.CONSTANT,
                        0,
                        0,
                        "Time",
                        2.0,
                        "s"
                );

        Node displacement =
                graph.add(
                        NodeType.DISPLACEMENT,
                        0,
                        0
                );

        graph.connect(
                acceleration.id(),
                displacement.id(),
                0
        );

        graph.connect(
                time.id(),
                displacement.id(),
                1
        );

        requireNear(
                6.0,
                displacement.result(),
                "½at² must project displacement over time"
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
