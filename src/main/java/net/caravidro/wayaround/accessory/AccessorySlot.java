package net.caravidro.wayaround.accessory;

public enum AccessorySlot {
    HEAD,
    FACE,
    HANDS,
    TORSO,
    LEGS,
    FEET,
    BACK,
    EXTRA;

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

    public String translationKey() {
        return "accessory.slot.wayaround."
                + name().toLowerCase();
    }
}
