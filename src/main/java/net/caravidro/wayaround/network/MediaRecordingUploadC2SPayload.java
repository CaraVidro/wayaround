package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaTransferServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MediaRecordingUploadC2SPayload(
        String recordingId,
        long totalLength,
        long offset,
        byte[] data
) implements CustomPacketPayload {

    public static final Type<MediaRecordingUploadC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "media_recording_upload_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MediaRecordingUploadC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(
                                payload.recordingId(),
                                64
                        );
                        buf.writeLong(
                                payload.totalLength()
                        );
                        buf.writeLong(
                                payload.offset()
                        );
                        buf.writeByteArray(
                                payload.data()
                        );
                    },
                    buf ->
                            new MediaRecordingUploadC2SPayload(
                                    buf.readUtf(64),
                                    buf.readLong(),
                                    buf.readLong(),
                                    buf.readByteArray(
                                            24 * 1024
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MediaRecordingUploadC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {

                        MediaTransferServer.acceptUpload(
                                player,
                                payload.recordingId(),
                                payload.totalLength(),
                                payload.offset(),
                                payload.data()
                        );
                    }
                }
        );
    }
}
