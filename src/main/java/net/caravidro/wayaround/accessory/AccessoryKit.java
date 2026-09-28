package net.caravidro.wayaround.accessory;

public enum AccessoryKit {
    ENGINEER("engineer"),
    AERO_ENGINEER("aero_engineer"),
    COOK("cook"),
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
