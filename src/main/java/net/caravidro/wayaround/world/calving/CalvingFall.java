package net.caravidro.wayaround.world.calving;

import java.util.HashMap;
import java.util.ArrayList;
import net.caravidro.wayaround.interaction.RigidFallMotion;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Rigid slab landing on actual terrain, including inland slopes and seabeds. */
public final class CalvingFall {
    private CalvingFall() {}

    public record Cell(int x, int y, int z) {}

    public static int drop(List<Cell> blocks, int dx, int dz, int minY, Predicate<Cell> solid) {
        Map<Long, Cell> bottoms = new HashMap<>();
        for (Cell block : blocks) {
            long key = ((long) block.x() << 32) ^ (block.z() & 0xffffffffL);
            bottoms.merge(key, block, (a, b) -> a.y() < b.y() ? a : b);
        }
        List<Integer> clearances = new ArrayList<>();
        for (Cell bottom : bottoms.values()) {
            int y = bottom.y();
            while (y > minY && !solid.test(new Cell(bottom.x() + dx, y - 1, bottom.z() + dz))) y--;
            clearances.add(bottom.y() - y);
        }
        return RigidFallMotion.supportedDrop(clearances, 512);
    }
}
