package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.media.VhsData;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecordingFinishedC2SPayload(
        String recordingId,
        long durationMillis,
        long startedAtMillis,
        boolean dropOnGround,
        int x,
        int y,
        int z
) implements CustomPacketPayload {

    public static final Type<RecordingFinishedC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "recording_finished_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RecordingFinishedC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.recordingId(), 64);
                        buf.writeLong(payload.durationMillis());
                        buf.writeLong(payload.startedAtMillis());
                        buf.writeBoolean(payload.dropOnGround());
                        buf.writeInt(payload.x());
                        buf.writeInt(payload.y());
                        buf.writeInt(payload.z());
                    },
                    buf ->
                            new RecordingFinishedC2SPayload(
                                    buf.readUtf(64),
                                    buf.readLong(),
                                    buf.readLong(),
                                    buf.readBoolean(),
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
            RecordingFinishedC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {

                        return;
                    }

                    BroadcastManager.setHandheldCamera(
                            player,
                            false
                    );

                    if (!valid(payload)) {
                        return;
                    }

                    int serial =
                            MediaInventory
                                    .nextTapeSerial(
                                            player
                                    );

                    String defaultTitle =
                            "Fita #"
                                    + serial;

                    boolean madeVhs =
                            MediaInventory.consumeOne(
                                    player,
                                    MediaContent.BLANK_VHS.get()
                            );

                    ItemStack medium =
                            new ItemStack(
                                    madeVhs
                                            ? MediaContent.VHS.get()
                                            : MediaContent.EXPOSED_FILM_ROLL.get()
                            );

                    VhsData.write(
                            medium,
                            payload.recordingId(),
                            payload.durationMillis(),
                            payload.startedAtMillis(),
                            defaultTitle,
                            serial,
                            payload.x(),
                            payload.y(),
                            payload.z()
                    );

                    if (payload.dropOnGround()) {
                        player.drop(
                                medium,
                                false
                        );
                        return;
                    }

                    MediaInventory.giveOrDrop(
                            player,
                            medium
                    );

                    PacketDistributor.sendToPlayer(
                            player,
                            new RecordingReadyS2CPayload(
                                    payload.recordingId(),
                                    defaultTitle,
                                    madeVhs
                            )
                    );
                }
        );
    }

    private static boolean valid(
            RecordingFinishedC2SPayload payload
    ) {
        try {
            UUID.fromString(
                    payload.recordingId()
            );
        } catch (Exception exception) {
            return false;
        }

        return payload.durationMillis()
                >= 100L
                && payload.durationMillis()
                <= 190_000L
                && payload.startedAtMillis()
                > 0L;
    }
}
