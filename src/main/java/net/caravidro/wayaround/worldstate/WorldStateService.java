package net.caravidro.wayaround.worldstate;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Small public API for the persistent memory of a Way Around world.
 */
public final class WorldStateService {

    private WorldStateService() {
    }

    public static WorldEvent record(
            ServerLevel level,
            ResourceLocation type,
            BlockPos position,
            @Nullable UUID actor,
            CompoundTag payload
    ) {
        return WorldStateData.get(
                level.getServer()
        ).record(
                type,
                level.dimension()
                        .location(),
                position,
                level.getGameTime(),
                actor,
                payload
        );
    }

    public static List<WorldEvent> recent(
            MinecraftServer server,
            int max
    ) {
        return WorldStateData.get(
                server
        ).recent(
                max
        );
    }

    public static List<WorldEvent> recent(
            MinecraftServer server,
            ResourceLocation type,
            int max
    ) {
        return WorldStateData.get(
                server
        ).recent(
                type,
                max
        );
    }

    public static List<WorldEvent> nearby(
            ServerLevel level,
            BlockPos center,
            double radius,
            int max
    ) {
        return WorldStateData.get(
                level.getServer()
        ).nearby(
                level.dimension()
                        .location(),
                center,
                radius,
                max
        );
    }

    public static CompoundTag component(
            MinecraftServer server,
            ResourceLocation key
    ) {
        return WorldStateData.get(
                server
        ).component(
                key
        );
    }

    public static void putComponent(
            MinecraftServer server,
            ResourceLocation key,
            CompoundTag value
    ) {
        WorldStateData.get(
                server
        ).putComponent(
                key,
                value
        );
    }

    /**
     * Atomically reads, mutates and writes a small namespaced component.
     * Intended for low-frequency persistent state, not per-tick simulation.
     */
    public static void updateComponent(
            MinecraftServer server,
            ResourceLocation key,
            Consumer<CompoundTag> mutation
    ) {
        CompoundTag value =
                component(
                        server,
                        key
                );

        mutation.accept(
                value
        );

        putComponent(
                server,
                key,
                value
        );
    }
}
