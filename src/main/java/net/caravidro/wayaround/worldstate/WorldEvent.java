package net.caravidro.wayaround.worldstate;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable historical fact recorded by the Way Around world.
 *
 * <p>The event payload is intentionally namespaced and data-driven. Systems can
 * add context without forcing the World State Engine to know their internal
 * classes.</p>
 */
public record WorldEvent(
        long sequence,
        ResourceLocation type,
        ResourceLocation dimension,
        long position,
        long gameTime,
        long createdAtEpochMillis,
        @Nullable UUID actor,
        CompoundTag data
) {

    public WorldEvent {
        data = data == null
                ? new CompoundTag()
                : data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }

    public BlockPos blockPos() {
        return BlockPos.of(position);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putLong("Sequence", sequence);
        tag.putString("Type", type.toString());
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position);
        tag.putLong("GameTime", gameTime);
        tag.putLong("CreatedAt", createdAtEpochMillis);

        if (actor != null) {
            tag.putUUID("Actor", actor);
        }

        tag.put("Data", data.copy());
        return tag;
    }

    @Nullable
    public static WorldEvent load(CompoundTag tag) {
        ResourceLocation type =
                ResourceLocation.tryParse(
                        tag.getString("Type")
                );

        ResourceLocation dimension =
                ResourceLocation.tryParse(
                        tag.getString("Dimension")
                );

        if (type == null || dimension == null) {
            return null;
        }

        UUID actor =
                tag.hasUUID("Actor")
                        ? tag.getUUID("Actor")
                        : null;

        CompoundTag payload =
                tag.contains("Data")
                        ? tag.getCompound("Data")
                        : new CompoundTag();

        return new WorldEvent(
                tag.getLong("Sequence"),
                type,
                dimension,
                tag.getLong("Position"),
                tag.getLong("GameTime"),
                tag.getLong("CreatedAt"),
                actor,
                payload
        );
    }
}
