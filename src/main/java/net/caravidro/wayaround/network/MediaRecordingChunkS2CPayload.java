package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MediaRecordingChunkS2CPayload(
        String recordingId,
        long totalLength,
        long offerToken,
        long offset,
        byte[] data
) implements CustomPacketPayload {

    public static final Type<MediaRecordingChunkS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "media_recording_chunk_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MediaRecordingChunkS2CPayload
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
                                payload.offerToken()
                        );
                        buf.writeLong(
                                payload.offset()
                        );
                        buf.writeByteArray(
                                payload.data()
                        );
                    },
                    buf ->
                            new MediaRecordingChunkS2CPayload(
                                    buf.readUtf(64),
                                    buf.readLong(),
                                    buf.readLong(),
                                    buf.readLong(),
                                    buf.readByteArray(
                                            MediaNetworkLimits.DOWNLOAD_CHUNK
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MediaRecordingChunkS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleMediaChunk(
                payload,
                context
        );
    }
}
