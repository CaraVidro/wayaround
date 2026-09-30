package net.caravidro.wayaround.industrial.engineering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic engineering node graph.
 *
 * Nodes may expose more than one output. This matters for physical objects:
 * a single block node can expose its selected engineering property plus width,
 * height and depth without creating fake intermediary nodes.
 */
public final class EngineeringCalculationGraph {

    public enum NodeType {
        CONSTANT("Number", 0, 1),
        BLOCK_PROPERTY("Block / properties", 0, 4),
        ATTRIBUTE("Attribute", 1, 1),
        NOTE("Note", 0, 0),
        MATERIAL_CONVERT("Material convert", 1, 1),

        ADD("Add", 2, 1),
        SUBTRACT("Subtract", 2, 1),
        MULTIPLY("Multiply", 2, 1),
        DIVIDE("Divide", 2, 1),
        PYTHAGORAS("Distance / Pythagoras", 2, 1),
        RECTANGLE_AREA("Rectangle area", 2, 1),
        BOX_VOLUME("Box volume", 3, 1),
        CIRCLE_AREA("Circle area", 1, 1),
        SLOPE_DEGREES("Slope angle", 2, 1),
        SAFETY_FACTOR("Safety factor", 2, 1),
        STRESS("Stress F/A", 2, 1),
        BEAM_UDL_MOMENT("Beam moment qL²/8", 2, 1),
        BEAM_CENTER_MOMENT("Beam moment PL/4", 2, 1),

        WIND_FORCE("Wind force", 2, 1),
        WATER_FORCE("Water force", 2, 1),
        GRAVITY_LOAD("Gravity load", 1, 1),
        ACCELERATION("Acceleration F/m", 2, 1),
        TIME_STEP("Rate × time", 2, 1),
        DISPLACEMENT("Displacement ½at²", 2, 1);

        private final String label;
        private final int inputs;
        private final int outputs;

        NodeType(
                String label,
                int inputs,
                int outputs
        ) {
            this.label = label;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        public String label() {
            return label;
        }

        public int inputs() {
            return inputs;
        }

        public int outputs() {
            return outputs;
        }
    }

    public static final class Node {

        private static final int MAX_INPUTS = 3;
        private static final int MAX_OUTPUTS = 4;

        private final int id;
        private final NodeType type;

        private final int[] inputs =
                new int[MAX_INPUTS];

        private final int[] inputOutputs =
                new int[MAX_INPUTS];

        private final double[] outputValues =
                new double[MAX_OUTPUTS];

        private final String[] outputLabels =
                new String[MAX_OUTPUTS];

        private final String[] outputUnits =
                new String[MAX_OUTPUTS];

        private int x;
        private int y;

        private String label;
        private String unit;
        private String parameter = "";

        private double literal;
        private double result;

        private Node(
                int id,
                NodeType type,
                int x,
                int y,
                String label,
                double literal,
                String unit
        ) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.label =
                    label == null
                            ? type.label()
                            : label;
            this.literal =
                    Double.isFinite(literal)
                            ? literal
                            : 0.0;
            this.unit =
                    unit == null
                            ? ""
                            : unit;

            Arrays.fill(
                    inputs,
                    -1
            );

            Arrays.fill(
                    inputOutputs,
                    0
            );

            Arrays.fill(
                    outputLabels,
                    ""
            );

            Arrays.fill(
                    outputUnits,
                    ""
            );

            outputValues[0] = this.literal;
            outputLabels[0] = "out";
            outputUnits[0] = this.unit;
            result = this.literal;
        }

        public int id() {
            return id;
        }

        public NodeType type() {
            return type;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public void move(
                int x,
                int y
        ) {
            this.x = x;
            this.y = y;
        }

        public String label() {
            return label;
        }

        public void label(
                String label
        ) {
            this.label =
                    label == null
                            ? type.label()
                            : label;
        }

        public String unit() {
            return unit;
        }

        public void unit(
                String unit
        ) {
            this.unit =
                    unit == null
                            ? ""
                            : unit;

            outputUnits[0] =
                    this.unit;
        }

        public String parameter() {
            return parameter;
        }

        public void parameter(
                String parameter
        ) {
            this.parameter =
                    parameter == null
                            ? ""
                            : parameter;
        }

        public double literal() {
            return literal;
        }

        public void literal(
                double literal
        ) {
            this.literal =
                    Double.isFinite(literal)
                            ? literal
                            : 0.0;

            outputValues[0] =
                    this.literal;
        }

        public double result() {
            return result;
        }

        public int input(
                int slot
        ) {
            if (slot < 0
                    || slot >= inputs.length) {
                return -1;
            }

            return inputs[slot];
        }

        public int inputOutput(
                int slot
        ) {
            if (slot < 0
                    || slot >= inputOutputs.length) {
                return 0;
            }

            return inputOutputs[slot];
        }

        public int inputCount() {
            return type.inputs();
        }

        public int outputCount() {
            return type.outputs();
        }

        public void output(
                int slot,
                double value,
                String label,
                String unit
        ) {
            if (slot < 0
                    || slot >= outputValues.length
                    || slot >= outputCount()) {
                return;
            }

            double safeValue =
                    Double.isFinite(value)
                            ? value
                            : 0.0;

            outputValues[slot] =
                    safeValue;

            outputLabels[slot] =
                    label == null
                            ? ""
                            : label;

            outputUnits[slot] =
                    unit == null
                            ? ""
                            : unit;

            if (slot == 0) {
                this.unit =
                        outputUnits[slot];

                if (type == NodeType.CONSTANT
                        || type == NodeType.BLOCK_PROPERTY
                        || type == NodeType.ATTRIBUTE) {
                    literal =
                            safeValue;
                }

                result =
                        safeValue;
            }
        }

        public double outputValue(
                int slot
        ) {
            if (slot < 0
                    || slot >= outputCount()) {
                return Double.NaN;
            }

            if (slot == 0
                    && type != NodeType.BLOCK_PROPERTY) {
                return result;
            }

            return outputValues[slot];
        }

        public String outputLabel(
                int slot
        ) {
            if (slot < 0
                    || slot >= outputCount()) {
                return "";
            }

            return outputLabels[slot];
        }

        public String outputUnit(
                int slot
        ) {
            if (slot < 0
                    || slot >= outputCount()) {
                return "";
            }

            return outputUnits[slot];
        }

        private void input(
                int slot,
                int nodeId,
                int sourceOutput
        ) {
            inputs[slot] =
                    nodeId;

            inputOutputs[slot] =
                    Math.max(
                            0,
                            sourceOutput
                    );
        }
    }

    private final List<Node> nodes =
            new ArrayList<>();

    private int nextId =
            1;

    public Node add(
            NodeType type,
            int x,
            int y
    ) {
        return add(
                type,
                x,
                y,
                type.label(),
                type == NodeType.CONSTANT
                        ? 1.0
                        : 0.0,
                ""
        );
    }

    public Node add(
            NodeType type,
            int x,
            int y,
            String label,
            double literal,
            String unit
    ) {
        Node node =
                new Node(
                        nextId++,
                        type,
                        x,
                        y,
                        label,
                        literal,
                        unit
                );

        nodes.add(
                node
        );

        recalculate();

        return node;
    }

    public List<Node> nodes() {
        return List.copyOf(
                nodes
        );
    }

    public void clear() {
        nodes.clear();
        nextId =
                1;
    }

    public Node node(
            int id
    ) {
        for (Node node :
                nodes) {
            if (node.id()
                    == id) {
                return node;
            }
        }

        return null;
    }

    public boolean connect(
            int fromId,
            int toId,
            int inputSlot
    ) {
        return connect(
                fromId,
                0,
                toId,
                inputSlot
        );
    }

    public boolean connect(
            int fromId,
            int fromOutput,
            int toId,
            int inputSlot
    ) {
        Node from =
                node(
                        fromId
                );

        Node to =
                node(
                        toId
                );

        if (from == null
                || to == null
                || from == to
                || fromOutput < 0
                || fromOutput >= from.outputCount()
                || inputSlot < 0
                || inputSlot >= to.inputCount()) {
            return false;
        }

        int oldId =
                to.input(
                        inputSlot
                );

        int oldOutput =
                to.inputOutput(
                        inputSlot
                );

        to.input(
                inputSlot,
                fromId,
                fromOutput
        );

        if (hasCycle()) {
            to.input(
                    inputSlot,
                    oldId,
                    oldOutput
            );

            return false;
        }

        recalculate();

        return true;
    }

    public void disconnect(
            int toId,
            int inputSlot
    ) {
        Node to =
                node(
                        toId
                );

        if (to == null
                || inputSlot < 0
                || inputSlot >= to.inputCount()) {
            return;
        }

        to.input(
                inputSlot,
                -1,
                0
        );

        recalculate();
    }

    public void remove(
            int nodeId
    ) {
        nodes.removeIf(
                node -> node.id()
                        == nodeId
        );

        for (Node node :
                nodes) {
            for (int slot = 0;
                 slot < node.inputCount();
                 slot++) {

                if (node.input(slot)
                        == nodeId) {
                    node.input(
                            slot,
                            -1,
                            0
                    );
                }
            }
        }

        recalculate();
    }

    public void recalculate() {
        for (Node node :
                nodes) {
            node.result =
                    evaluateOutput(
                            node,
                            0,
                            new HashSet<>()
                    );
        }
    }

    public double outputValue(
            int nodeId,
            int outputSlot
    ) {
        Node node =
                node(
                        nodeId
                );

        if (node == null) {
            return Double.NaN;
        }

        return evaluateOutput(
                node,
                outputSlot,
                new HashSet<>()
        );
    }

    private double evaluateOutput(
            Node node,
            int outputSlot,
            Set<Integer> visiting
    ) {
        if (outputSlot < 0
                || outputSlot >= node.outputCount()) {
            return Double.NaN;
        }

        if (node.type()
                == NodeType.BLOCK_PROPERTY) {
            return node.outputValue(
                    outputSlot
            );
        }

        if (outputSlot != 0) {
            return node.outputValue(
                    outputSlot
            );
        }

        return evaluate(
                node,
                visiting
        );
    }

    private double evaluate(
            Node node,
            Set<Integer> visiting
    ) {
        if (!visiting.add(
                node.id()
        )) {
            return Double.NaN;
        }

        double a =
                inputValue(
                        node,
                        0,
                        visiting
                );

        double b =
                inputValue(
                        node,
                        1,
                        visiting
                );

        double c =
                inputValue(
                        node,
                        2,
                        visiting
                );

        double result =
                switch (node.type()) {
                    case CONSTANT, BLOCK_PROPERTY ->
                            node.literal();

                    case ATTRIBUTE ->
                            node.input(0) >= 0
                                    ? a
                                    : node.literal();

                    case NOTE ->
                            0.0;

                    case MATERIAL_CONVERT ->
                            Math.abs(node.literal()) < 0.0000001
                                    ? Double.NaN
                                    : Math.ceil(
                                    Math.abs(a)
                                            / Math.abs(
                                            node.literal()
                                    )
                            );

                    case ADD ->
                            a + b;

                    case SUBTRACT ->
                            a - b;

                    case MULTIPLY ->
                            a * b;

                    case DIVIDE ->
                            Math.abs(b) < 0.0000001
                                    ? Double.NaN
                                    : a / b;

                    case PYTHAGORAS ->
                            Math.hypot(
                                    a,
                                    b
                            );

                    case RECTANGLE_AREA ->
                            Math.abs(
                                    a * b
                            );

                    case BOX_VOLUME ->
                            Math.abs(
                                    a * b * c
                            );

                    case CIRCLE_AREA ->
                            Math.PI
                                    * a
                                    * a;

                    case SLOPE_DEGREES ->
                            Math.toDegrees(
                                    Math.atan2(
                                            a,
                                            b
                                    )
                            );

                    case SAFETY_FACTOR ->
                            Math.abs(b) < 0.0000001
                                    ? Double.NaN
                                    : a / b;

                    case STRESS ->
                            Math.abs(b) < 0.0000001
                                    ? Double.NaN
                                    : a / b;

                    case BEAM_UDL_MOMENT ->
                            a
                                    * b
                                    * b
                                    / 8.0;

                    case BEAM_CENTER_MOMENT ->
                            a
                                    * b
                                    / 4.0;

                    case WIND_FORCE ->
                            0.5
                                    * 1.225
                                    * 1.20
                                    * a
                                    * a
                                    * Math.abs(b);

                    case WATER_FORCE ->
                            0.5
                                    * 1000.0
                                    * 1.0
                                    * a
                                    * a
                                    * Math.abs(b);

                    case GRAVITY_LOAD ->
                            Math.abs(a)
                                    * 9.80665;

                    case ACCELERATION ->
                            Math.abs(b) < 0.0000001
                                    ? Double.NaN
                                    : a / b;

                    case TIME_STEP ->
                            a * b;

                    case DISPLACEMENT ->
                            0.5
                                    * a
                                    * b
                                    * b;
                };

        visiting.remove(
                node.id()
        );

        return Double.isFinite(
                result
        )
                ? result
                : Double.NaN;
    }

    private double inputValue(
            Node node,
            int slot,
            Set<Integer> visiting
    ) {
        if (slot >= node.inputCount()) {
            return 0.0;
        }

        int sourceId =
                node.input(
                        slot
                );

        if (sourceId < 0) {
            return 0.0;
        }

        Node source =
                node(
                        sourceId
                );

        if (source == null) {
            return 0.0;
        }

        return evaluateOutput(
                source,
                node.inputOutput(
                        slot
                ),
                visiting
        );
    }

    private boolean hasCycle() {
        for (Node node :
                nodes) {
            if (cycleFrom(
                    node,
                    new HashSet<>(),
                    new HashSet<>()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean cycleFrom(
            Node node,
            Set<Integer> visiting,
            Set<Integer> visited
    ) {
        if (visited.contains(
                node.id()
        )) {
            return false;
        }

        if (!visiting.add(
                node.id()
        )) {
            return true;
        }

        for (int slot = 0;
             slot < node.inputCount();
             slot++) {

            Node source =
                    node(
                            node.input(slot)
                    );

            if (source != null
                    && cycleFrom(
                    source,
                    visiting,
                    visited
            )) {
                return true;
            }
        }

        visiting.remove(
                node.id()
        );

        visited.add(
                node.id()
        );

        return false;
    }
}
