package net.caravidro.wayaround.worldgen.terrain;

import net.caravidro.wayaround.worldgen.geography.AntarcticField;

/** Deterministic escarpments; evaluating a location never loads chunks. */
public final class IceCliffField {
    private IceCliffField() {}

    public static double coastalStrength(int x, int z) {
        double latitude = AntarcticField.sample(x, z);
        if (latitude < 0.38 || latitude > 0.82) return 0;
        // Long stretches of wall alternate with the existing gently sloping coast.
        double patches = Math.sin(x / 410.0 + 1.7) * 0.65
                + Math.sin(x / 137.0 + z / 1100.0) * 0.35;
        return smooth(-0.45, 0.05, patches);
    }

    public static boolean hasOverhang(int x, int z) {
        return Math.sin(x / 57.0 + z / 83.0) > 0.15;
    }

    public static double coastBlend(int x, int y, int z, double latitude) {
        double cliff = coastalStrength(x, z);
        double soft = smooth(0.50, 0.64, latitude);
        // About two horizontal blocks instead of the former ~53-block ramp.
        double lip = 0;
        if (cliff > 0.8 && hasOverhang(x, z)) {
            double top = AntarcticTerrain.getSurfaceHeight(x, z);
            lip = 0.0012 * smooth(top - 13, top - 9, y);
        }
        double wall = smooth(0.5020 - lip, 0.5024 - lip, latitude);
        return soft + (wall - soft) * cliff;
    }

    public static Inland inland(int x, int z) {
        if (AntarcticField.sample(x, z) < 0.9) return Inland.NONE;
        int tileX = Math.floorDiv(x, 768);
        int tileZ = Math.floorDiv(z, 768);
        long hash = mix(tileX * 341873128712L + tileZ * 132897987541L);
        // One small plateau in roughly twelve 768 x 768 cells.
        if (Math.floorMod(hash, 12) != 0) return Inland.NONE;
        double centerX = tileX * 768.0 + 240 + Math.floorMod(hash >>> 8, 288);
        double centerZ = tileZ * 768.0 + 240 + Math.floorMod(hash >>> 20, 288);
        double radius = 72 + Math.floorMod(hash >>> 32, 56);
        double distance = Math.hypot(x - centerX, z - centerZ);
        double edge = radius - distance;
        double height = 24 + Math.floorMod(hash >>> 40, 20);
        return new Inland(height * smooth(-1, 1, edge), edge,
                Math.abs(edge) < 24 ? 1.0 : 0.0);
    }

    public static double calvingStrength(int x, int z) {
        double latitude = AntarcticField.sample(x, z);
        double coast = latitude >= 0.497 && latitude <= 0.508 ? coastalStrength(x, z) : 0;
        return Math.max(coast, inland(x, z).cliff());
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static double smooth(double min, double max, double value) {
        double t = Math.max(0, Math.min(1, (value - min) / (max - min)));
        return t * t * (3 - 2 * t);
    }

    public record Inland(double lift, double edge, double cliff) {
        private static final Inland NONE = new Inland(0, -1000, 0);
    }
}
