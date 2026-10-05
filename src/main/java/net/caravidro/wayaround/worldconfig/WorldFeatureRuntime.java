package net.caravidro.wayaround.worldconfig;

import net.minecraft.world.level.Level;

/**
 * Fast runtime mirror. SavedData remains the source of truth on the server;
 * this class keeps hot-path feature checks allocation-free.
 */
public final class WorldFeatureRuntime {

    private WorldFeatureRuntime() {}

    private record Snapshot(WorldFeatureSettings settings, long effectiveMask) {
        static Snapshot of(WorldFeatureSettings settings) {
            WorldFeatureSettings copy = new WorldFeatureSettings(settings);
            long mask = 0;
            for (WorldFeature feature : WorldFeature.values()) {
                if (copy.enabled(feature)) mask |= 1L << feature.ordinal();
            }
            return new Snapshot(copy, mask);
        }
        boolean enabled(WorldFeature feature) {
            return (effectiveMask & (1L << feature.ordinal())) != 0;
        }
    }

    private static volatile boolean attachedServer;
    private static volatile Snapshot SERVER = Snapshot.of(WorldFeatureSettings.defaults());
    private static volatile Snapshot CLIENT = Snapshot.of(WorldFeatureSettings.defaults());

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
                SERVER.settings()
        );
    }

    public static WorldFeatureSettings clientCopy() {
        return new WorldFeatureSettings(
                CLIENT.settings()
        );
    }

    public static void applyServer(
            WorldFeatureSettings settings
    ) {
        attachedServer=true;SERVER =
                Snapshot.of(settings);
    }

    public static void applyClient(
            WorldFeatureSettings settings
    ) {
        CLIENT =
                Snapshot.of(settings);
        if(!attachedServer)SERVER=Snapshot.of(settings);
    }

    public static void resetServer() {
        attachedServer=false;
        SERVER =
                Snapshot.of(WorldFeatureSettings.defaults());
    }

    public static void resetClient() {
        CLIENT =
                Snapshot.of(WorldFeatureSettings.defaults());
        if(!attachedServer)SERVER=CLIENT;
    }
}
