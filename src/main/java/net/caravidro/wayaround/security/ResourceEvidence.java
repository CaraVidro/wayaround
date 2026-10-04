package net.caravidro.wayaround.security;

/** Fixed-size observations, never pack names, paths, screenshots or arbitrary files. */
public record ResourceEvidence(int transparent, int hiddenModels, int opaqueOres, int inspected, String fingerprint) {
    public static final int HOSTS = 16;
    public static final int ORES = 4;
    public static final int HOST_MASK = (1 << HOSTS) - 1;
    public static final int ALL_MASK = (1 << (HOSTS + ORES)) - 1;

    public boolean valid() {
        return ((transparent | hiddenModels) & ~HOST_MASK) == 0
                && (opaqueOres & ~15) == 0 && (inspected & ~ALL_MASK) == 0
                && ((transparent | hiddenModels) & ~inspected) == 0
                && ((opaqueOres << HOSTS) & ~inspected) == 0
                && fingerprint != null && fingerprint.matches("[0-9a-f]{64}");
    }

    public int anomalies() { return Integer.bitCount(transparent | hiddenModels); }
    public boolean strong() {
        return valid() && anomalies() >= 6 && Integer.bitCount(opaqueOres) >= 2;
    }
    public boolean suspicious() { return valid() && anomalies() >= 3; }
    public boolean clean() { return valid() && inspected == ALL_MASK && anomalies() == 0; }

    /** Shared with the PNG scanner; legitimate slightly translucent textures stay below this threshold. */
    public static boolean transparentSample(int translucent, int samples) {
        return samples >= 64 && translucent >= Math.ceil(samples * .70);
    }

    public String summary() {
        return "host=" + anomalies() + "/16 alpha=" + Integer.bitCount(transparent)
                + " geometry=" + Integer.bitCount(hiddenModels) + " ore=" + Integer.bitCount(opaqueOres)
                + "/4 inspected=" + Integer.bitCount(inspected) + " fingerprint=" + fingerprint;
    }
}
