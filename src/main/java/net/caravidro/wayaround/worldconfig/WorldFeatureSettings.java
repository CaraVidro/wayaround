package net.caravidro.wayaround.worldconfig;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;

public final class WorldFeatureSettings {

    private final EnumMap<WorldFeature, Boolean> values =
            new EnumMap<>(
                    WorldFeature.class
            );

    public WorldFeatureSettings() {
        setAll(
                true
        );
    }

    public WorldFeatureSettings(
            WorldFeatureSettings other
    ) {
        for (WorldFeature feature :
                WorldFeature.values()) {
            values.put(
                    feature,
                    other.rawEnabled(
                            feature
                    )
            );
        }
    }

    public static WorldFeatureSettings allEnabled() {
        return new WorldFeatureSettings();
    }

    public static WorldFeatureSettings allDisabled() {
        WorldFeatureSettings settings =
                new WorldFeatureSettings();

        settings.setAll(
                false
        );

        return settings;
    }

    public boolean rawEnabled(
            WorldFeature feature
    ) {
        return values.getOrDefault(
                feature,
                true
        );
    }

    /**
     * Effective state includes only hard logical dependencies. Children remain
     * individually configurable, but cannot operate while their root system is
     * disabled.
     */
    public boolean enabled(
            WorldFeature feature
    ) {
        if (!rawEnabled(
                feature
        )) {
            return false;
        }

        return switch (feature) {
            case DOMAINS,
                 TUKUNA_SYSTEM,
                 IMMORTAL_WHEEL,
                 THERMAL_SYSTEM ->
                    rawEnabled(
                            WorldFeature.SPECTRUMS
                    );

            case ASSEMBLY,
                 POWER_NETWORKS,
                 SHIPS ->
                    rawEnabled(
                            WorldFeature.INDUSTRIAL_MACHINES
                    );

            default ->
                    true;
        };
    }

    public void set(
            WorldFeature feature,
            boolean enabled
    ) {
        values.put(
                feature,
                enabled
        );
    }

    public void toggle(
            WorldFeature feature
    ) {
        set(
                feature,
                !rawEnabled(
                        feature
                )
        );
    }

    public void setAll(
            boolean enabled
    ) {
        for (WorldFeature feature :
                WorldFeature.values()) {
            values.put(
                    feature,
                    enabled
            );
        }
    }

    public int enabledCount() {
        int count =
                0;

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (enabled(
                    feature
            )) {
                count++;
            }
        }

        return count;
    }

    public CompoundTag saveToTag() {
        CompoundTag tag =
                new CompoundTag();

        for (WorldFeature feature :
                WorldFeature.values()) {
            tag.putBoolean(
                    feature.key(),
                    rawEnabled(
                            feature
                    )
            );
        }

        return tag;
    }

    public static WorldFeatureSettings loadFromTag(
            CompoundTag tag
    ) {
        WorldFeatureSettings settings =
                allEnabled();

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (tag.contains(
                    feature.key()
            )) {
                settings.set(
                        feature,
                        tag.getBoolean(
                                feature.key()
                        )
                );
            }
        }

        return settings;
    }

    public long toMask() {
        long mask =
                0L;

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (rawEnabled(
                    feature
            )) {
                mask |=
                        1L
                                << feature.ordinal();
            }
        }

        return mask;
    }

    public static WorldFeatureSettings fromMask(
            long mask
    ) {
        WorldFeatureSettings settings =
                allDisabled();

        for (WorldFeature feature :
                WorldFeature.values()) {
            settings.set(
                    feature,
                    (
                            mask
                                    & (
                                    1L
                                            << feature.ordinal()
                            )
                    )
                            != 0L
            );
        }

        return settings;
    }

    public Map<WorldFeature, Boolean> snapshot() {
        return Map.copyOf(
                values
        );
    }
}
