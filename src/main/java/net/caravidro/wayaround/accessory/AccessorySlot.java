package net.caravidro.wayaround.accessory;

public enum AccessorySlot {
    HEAD,
    HANDS,
    TORSO,
    FEET;

    public static AccessorySlot byOrdinal(
            int ordinal
    ) {
        AccessorySlot[] values =
                values();

        return ordinal >= 0
                && ordinal < values.length
                ? values[ordinal]
                : null;
    }
}
