package net.caravidro.wayaround.worldconfig;

import java.util.Locale;

public enum WaveMode {

    OFF(
            "Off",
            "No Way Around surface-wave mesh or wave-driven vessel motion."
    ),

    STYLIZED(
            "Stylized",
            "Legacy lightweight waves: small animated surface motion with minimal vessel response."
    ),

    REALISTIC(
            "Realistic (Experimental)",
            "Experimental shared OceanWaveField: connected mesh, breakers, shoreline run-up and hull response. May change substantially."
    );

    private final String title;
    private final String description;

    WaveMode(
            String title,
            String description
    ) {
        this.title =
                title;

        this.description =
                description;
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
                        Locale.ROOT
                );
    }

    public WaveMode next() {
        return switch (this) {
            case OFF ->
                    STYLIZED;

            case STYLIZED ->
                    REALISTIC;

            case REALISTIC ->
                    OFF;
        };
    }

    public static WaveMode fromKey(
            String key,
            WaveMode fallback
    ) {
        if (key == null) {
            return fallback;
        }

        for (WaveMode mode :
                values()) {
            if (mode.key()
                    .equalsIgnoreCase(
                            key
                    )) {
                return mode;
            }
        }

        return fallback;
    }

    public static WaveMode fromNetwork(
            int ordinal
    ) {
        WaveMode[] values =
                values();

        if (ordinal < 0
                || ordinal >= values.length) {
            return STYLIZED;
        }

        return values[ordinal];
    }
}
