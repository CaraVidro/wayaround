package net.caravidro.wayaround.accessory;

/** Lens colors agree with the worn models; lifted spectacles do not cover the eyes. */
public final class EyewearOptics {
    private EyewearOptics() {}
    public static boolean coversEyes(AccessoryKind kind, int mode) {
        return kind != null && kind.breakableGlass()
                && !(kind == AccessoryKind.SPECTRAL_GLASSES && mode == 1);
    }
    public static int tint(AccessoryKind kind, int mode, int damage) {
        if (!coversEyes(kind, mode)) return 0;
        int rgb = kind == AccessoryKind.AERO_GOGGLES ? 0x706384 : 0x639ECD;
        int alpha = damage >= 2 ? 8 : damage == 1 ? 22 : 36;
        return alpha << 24 | rgb;
    }
}
