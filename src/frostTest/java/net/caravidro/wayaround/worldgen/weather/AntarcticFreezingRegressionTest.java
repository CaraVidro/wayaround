package net.caravidro.wayaround.worldgen.weather;

import java.util.HashSet;
import java.util.Set;

public final class AntarcticFreezingRegressionTest {
    public static void main(String[] args) {
        long[] starts = {0, 1739, Integer.MAX_VALUE - 100L, Long.MAX_VALUE - 100, -200};
        int[][] chunks = {{0, 1700}, {-2000, 1700}, {1000, 2000}, {Integer.MAX_VALUE, Integer.MIN_VALUE}};
        for (long start : starts) {
            for (int[] chunk : chunks) {
                Set<Integer> covered = new HashSet<>();
                for (int tick = 0; tick < AntarcticFreezingSampling.COLUMNS; tick++) {
                    int column = AntarcticFreezingSampling.column(start + tick, chunk[0], chunk[1]);
                    check(column >= 0 && column < 256, "Freezing sample escaped its loaded chunk");
                    check(covered.add(column), "A surface column was repeated before every pool was visited");
                }
                check(covered.size() == 256, "Small pools must receive a freezing check within 12.8 seconds");
            }
        }
        check(AntarcticFreezingSampling.column(0, 0, 1700) != AntarcticFreezingSampling.column(0, 1, 1700),
                "Adjacent chunks should not freeze identical columns simultaneously");
        System.out.println("Antarctic freezing regression passed: complete bounded sweep and tick overflow.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
