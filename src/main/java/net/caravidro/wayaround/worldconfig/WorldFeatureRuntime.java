package net.caravidro.wayaround.worldconfig;

import net.minecraft.world.level.Level;

/**
 * Fast runtime mirror. SavedData remains the source of truth on the server;
 * this class keeps hot-path feature checks allocation-free.
 */
public final class WorldFeatureRuntime {

    private WorldFeatureRuntime() {}

    private static volatile WorldFeatureSettings SERVER =
            WorldFeatureSettings.allEnabled();

    private static volatile WorldFeatureSettings CLIENT =
            WorldFeatureSettings.allEnabled();

    public static boolean serverEnabled(
            WorldFeature feature
    ) {
        return SERVER.enabled(
                feature
        );
    }

    public static boolean clientEnabled(
            WorldFeature feature
    ) {
        return CLIENT.enabled(
                feature
        );
    }

    public static boolean enabled(
            Level level,
            WorldFeature feature
    ) {
        return level != null
                && (
                level.isClientSide
                        ? clientEnabled(
                        feature
                )
                        : serverEnabled(
                        feature
                )
        );
    }

    public static WorldFeatureSettings serverCopy() {
        return new WorldFeatureSettings(
                SERVER
        );
    }

    public static WorldFeatureSettings clientCopy() {
        return new WorldFeatureSettings(
                CLIENT
        );
    }

    public static void applyServer(
            WorldFeatureSettings settings
    ) {
        SERVER =
                new WorldFeatureSettings(
                        settings
                );
    }

    public static void applyClient(
            WorldFeatureSettings settings
    ) {
        CLIENT =
                new WorldFeatureSettings(
                        settings
                );
    }

    public static void resetServer() {
        SERVER =
                WorldFeatureSettings.allEnabled();
    }

    public static void resetClient() {
        CLIENT =
                WorldFeatureSettings.allEnabled();
    }
}
