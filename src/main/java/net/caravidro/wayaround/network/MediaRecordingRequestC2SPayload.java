package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaTransferServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MediaRecordingRequestC2SPayload(
        String recordingId
) implements CustomPacketPayload {

    public static final Type<MediaRecordingRequestC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "media_recording_request_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MediaRecordingRequestC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeUtf(
                                    payload.recordingId(),
                                    64
                            ),
                    buf ->
                            new MediaRecordingRequestC2SPayload(
                                    buf.readUtf(64)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MediaRecordingRequestC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {

                        MediaTransferServer.request(
                                player,
                                payload.recordingId()
                        );
                    }
                }
        );
    }
}
