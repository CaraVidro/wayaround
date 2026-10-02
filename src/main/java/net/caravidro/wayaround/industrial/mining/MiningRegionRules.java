package net.caravidro.wayaround.industrial.mining;

import java.util.Optional;

public final class MiningRegionRules {
    public static final int CELL_SIZE = 384;
    public static final int ACTIVE_PERMILLE = 340;

    public record Region(
            int cellX,
            int cellZ,
            int centerX,
            int centerZ,
            int entryX,
            int entryZ,
            ComplexOreKind kind,
            boolean openPit,
            int radius,
            int undergroundY
    ) {}

    private MiningRegionRules() {}

    public static Optional<Region> region(long seed, int cellX, int cellZ) {
        long base = mix64(
                seed
                        ^ (long) cellX * 0x9E3779B97F4A7C15L
                        ^ (long) cellZ * 0xC2B2AE3D27D4EB4FL
                        ^ 0x57415941524F554EL
        );

        if (Math.floorMod(base, 1000L) >= ACTIVE_PERMILLE) {
            return Optional.empty();
        }

        int margin = 58;
        int span = CELL_SIZE - margin * 2;
        int centerX = cellX * CELL_SIZE
                + margin
                + (int) Math.floorMod(mix64(base ^ 0x11L), span);
        int centerZ = cellZ * CELL_SIZE
                + margin
                + (int) Math.floorMod(mix64(base ^ 0x22L), span);

        int kindRoll = (int) Math.floorMod(mix64(base ^ 0x33L), 100L);
        ComplexOreKind kind =
                kindRoll < 34 ? ComplexOreKind.IRON
                        : kindRoll < 61 ? ComplexOreKind.COAL
                        : kindRoll < 84 ? ComplexOreKind.COPPER
                        : ComplexOreKind.GOLD;

        boolean openPit =
                Math.floorMod(mix64(base ^ 0x44L), 100L) < 29L;

        int radius =
                18 + (int) Math.floorMod(mix64(base ^ 0x55L), 11L);

        int angleIndex =
                (int) Math.floorMod(mix64(base ^ 0x66L), 8L);

        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dz = {0, 1, 1, 1, 0, -1, -1, -1};

        int entryDistance =
                radius + 8;

        int entryX =
                centerX
                        + dx[angleIndex]
                                * entryDistance;

        int entryZ =
                centerZ
                        + dz[angleIndex]
                                * entryDistance;

        int yRoll =
                (int) Math.floorMod(
                        mix64(base ^ 0x77L),
                        29L
                );

        int undergroundY =
                switch (kind) {
                    case COAL -> 26 + yRoll;
                    case COPPER -> 4 + yRoll;
                    case IRON -> -18 + yRoll;
                    case GOLD -> -48 + Math.min(24, yRoll);
                };

        return Optional.of(
                new Region(
                        cellX,
                        cellZ,
                        centerX,
                        centerZ,
                        entryX,
                        entryZ,
                        kind,
                        openPit,
                        radius,
                        undergroundY
                )
        );
    }

    public static int cell(int coordinate) {
        return Math.floorDiv(coordinate, CELL_SIZE);
    }

    public static long regionKey(Region region) {
        return ((long) region.cellX() << 32)
                ^ (region.cellZ() & 0xffffffffL);
    }

    public static long localHash(
            long seed,
            Region region,
            int x,
            int y,
            int z,
            long salt) {
        return mix64(
                seed
                        ^ regionKey(region)
                        ^ (long) x * 73428767L
                        ^ (long) y * 912931L
                        ^ (long) z * 438289L
                        ^ salt
        );
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
