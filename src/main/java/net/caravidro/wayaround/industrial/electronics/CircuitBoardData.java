package net.caravidro.wayaround.industrial.electronics;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CircuitBoardData {

    public record Trace(
            int a,
            int b
    ) {}

    public static final int WIDTH = 6;
    public static final int HEIGHT = 4;
    public static final int CELL_COUNT = WIDTH * HEIGHT;
    public static final int MAX_TRACES = 72;

    private static final String KEY = "WayAroundCircuitBoard";

    public enum ComponentType {
        EMPTY(0),
        INPUT_TERMINAL(1),
        OUTPUT_TERMINAL(1),
        RESISTOR(1),
        CAPACITOR(2),
        DIODE(1),
        TRANSISTOR(3),
        RELAY(4),
        LED(1),
        BUZZER(2),
        DISTANCE_DETECTOR(3);

        private final int energyCost;

        ComponentType(int energyCost) {
            this.energyCost = energyCost;
        }

        public int energyCost() {
            return energyCost;
        }
    }

    private final ComponentType[] components =
            new ComponentType[CELL_COUNT];

    private final Set<Integer> traces =
            new HashSet<>();

    private CircuitBoardData() {
        Arrays.fill(
                components,
                ComponentType.EMPTY
        );
    }

    public static CircuitBoardData empty() {
        return new CircuitBoardData();
    }

    public static CircuitBoardData read(
            ItemStack stack
    ) {
        CircuitBoardData board =
                empty();

        if (stack == null
                || stack.isEmpty()) {
            return board;
        }

        CompoundTag root =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                ).copyTag();

        if (!root.contains(
                KEY,
                net.minecraft.nbt.Tag.TAG_COMPOUND
        )) {
            return board;
        }

        CompoundTag tag =
                root.getCompound(
                        KEY
                );

        int[] savedComponents =
                tag.getIntArray(
                        "Components"
                );

        for (int index = 0;
             index < Math.min(
                     CELL_COUNT,
                     savedComponents.length
             );
             index++) {

            int ordinal =
                    savedComponents[index];

            if (ordinal >= 0
                    && ordinal < ComponentType.values().length) {
                board.components[index] =
                        ComponentType.values()[ordinal];
            }
        }

        int[] savedTraces =
                tag.getIntArray(
                        "Traces"
                );

        for (int encoded : savedTraces) {
            if (board.traces.size() >= MAX_TRACES) {
                break;
            }

            int a =
                    encoded / CELL_COUNT;

            int b =
                    encoded % CELL_COUNT;

            if (validCell(a)
                    && validCell(b)
                    && a != b) {
                board.traces.add(
                        encode(
                                a,
                                b
                        )
                );
            }
        }

        return board;
    }

    public void write(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return;
        }

        CompoundTag board =
                new CompoundTag();

        int[] savedComponents =
                new int[CELL_COUNT];

        for (int index = 0;
             index < CELL_COUNT;
             index++) {
            savedComponents[index] =
                    components[index].ordinal();
        }

        board.putIntArray(
                "Components",
                savedComponents
        );

        int[] savedTraces =
                traces.stream()
                        .sorted()
                        .limit(MAX_TRACES)
                        .mapToInt(Integer::intValue)
                        .toArray();

        board.putIntArray(
                "Traces",
                savedTraces
        );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.put(
                        KEY,
                        board
                )
        );
    }

    public ComponentType component(
            int cell
    ) {
        return validCell(cell)
                ? components[cell]
                : ComponentType.EMPTY;
    }

    public boolean place(
            int cell,
            ComponentType type
    ) {
        if (!validCell(cell)
                || type == null
                || type == ComponentType.EMPTY
                || components[cell] != ComponentType.EMPTY) {
            return false;
        }

        components[cell] =
                type;

        return true;
    }

    public ComponentType remove(
            int cell
    ) {
        if (!validCell(cell)) {
            return ComponentType.EMPTY;
        }

        ComponentType previous =
                components[cell];

        components[cell] =
                ComponentType.EMPTY;

        removeTracesAt(
                cell
        );

        return previous;
    }

    public boolean hasTrace(
            int a,
            int b
    ) {
        return validCell(a)
                && validCell(b)
                && a != b
                && traces.contains(
                encode(
                        a,
                        b
                )
        );
    }

    public boolean removeTrace(
            int a,
            int b
    ) {
        return validCell(a)
                && validCell(b)
                && a != b
                && traces.remove(
                encode(
                        a,
                        b
                )
        );
    }

    public List<Trace> traceEdges() {
        List<Trace> result =
                new ArrayList<>();

        traces.stream()
                .sorted()
                .forEach(
                        encoded -> result.add(
                                new Trace(
                                        encoded / CELL_COUNT,
                                        encoded % CELL_COUNT
                                )
                        )
                );

        return List.copyOf(
                result
        );
    }

    public static int traceCopperCost(
            int a,
            int b
    ) {
        if (!validCell(a)
                || !validCell(b)
                || a == b) {
            return 0;
        }

        int dx =
                cellX(a)
                        - cellX(b);

        int dy =
                cellY(a)
                        - cellY(b);

        double distance =
                Math.sqrt(
                        dx * dx
                                + dy * dy
                );

        /*
         * One copper trace item represents about half a board-cell of routed
         * conductor. Long diagonal runs therefore cost noticeably more than
         * neighboring pads instead of every connection costing one item.
         */
        return Math.max(
                1,
                (int) Math.ceil(
                        distance * 2.0
                )
        );
    }

    public int copperCostAt(
            int cell
    ) {
        int total =
                0;

        for (Trace trace : traceEdges()) {
            if (trace.a() == cell
                    || trace.b() == cell) {
                total +=
                        traceCopperCost(
                                trace.a(),
                                trace.b()
                        );
            }
        }

        return total;
    }

    public boolean addTrace(
            int a,
            int b
    ) {
        if (!validCell(a)
                || !validCell(b)
                || a == b
                || traces.size() >= MAX_TRACES) {
            return false;
        }

        return traces.add(
                encode(
                        a,
                        b
                )
        );
    }

    public int removeTracesAt(
            int cell
    ) {
        if (!validCell(cell)) {
            return 0;
        }

        int before =
                traces.size();

        traces.removeIf(
                encoded -> {
                    int a =
                            encoded / CELL_COUNT;

                    int b =
                            encoded % CELL_COUNT;

                    return a == cell
                            || b == cell;
                }
        );

        return before
                - traces.size();
    }

    public boolean hasComponent(
            ComponentType type
    ) {
        if (type == null
                || type == ComponentType.EMPTY) {
            return false;
        }

        for (ComponentType component : components) {
            if (component == type) {
                return true;
            }
        }

        return false;
    }

    public int componentCount() {
        int count = 0;

        for (ComponentType component : components) {
            if (component != ComponentType.EMPTY) {
                count++;
            }
        }

        return count;
    }

    public int traceCount() {
        return traces.size();
    }

    public int powerDraw() {
        int draw =
                1
                        + Math.max(
                        0,
                        traceCount() / 8
                );

        for (ComponentType component : components) {
            draw +=
                    component.energyCost();
        }

        return Math.clamp(
                draw,
                1,
                64
        );
    }

    public boolean connected(
            ComponentType source,
            ComponentType target
    ) {
        if (source == null
                || target == null
                || source == ComponentType.EMPTY
                || target == ComponentType.EMPTY) {
            return false;
        }

        Set<Integer> sources =
                cellsOf(
                        source
                );

        Set<Integer> targets =
                cellsOf(
                        target
                );

        if (sources.isEmpty()
                || targets.isEmpty()) {
            return false;
        }

        boolean[] visited =
                new boolean[CELL_COUNT];

        ArrayDeque<Integer> pending =
                new ArrayDeque<>();

        for (int sourceCell : sources) {
            visited[sourceCell] =
                    true;

            pending.addLast(
                    sourceCell
            );
        }

        while (!pending.isEmpty()) {
            int current =
                    pending.removeFirst();

            if (targets.contains(
                    current
            )) {
                return true;
            }

            for (int encoded : traces) {
                int a =
                        encoded / CELL_COUNT;

                int b =
                        encoded % CELL_COUNT;

                int next =
                        a == current
                                ? b
                                : b == current
                                        ? a
                                        : -1;

                if (next >= 0
                        && !visited[next]) {
                    visited[next] =
                            true;

                    pending.addLast(
                            next
                    );
                }
            }
        }

        return false;
    }

    public boolean hasSignalPath() {
        return connected(
                ComponentType.INPUT_TERMINAL,
                ComponentType.OUTPUT_TERMINAL
        );
    }

    public float complexity() {
        return Mth.clamp(
                componentCount()
                        / 12.0F
                        + traceCount()
                                / 36.0F,
                0.0F,
                2.0F
        );
    }

    private Set<Integer> cellsOf(
            ComponentType type
    ) {
        Set<Integer> result =
                new HashSet<>();

        for (int index = 0;
             index < CELL_COUNT;
             index++) {
            if (components[index] == type) {
                result.add(
                        index
                );
            }
        }

        return result;
    }

    public static int cell(
            int x,
            int y
    ) {
        if (x < 0
                || x >= WIDTH
                || y < 0
                || y >= HEIGHT) {
            return -1;
        }

        return y * WIDTH
                + x;
    }

    public static int cellX(
            int cell
    ) {
        return validCell(cell)
                ? cell % WIDTH
                : -1;
    }

    public static int cellY(
            int cell
    ) {
        return validCell(cell)
                ? cell / WIDTH
                : -1;
    }

    private static boolean validCell(
            int cell
    ) {
        return cell >= 0
                && cell < CELL_COUNT;
    }

    private static int encode(
            int a,
            int b
    ) {
        int first =
                Math.min(
                        a,
                        b
                );

        int second =
                Math.max(
                        a,
                        b
                );

        return first * CELL_COUNT
                + second;
    }
}
