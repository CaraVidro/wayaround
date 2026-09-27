package net.caravidro.wayaround.worldstate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent memory shared by Way Around systems.
 *
 * <p>This is deliberately a foundation, not a simulation god-object. It stores
 * historical events and small namespaced state components. Domain-specific
 * engines continue to own their detailed runtime data.</p>
 */
public final class WorldStateData extends SavedData {

    public static final String ID =
            "wayaround_world_state";

    public static final int SCHEMA_VERSION =
            1;

    public static final int MAX_EVENTS =
            8192;

    private final Deque<WorldEvent> events =
            new ArrayDeque<>();

    private final Map<ResourceLocation, CompoundTag> components =
            new LinkedHashMap<>();

    private long nextSequence =
            1L;

    public static WorldStateData get(
            MinecraftServer server
    ) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                WorldStateData::new,
                                WorldStateData::load
                        ),
                        ID
                );
    }

    public WorldEvent record(
            ResourceLocation type,
            ResourceLocation dimension,
            BlockPos position,
            long gameTime,
            @Nullable UUID actor,
            CompoundTag payload
    ) {
        WorldEvent event =
                new WorldEvent(
                        nextSequence++,
                        type,
                        dimension,
                        position.asLong(),
                        gameTime,
                        System.currentTimeMillis(),
                        actor,
                        payload
                );

        events.addLast(
                event
        );

        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }

        setDirty();
        return event;
    }

    public List<WorldEvent> recent(
            int max
    ) {
        int limit =
                Math.max(
                        0,
                        max
                );

        if (limit == 0) {
            return List.of();
        }

        ArrayList<WorldEvent> result =
                new ArrayList<>(
                        Math.min(
                                limit,
                                events.size()
                        )
                );

        var iterator =
                events.descendingIterator();

        while (iterator.hasNext()
                && result.size() < limit) {
            result.add(
                    iterator.next()
            );
        }

        return List.copyOf(
                result
        );
    }

    public List<WorldEvent> recent(
            ResourceLocation type,
            int max
    ) {
        int limit =
                Math.max(
                        0,
                        max
                );

        if (limit == 0) {
            return List.of();
        }

        ArrayList<WorldEvent> result =
                new ArrayList<>(
                        limit
                );

        var iterator =
                events.descendingIterator();

        while (iterator.hasNext()
                && result.size() < limit) {
            WorldEvent event =
                    iterator.next();

            if (event.type()
                    .equals(
                            type
                    )) {
                result.add(
                        event
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public List<WorldEvent> nearby(
            ResourceLocation dimension,
            BlockPos center,
            double radius,
            int max
    ) {
        int limit =
                Math.max(
                        0,
                        max
                );

        if (limit == 0
                || radius < 0.0D) {
            return List.of();
        }

        double radiusSq =
                radius * radius;

        ArrayList<WorldEvent> result =
                new ArrayList<>(
                        limit
                );

        var iterator =
                events.descendingIterator();

        while (iterator.hasNext()
                && result.size() < limit) {
            WorldEvent event =
                    iterator.next();

            if (!event.dimension()
                    .equals(
                            dimension
                    )) {
                continue;
            }

            BlockPos pos =
                    event.blockPos();

            double dx =
                    pos.getX()
                            - center.getX();

            double dy =
                    pos.getY()
                            - center.getY();

            double dz =
                    pos.getZ()
                            - center.getZ();

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    <= radiusSq) {
                result.add(
                        event
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public CompoundTag component(
            ResourceLocation key
    ) {
        CompoundTag value =
                components.get(
                        key
                );

        return value == null
                ? new CompoundTag()
                : value.copy();
    }

    public boolean hasComponent(
            ResourceLocation key
    ) {
        return components.containsKey(
                key
        );
    }

    public void putComponent(
            ResourceLocation key,
            CompoundTag value
    ) {
        components.put(
                key,
                value.copy()
        );

        setDirty();
    }

    public void removeComponent(
            ResourceLocation key
    ) {
        if (components.remove(
                key
        ) != null) {
            setDirty();
        }
    }

    public int eventCount() {
        return events.size();
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putInt(
                "SchemaVersion",
                SCHEMA_VERSION
        );

        tag.putLong(
                "NextSequence",
                nextSequence
        );

        ListTag eventList =
                new ListTag();

        for (WorldEvent event :
                events) {
            eventList.add(
                    event.save()
            );
        }

        tag.put(
                "Events",
                eventList
        );

        ListTag componentList =
                new ListTag();

        for (Map.Entry<ResourceLocation, CompoundTag> entry :
                components.entrySet()) {
            CompoundTag row =
                    new CompoundTag();

            row.putString(
                    "Key",
                    entry.getKey()
                            .toString()
            );

            row.put(
                    "Value",
                    entry.getValue()
                            .copy()
            );

            componentList.add(
                    row
            );
        }

        tag.put(
                "Components",
                componentList
        );

        return tag;
    }

    public static WorldStateData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        WorldStateData data =
                new WorldStateData();

        data.nextSequence =
                Math.max(
                        1L,
                        tag.getLong(
                                "NextSequence"
                        )
                );

        ListTag eventList =
                tag.getList(
                        "Events",
                        Tag.TAG_COMPOUND
                );

        long highestSequence =
                0L;

        for (int index = 0;
             index < eventList.size();
             index++) {
            WorldEvent event =
                    WorldEvent.load(
                            eventList.getCompound(
                                    index
                            )
                    );

            if (event == null) {
                continue;
            }

            data.events.addLast(
                    event
            );

            highestSequence =
                    Math.max(
                            highestSequence,
                            event.sequence()
                    );
        }

        while (data.events.size() > MAX_EVENTS) {
            data.events.removeFirst();
        }

        data.nextSequence =
                Math.max(
                        data.nextSequence,
                        highestSequence + 1L
                );

        ListTag componentList =
                tag.getList(
                        "Components",
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < componentList.size();
             index++) {
            CompoundTag row =
                    componentList.getCompound(
                            index
                    );

            ResourceLocation key =
                    ResourceLocation.tryParse(
                            row.getString(
                                    "Key"
                            )
                    );

            if (key == null
                    || !row.contains(
                    "Value",
                    Tag.TAG_COMPOUND
            )) {
                continue;
            }

            data.components.put(
                    key,
                    row.getCompound(
                            "Value"
                    ).copy()
            );
        }

        return data;
    }
}
