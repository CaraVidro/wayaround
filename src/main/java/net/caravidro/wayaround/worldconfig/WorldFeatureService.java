package net.caravidro.wayaround.worldconfig;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.WorldFeatureConfigS2CPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Owns the world-scoped Way Around feature configuration.
 *
 * New integrated worlds carry a pending selection from the creation screen.
 * Existing worlds load SavedData. Worlds made before this system default to
 * every feature enabled, preserving old behavior.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class WorldFeatureService {

    private WorldFeatureService() {}

    private static volatile WorldFeatureSettings pendingCreation;
    private static WorldFeatureSettings bootSettings;

    public static void prepareNewWorld(
            WorldFeatureSettings settings
    ) {
        pendingCreation =
                new WorldFeatureSettings(
                        settings
                );

        /*
         * Integrated world generation begins in the same JVM shortly after
         * CreateWorldScreen returns. Apply immediately so even spawn/worldgen
         * hooks see the selection before SavedData is created.
         */
        WorldFeatureRuntime.applyServer(
                settings
        );

        WorldFeatureRuntime.applyClient(
                settings
        );
    }

    public static boolean hasPendingCreation() {
        return pendingCreation != null;
    }

    @SubscribeEvent
    public static void aboutToStart(
            ServerAboutToStartEvent event
    ) {
        if (pendingCreation != null) {
            WorldFeatureRuntime.applyServer(
                    pendingCreation
            );
        } else {
            /*
             * Never leak settings from a previously closed integrated world
             * into the next server while its SavedData is loading.
             */
            bootSettings=loadBeforeGeneration(event.getServer());
            WorldFeatureRuntime.applyServer(bootSettings);
        }
    }

    @SubscribeEvent
    public static void started(
            ServerStartedEvent event
    ) {
        MinecraftServer server =
                event.getServer();

        WorldFeatureSavedData data =
                server.overworld()
                        .getDataStorage()
                        .computeIfAbsent(
                                WorldFeatureSavedData.factory(),
                                WorldFeatureSavedData.ID
                        );

        WorldFeatureSettings chosen =
                pendingCreation;

        if (chosen != null) {
            data.setSettings(
                    chosen
            );

            pendingCreation =
                    null;
        } else {
            chosen = bootSettings!=null?bootSettings:data.settings();
            data.setSettings(chosen);
        }

        WorldFeatureRuntime.applyServer(
                chosen
        );
    }

    private static WorldFeatureSettings loadBeforeGeneration(MinecraftServer server) {
        if(server instanceof net.minecraft.gametest.framework.GameTestServer)return WorldFeatureSettings.legacy();
        java.nio.file.Path root=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        java.nio.file.Path file=root.resolve("data").resolve(WorldFeatureSavedData.ID+".dat");
        try {
            if(java.nio.file.Files.isRegularFile(file)) {
                var tag=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(4*1024*1024));
                return WorldFeatureSettings.loadFromTag(tag.getCompound("data").getCompound("features"));
            }
            java.nio.file.Path region=root.resolve("region");
            if(java.nio.file.Files.isDirectory(region))try(var files=java.nio.file.Files.newDirectoryStream(region,"*.mca")) {
                if(files.iterator().hasNext())return WorldFeatureSettings.legacy();
            }
        } catch(java.io.IOException|RuntimeException e) {
            WayAround.LOGGER.error("Cannot read world feature settings before generation; preserving legacy geography",e);
            return WorldFeatureSettings.legacy();
        }
        return WorldFeatureSettings.defaults();
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            sync(
                    player
            );
        }
    }

    public static void sync(
            ServerPlayer player
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new WorldFeatureConfigS2CPayload(
                        WorldFeatureRuntime.serverCopy()
                                .toMask()
                )
        );
        net.caravidro.wayaround.worldgen.weather.local.CloudRainOverrides.sync(player);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void stopped(
            ServerStoppedEvent event
    ) {
        bootSettings=null;pendingCreation=null;WorldFeatureRuntime.resetServer();
        net.caravidro.wayaround.worldgen.weather.local.CloudRainOverrides.clear();
    }
}
