package net.caravidro.wayaround.physical;

/**
 * Fundamental macroscopic matter phases used by the universal physics layer.
 *
 * <p>This is deliberately small. Exotic phases can be added later without
 * making today's thermal/pressure/flow contracts depend on them.</p>
 */
public enum MatterPhase {
    SOLID(true, true),
    LIQUID(false, true),
    GAS(false, false);

    private final boolean keepsShape;
    private final boolean keepsVolume;

    MatterPhase(
            boolean keepsShape,
            boolean keepsVolume
    ) {
        this.keepsShape = keepsShape;
        this.keepsVolume = keepsVolume;
    }

    public boolean keepsShape() {
        return keepsShape;
    }

    public boolean keepsVolume() {
        return keepsVolume;
    }

    public boolean isFluid() {
        return this != SOLID;
    }
}
