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

        int rgb = switch (kind) {
            case AERO_GOGGLES -> 0x706384;
            case RAILWAY_GOGGLES -> 0xC68A43;
            case MINER_GOGGLES -> 0x315E64;
            case STORM_VISOR -> 0x6E9FB8;
            case ARCTIC_GOGGLES -> 0xD6B66A;
            default -> 0x639ECD;
        };

        int base = switch (kind) {
            case MINER_GOGGLES -> 150;
            case STORM_VISOR -> 92;
            case ARCTIC_GOGGLES -> 82;
            case RAILWAY_GOGGLES -> 100;
            default -> 110;
        };

        int strength = damage >= 2
                ? Math.max(18, base / 4)
                : damage == 1
                ? Math.max(40, base * 3 / 5)
                : base;

        int red = 255 - (255 - (rgb >> 16 & 255)) * strength / 255;
        int green = 255 - (255 - (rgb >> 8 & 255)) * strength / 255;
        int blue = 255 - (255 - (rgb & 255)) * strength / 255;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
