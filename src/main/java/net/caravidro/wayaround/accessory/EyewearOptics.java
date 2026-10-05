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
        int strength = damage >= 2 ? 24 : damage == 1 ? 64 : 110;
        int red = 255 - (255 - (rgb >> 16 & 255)) * strength / 255;
        int green = 255 - (255 - (rgb >> 8 & 255)) * strength / 255;
        int blue = 255 - (255 - (rgb & 255)) * strength / 255;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
