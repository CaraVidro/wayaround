package net.caravidro.wayaround.industrial.electronics;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CircuitBoardData {

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
        RELAY(4);

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

    public boolean hasSignalPath() {
        Set<Integer> inputs =
                cellsOf(
                        ComponentType.INPUT_TERMINAL
                );

        Set<Integer> outputs =
                cellsOf(
                        ComponentType.OUTPUT_TERMINAL
                );

        if (inputs.isEmpty()
                || outputs.isEmpty()) {
            return false;
        }

        boolean[] visited =
                new boolean[CELL_COUNT];

        ArrayDeque<Integer> pending =
                new ArrayDeque<>();

        for (int input : inputs) {
            visited[input] =
                    true;

            pending.addLast(
                    input
            );
        }

        while (!pending.isEmpty()) {
            int current =
                    pending.removeFirst();

            if (outputs.contains(
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
