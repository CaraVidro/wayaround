package net.caravidro.wayaround.worldconfig;

/**
 * World-scoped switches for every major Way Around system.
 *
 * Registration is intentionally never conditional: items/blocks/entities stay
 * registry-stable. These switches decide whether their mechanics may actually
 * operate in the current world.
 */
public enum WorldFeature {

    SPECTRUMS(
            WorldFeatureCategory.POWERS,
            "Spectrums",
            "Void, Tukuna and Justice access / abilities."
    ),

    DOMAINS(
            WorldFeatureCategory.POWERS,
            "Domains",
            "Domain expansion systems and domain combat."
    ),

    TUKUNA_SYSTEM(
            WorldFeatureCategory.POWERS,
            "Tukuna",
            "Fingers, receptacles, possession and Tukuna progression."
    ),

    IMMORTAL_WHEEL(
            WorldFeatureCategory.POWERS,
            "Immortal Wheel",
            "Adaptation wheel, remnant and reactivation."
    ),

    THERMAL_SYSTEM(
            WorldFeatureCategory.POWERS,
            "Spectrum Thermal Physics",
            "Extreme heat, melting, ignition and Fuga temperature effects."
    ),

    PROCEDURAL_CLOUDS(
            WorldFeatureCategory.WORLD,
            "Procedural Clouds",
            "Way Around volumetric cloud renderer and cloud shadows."
    ),

    LIVING_WEATHER(
            WorldFeatureCategory.WORLD,
            "Living Weather",
            "Local storms, wind, ecology reactions and dynamic precipitation."
    ),

    WIND_PARTICLES(
            WorldFeatureCategory.WORLD,
            "Wind & Foliage",
            "Wind-driven leaves, particles and precipitation drift."
    ),

    WATER_DYNAMICS(
            WorldFeatureCategory.WORLD,
            "Living Water",
            "Surface waves, currents, splashes and hydrodynamic ambience."
    ),

    LIVING_VEGETATION(
            WorldFeatureCategory.WORLD,
            "Living Vegetation",
            "Extra vegetation generation and environmental vegetation behavior."
    ),

    ANTARCTICA(
            WorldFeatureCategory.WORLD,
            "Antarctica & Geography",
            "Antarctic terrain, freezing, blizzards, frost and glacial systems."
    ),

    INDUSTRIAL_MACHINES(
            WorldFeatureCategory.INDUSTRY,
            "Industrial Machines",
            "Blaster, sawmill and general industrial machinery."
    ),

    ASSEMBLY(
            WorldFeatureCategory.INDUSTRY,
            "Assembly",
            "Primitive assembly, workbench and assembled mechanisms."
    ),

    POWER_NETWORKS(
            WorldFeatureCategory.INDUSTRY,
            "Power Networks",
            "Shafts, gearboxes, generators, cables and energy networks."
    ),

    SHIPS(
            WorldFeatureCategory.INDUSTRY,
            "Ships",
            "Caravels, sailing ships, coal ships and great ships."
    ),

    WAR_WITHOUT_REASON(
            WorldFeatureCategory.ITEMS,
            "War Without Reason",
            "Firearms, physical bullets, rockets and war projectiles."
    ),

    SPECTRAL_OBJECTS(
            WorldFeatureCategory.ITEMS,
            "Spectral Objects",
            "Spectral sword, armor, compass and hidden item traits."
    ),

    ACCESSORIES(
            WorldFeatureCategory.ITEMS,
            "Aceculture / Accessories",
            "Accessory slots and cosmetic wearable objects."
    ),

    VOICE_CHAT(
            WorldFeatureCategory.SYSTEMS,
            "Voice Chat",
            "Proximity voice transport and spoken Spectrum commands."
    ),

    MEDIA(
            WorldFeatureCategory.SYSTEMS,
            "Media",
            "Cameras, TV/audio capture and media devices."
    ),

    DREAMS(
            WorldFeatureCategory.SYSTEMS,
            "Dreams",
            "Dream / wake systems and dream camera behavior."
    ),

    CINEMATICS(
            WorldFeatureCategory.SYSTEMS,
            "Cinematics",
            "Way Around camera locks, procedural cutscenes and cinematic poses."
    );

    private final WorldFeatureCategory category;
    private final String title;
    private final String description;

    WorldFeature(
            WorldFeatureCategory category,
            String title,
            String description
    ) {
        this.category =
                category;
        this.title =
                title;
        this.description =
                description;
    }

    public WorldFeatureCategory category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String key() {
        return name()
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }
}
