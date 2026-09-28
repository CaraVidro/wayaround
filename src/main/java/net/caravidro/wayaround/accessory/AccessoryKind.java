package net.caravidro.wayaround.accessory;

public enum AccessoryKind {

    // Compatibility / older experimental pieces.
    SPECTRAL_GLASSES(
            "spectral_glasses",
            AccessoryKit.UTILITY,
            AccessorySlot.FACE,
            AccessoryMotion.NONE,
            true,
            false,
            420
    ),
    WORK_GLOVES(
            "work_gloves",
            AccessoryKit.ENGINEER,
            AccessorySlot.HANDS,
            AccessoryMotion.NONE,
            false,
            false,
            520
    ),
    ENGINEER_CAPE(
            "engineer_cape",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.BACK,
            AccessoryMotion.CLOTH,
            false,
            false,
            620
    ),
    WIND_BOOTS(
            "wind_boots",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.FEET,
            AccessoryMotion.NONE,
            false,
            false,
            560
    ),

    // Engineer.
    ENGINEER_CAP(
            "engineer_cap",
            AccessoryKit.ENGINEER,
            AccessorySlot.HEAD,
            AccessoryMotion.NONE,
            false,
            false,
            520
    ),
    ENGINEER_GOGGLES(
            "engineer_goggles",
            AccessoryKit.ENGINEER,
            AccessorySlot.FACE,
            AccessoryMotion.NONE,
            true,
            false,
            460
    ),
    ENGINEER_JACKET(
            "engineer_jacket",
            AccessoryKit.ENGINEER,
            AccessorySlot.TORSO,
            AccessoryMotion.NONE,
            false,
            false,
            760
    ),
    ENGINEER_GLOVES(
            "engineer_gloves",
            AccessoryKit.ENGINEER,
            AccessorySlot.HANDS,
            AccessoryMotion.NONE,
            false,
            false,
            560
    ),
    ENGINEER_TROUSERS(
            "engineer_trousers",
            AccessoryKit.ENGINEER,
            AccessorySlot.LEGS,
            AccessoryMotion.NONE,
            false,
            false,
            720
    ),
    ENGINEER_BOOTS(
            "engineer_boots",
            AccessoryKit.ENGINEER,
            AccessorySlot.FEET,
            AccessoryMotion.NONE,
            false,
            false,
            680
    ),
    ENGINEER_GEAR_HARNESS(
            "engineer_gear_harness",
            AccessoryKit.ENGINEER,
            AccessorySlot.EXTRA,
            AccessoryMotion.GEARS,
            false,
            false,
            640
    ),

    // Aero engineer.
    AERO_ENGINEER_CAP(
            "aero_engineer_cap",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.HEAD,
            AccessoryMotion.NONE,
            false,
            false,
            560
    ),
    AERO_GOGGLES(
            "aero_goggles",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.FACE,
            AccessoryMotion.NONE,
            true,
            false,
            500
    ),
    AERO_JACKET(
            "aero_jacket",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.TORSO,
            AccessoryMotion.NONE,
            false,
            false,
            820
    ),
    AERO_GLOVES(
            "aero_gloves",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.HANDS,
            AccessoryMotion.NONE,
            false,
            false,
            600
    ),
    AERO_TROUSERS(
            "aero_trousers",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.LEGS,
            AccessoryMotion.NONE,
            false,
            false,
            760
    ),
    AERO_BOOTS(
            "aero_boots",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.FEET,
            AccessoryMotion.NONE,
            false,
            false,
            720
    ),
    AERO_CAPE(
            "aero_cape",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.BACK,
            AccessoryMotion.CLOTH,
            false,
            false,
            700
    ),
    AERO_GEAR_CLUSTER(
            "aero_gear_cluster",
            AccessoryKit.AERO_ENGINEER,
            AccessorySlot.EXTRA,
            AccessoryMotion.GEARS,
            false,
            false,
            680
    ),

    // Cook.
    CHEF_HAT(
            "chef_hat",
            AccessoryKit.COOK,
            AccessorySlot.HEAD,
            AccessoryMotion.NONE,
            false,
            true,
            420
    ),
    CHEF_COAT(
            "chef_coat",
            AccessoryKit.COOK,
            AccessorySlot.TORSO,
            AccessoryMotion.NONE,
            false,
            false,
            620
    ),
    CHEF_GLOVES(
            "chef_gloves",
            AccessoryKit.COOK,
            AccessorySlot.HANDS,
            AccessoryMotion.NONE,
            false,
            false,
            420
    ),
    CHEF_TROUSERS(
            "chef_trousers",
            AccessoryKit.COOK,
            AccessorySlot.LEGS,
            AccessoryMotion.NONE,
            false,
            false,
            560
    ),
    CHEF_SHOES(
            "chef_shoes",
            AccessoryKit.COOK,
            AccessorySlot.FEET,
            AccessoryMotion.NONE,
            false,
            false,
            520
    ),
    CHEF_APRON(
            "chef_apron",
            AccessoryKit.COOK,
            AccessorySlot.EXTRA,
            AccessoryMotion.CLOTH,
            false,
            false,
            500
    );

    private final String path;
    private final AccessoryKit kit;
    private final AccessorySlot slot;
    private final AccessoryMotion motion;
    private final boolean breakableGlass;
    private final boolean ratHost;
    private final int maxWear;

    AccessoryKind(
            String path,
            AccessoryKit kit,
            AccessorySlot slot,
            AccessoryMotion motion,
            boolean breakableGlass,
            boolean ratHost,
            int maxWear
    ) {
        this.path = path;
        this.kit = kit;
        this.slot = slot;
        this.motion = motion;
        this.breakableGlass = breakableGlass;
        this.ratHost = ratHost;
        this.maxWear = maxWear;
    }

    public String path() {
        return path;
    }

    public AccessoryKit kit() {
        return kit;
    }

    public AccessorySlot slot() {
        return slot;
    }

    public AccessoryMotion motion() {
        return motion;
    }

    public boolean breakableGlass() {
        return breakableGlass;
    }

    /**
     * Reserved for the future rat system. A rat may treat this accessory as a
     * valid small-passenger shelter without the accessory code knowing about
     * the rat implementation itself.
     */
    public boolean ratHost() {
        return ratHost;
    }

    public int maxWear() {
        return maxWear;
    }

    public String translationKey() {
        return "item.wayaround."
                + path;
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
