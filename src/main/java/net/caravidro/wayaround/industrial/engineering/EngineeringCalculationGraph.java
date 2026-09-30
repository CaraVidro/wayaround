package net.caravidro.wayaround.industrial.engineering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Small deterministic node graph used by the engineering workbench.
 *
 * The graph deliberately stores geometry/calculation semantics separately from
 * the screen so future machines, blueprints and server-side validation can
 * reuse the same evaluator.
 */
public final class EngineeringCalculationGraph {

    public enum NodeType {
        CONSTANT(
                "Number",
                0
        ),
        BLOCK_PROPERTY(
                "Block property",
                0
        ),
        ADD(
                "Add",
                2
        ),
        SUBTRACT(
                "Subtract",
                2
        ),
        MULTIPLY(
                "Multiply",
                2
        ),
        DIVIDE(
                "Divide",
                2
        ),
        PYTHAGORAS(
                "Distance / Pythagoras",
                2
        ),
        RECTANGLE_AREA(
                "Rectangle area",
                2
        ),
        BOX_VOLUME(
                "Box volume",
                3
        ),
        CIRCLE_AREA(
                "Circle area",
                1
        ),
        SLOPE_DEGREES(
                "Slope angle",
                2
        ),
        SAFETY_FACTOR(
                "Safety factor",
                2
        ),
        STRESS(
                "Stress F/A",
                2
        ),
        BEAM_UDL_MOMENT(
                "Beam moment qL²/8",
                2
        ),
        BEAM_CENTER_MOMENT(
                "Beam moment PL/4",
                2
        );

        private final String label;
        private final int inputs;

        NodeType(
                String label,
                int inputs
        ) {
            this.label =
                    label;

            this.inputs =
                    inputs;
        }

        public String label() {
            return label;
        }

        public int inputs() {
            return inputs;
        }
    }

    public static final class Node {

        private final int id;
        private final NodeType type;
        private final int[] inputs =
                new int[] {
                        -1,
                        -1,
                        -1
                };

        private int x;
        private int y;

        private String label;
        private String unit;

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
            this.id =
                    id;

            this.type =
                    type;

            this.x =
                    x;

            this.y =
                    y;

            this.label =
                    label == null
                            ? type.label()
                            : label;

            this.literal =
                    literal;

            this.unit =
                    unit == null
                            ? ""
                            : unit;

            this.result =
                    literal;
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
            this.x =
                    x;

            this.y =
                    y;
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
        }

        public double literal() {
            return literal;
        }

        public void literal(
                double literal
        ) {
            this.literal =
                    Double.isFinite(
                            literal
                    )
                            ? literal
                            : 0.0;
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

        public int inputCount() {
            return type.inputs();
        }

        private void input(
                int slot,
                int nodeId
        ) {
            inputs[slot] =
                    nodeId;
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
                || inputSlot < 0
                || inputSlot >= to.inputCount()) {
            return false;
        }

        int old =
                to.input(
                        inputSlot
                );

        to.input(
                inputSlot,
                fromId
        );

        if (hasCycle()) {
            to.input(
                    inputSlot,
                    old
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
                -1
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
            for (int slot =
                         0;
                 slot < node.inputCount();
                 slot++) {

                if (node.input(
                        slot
                ) == nodeId) {
                    node.input(
                            slot,
                            -1
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
                    evaluate(
                            node,
                            new HashSet<>()
                    );
        }
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

                    case ADD ->
                            a + b;

                    case SUBTRACT ->
                            a - b;

                    case MULTIPLY ->
                            a * b;

                    case DIVIDE ->
                            Math.abs(
                                    b
                            ) < 0.0000001
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
                            Math.abs(
                                    b
                            ) < 0.0000001
                                    ? Double.NaN
                                    : a / b;

                    case STRESS ->
                            Math.abs(
                                    b
                            ) < 0.0000001
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

        return evaluate(
                source,
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

        for (int slot =
                     0;
             slot < node.inputCount();
             slot++) {

            Node source =
                    node(
                            node.input(
                                    slot
                            )
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
