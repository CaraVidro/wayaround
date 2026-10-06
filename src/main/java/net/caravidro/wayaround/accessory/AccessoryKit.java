package net.caravidro.wayaround.accessory;

public enum AccessoryKit {
    ENGINEER("engineer"),
    AERO_ENGINEER("aero_engineer"),
    COOK("cook"),
    CONVENTIONAL("conventional"),
    OBJECT_HEADS("object_heads"),
    RAILWAY_WORKER("railway_worker"),
    DEEP_MINER("deep_miner"),
    STORM_CHASER("storm_chaser"),
    FIELD_NATURALIST("field_naturalist"),
    ARCTIC_EXPEDITION("arctic_expedition"),
    UTILITY("utility");

    private final String path;

    AccessoryKit(
            String path
    ) {
        this.path =
                path;
    }

    public String path() {
        return path;
    }

    public String translationKey() {
        return "accessory.kit.wayaround."
                + path;
    }
}
