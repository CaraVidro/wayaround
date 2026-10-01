package net.caravidro.wayaround.storage;

/** Center-out square traversal. The caller supplies a fixed per-tick budget. */
public final class IncrementalSquareScan {
    private final int size;
    private int index;
    private int x;
    private int z;

    public IncrementalSquareScan(int radius) {
        if (radius < 0 || radius > 1024) throw new IllegalArgumentException("radius");
        size = (2 * radius + 1) * (2 * radius + 1);
    }

    public boolean advance() {
        if (index >= size) return false;
        int i = index++;
        if (i == 0) { x = 0; z = 0; return true; }
        int ring = (int) Math.ceil((Math.sqrt(i + 1.0) - 1.0) / 2.0);
        int edge = 2 * ring;
        int offset = i - (edge - 1) * (edge - 1);
        switch (offset / edge) {
            case 0 -> { x = -ring + offset; z = -ring; }
            case 1 -> { x = ring; z = -ring + offset - edge; }
            case 2 -> { x = ring - (offset - 2 * edge); z = ring; }
            default -> { x = -ring; z = ring - (offset - 3 * edge); }
        }
        return true;
    }

    public boolean complete() { return index == size; }
    public int x() { return x; }
    public int z() { return z; }
}
