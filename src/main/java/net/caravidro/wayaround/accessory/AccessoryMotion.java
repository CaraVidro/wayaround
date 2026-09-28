package net.caravidro.wayaround.accessory;

/**
 * Declares whether a wearable owns lightweight client-side secondary motion.
 *
 * This is intentionally metadata on the accessory instead of hard-coded checks
 * in the renderer so future kits can reuse the same cloth/pendulum solvers.
 */
public enum AccessoryMotion {
    NONE,
    CLOTH,
    GEARS,
    CLOTH_AND_GEARS;

    public boolean cloth() {
        return this == CLOTH
                || this == CLOTH_AND_GEARS;
    }

    public boolean gears() {
        return this == GEARS
                || this == CLOTH_AND_GEARS;
    }
}
