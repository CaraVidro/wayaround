package net.caravidro.wayaround.worldconfig;

import net.minecraft.nbt.CompoundTag;

public final class WaveModeSettingsTest {

    public static void main(String[] args) {
        WorldFeatureSettings fresh =
                WorldFeatureSettings.allEnabled();

        require(
                fresh.waveMode()
                        == WaveMode.REALISTIC,
                "New selections should default to realistic waves"
        );

        fresh.setWaveMode(
                WaveMode.OFF
        );

        require(
                fresh.rawEnabled(
                        WorldFeature.WATER_DYNAMICS
                ),
                "Wave OFF must not disable currents / Living Water"
        );

        CompoundTag saved =
                fresh.saveToTag();

        WorldFeatureSettings loaded =
                WorldFeatureSettings.loadFromTag(
                        saved
                );

        require(
                loaded.waveMode()
                        == WaveMode.OFF,
                "Wave mode must persist in world data"
        );

        CompoundTag legacyEnabled =
                new CompoundTag();

        legacyEnabled.putBoolean(
                WorldFeature.WATER_DYNAMICS.key(),
                true
        );

        WorldFeatureSettings migratedEnabled =
                WorldFeatureSettings.loadFromTag(
                        legacyEnabled
                );

        require(
                migratedEnabled.waveMode()
                        == WaveMode.STYLIZED,
                "Legacy worlds with Living Water enabled must migrate to stylized"
        );

        CompoundTag legacyDisabled =
                new CompoundTag();

        legacyDisabled.putBoolean(
                WorldFeature.WATER_DYNAMICS.key(),
                false
        );

        WorldFeatureSettings migratedDisabled =
                WorldFeatureSettings.loadFromTag(
                        legacyDisabled
                );

        require(
                migratedDisabled.waveMode()
                        == WaveMode.OFF,
                "Legacy worlds with Living Water disabled must keep waves off"
        );

        WorldFeatureSettings disabled =
                WorldFeatureSettings.allDisabled();

        require(
                disabled.waveMode()
                        == WaveMode.OFF
                        && !disabled.rawEnabled(
                        WorldFeature.WATER_DYNAMICS
                ),
                "Disable all must disable both Living Water and its wave mode"
        );

        WorldFeatureSettings mask =
                WorldFeatureSettings.fromMask(
                        WorldFeatureSettings.allEnabled()
                                .toMask()
                );

        require(
                mask.waveMode()
                        == WaveMode.STYLIZED,
                "Boolean-only masks must decode to compatibility mode until explicit wave sync arrives"
        );

        System.out.println(
                "Wave mode world-config regression checks passed"
        );
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(
                    message
            );
        }
    }
}
