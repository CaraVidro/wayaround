package net.caravidro.wayaround.worldconfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldFeatureSavedData
        extends SavedData {

    public static final String ID =
            "wayaround_world_features";

    private WorldFeatureSettings settings =
            compatibilityDefaults();

    public WorldFeatureSavedData() {
    }

    private static WorldFeatureSettings compatibilityDefaults() {
        WorldFeatureSettings settings =
                WorldFeatureSettings.allEnabled();

        /*
         * No SavedData means this is most likely a world created before wave
         * modes existed. New worlds arrive with pendingCreation and overwrite
         * this value before it is persisted.
         */
        settings.setWaveMode(
                WaveMode.STYLIZED
        );

        return settings;
    }

    private WorldFeatureSavedData(
            WorldFeatureSettings settings
    ) {
        this.settings =
                new WorldFeatureSettings(
                        settings
                );
    }

    public static SavedData.Factory<WorldFeatureSavedData> factory() {
        return new SavedData.Factory<>(
                WorldFeatureSavedData::new,
                WorldFeatureSavedData::load,
                null
        );
    }

    private static WorldFeatureSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        CompoundTag features =
                tag.contains(
                        "features"
                )
                        ? tag.getCompound(
                        "features"
                )
                        : new CompoundTag();

        return new WorldFeatureSavedData(
                WorldFeatureSettings.loadFromTag(
                        features
                )
        );
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.put(
                "features",
                settings.saveToTag()
        );

        return tag;
    }

    public WorldFeatureSettings settings() {
        return new WorldFeatureSettings(
                settings
        );
    }

    public void setSettings(
            WorldFeatureSettings settings
    ) {
        this.settings =
                new WorldFeatureSettings(
                        settings
                );

        setDirty();
    }
}
