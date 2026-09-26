package net.caravidro.wayaround.worldconfig;

public enum WorldFeatureCategory {

    POWERS(
            "Powers & Spectrums"
    ),

    WORLD(
            "World & Atmosphere"
    ),

    INDUSTRY(
            "Industry & Engineering"
    ),

    ITEMS(
            "Combat & Equipment"
    ),

    SYSTEMS(
            "Immersion & Systems"
    );

    private final String title;

    WorldFeatureCategory(
            String title
    ) {
        this.title =
                title;
    }

    public String title() {
        return title;
    }
}
