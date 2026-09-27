package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaTransferServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent only after the local player explicitly accepts the download prompt.
 */
public record MediaRecordingApproveC2SPayload(
        String recordingId
) implements CustomPacketPayload {

    public static final Type<MediaRecordingApproveC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "media_recording_approve_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MediaRecordingApproveC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeUtf(
                                    payload.recordingId(),
                                    64
                            ),
                    buf ->
                            new MediaRecordingApproveC2SPayload(
                                    buf.readUtf(
                                            64
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MediaRecordingApproveC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        MediaTransferServer.approveDownload(
                                player,
                                payload.recordingId()
                        );
                    }
                }
        );
    }
}
