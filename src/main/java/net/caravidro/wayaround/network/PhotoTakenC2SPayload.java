package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.media.PhotoData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PhotoTakenC2SPayload(
        String photoId,
        long takenAt,
        int x,
        int y,
        int z
) implements CustomPacketPayload {

    public static final Type<PhotoTakenC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "photo_taken_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PhotoTakenC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.photoId(), 64);
                        buf.writeLong(payload.takenAt());
                        buf.writeInt(payload.x());
                        buf.writeInt(payload.y());
                        buf.writeInt(payload.z());
                    },
                    buf ->
                            new PhotoTakenC2SPayload(
                                    buf.readUtf(64),
                                    buf.readLong(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            PhotoTakenC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {

                        return;
                    }

                    try {
                        UUID.fromString(
                                payload.photoId()
                        );
                    } catch (Exception exception) {
                        return;
                    }

                    if (!MediaInventory.consumeOne(
                            player,
                            MediaContent.PHOTO_PAPER.get()
                    )) {
                        return;
                    }

                    ItemStack photo =
                            new ItemStack(
                                    MediaContent.PHOTO.get()
                            );

                    PhotoData.write(
                            photo,
                            payload.photoId(),
                            payload.takenAt(),
                            payload.x(),
                            payload.y(),
                            payload.z()
                    );

                    MediaInventory.giveOrDrop(
                            player,
                            photo
                    );

                    WayAroundAdvancements.mediaPhoto(
                            player
                    );
                }
        );
    }
}
