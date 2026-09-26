package net.caravidro.wayaround.accessory;

public enum AccessoryKind {
    SPECTRAL_GLASSES(
            "spectral_glasses",
            AccessorySlot.HEAD
    ),
    WORK_GLOVES(
            "work_gloves",
            AccessorySlot.HANDS
    ),
    ENGINEER_CAPE(
            "engineer_cape",
            AccessorySlot.TORSO
    ),
    WIND_BOOTS(
            "wind_boots",
            AccessorySlot.FEET
    );

    private final String path;
    private final AccessorySlot slot;

    AccessoryKind(
            String path,
            AccessorySlot slot
    ) {
        this.path =
                path;

        this.slot =
                slot;
    }

    public String path() {
        return path;
    }

    public AccessorySlot slot() {
        return slot;
    }

    public static AccessoryKind byPath(
            String path
    ) {
        if (path == null
                || path.isBlank()) {
            return null;
        }

        for (AccessoryKind kind :
                values()) {
            if (kind.path.equals(
                    path
            )) {
                return kind;
            }
        }

        return null;
    }
}
