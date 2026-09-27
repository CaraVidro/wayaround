package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.client.MediaTransferClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Metadata only. Receiving this packet never writes a file.
 */
public record MediaRecordingOfferS2CPayload(
        String recordingId,
        long totalLength
) implements CustomPacketPayload {

    public static final Type<MediaRecordingOfferS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "media_recording_offer_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MediaRecordingOfferS2CPayload
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
                    },
                    buf ->
                            new MediaRecordingOfferS2CPayload(
                                    buf.readUtf(
                                            64
                                    ),
                                    buf.readLong()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MediaRecordingOfferS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        MediaTransferClient.offer(
                                payload.recordingId(),
                                payload.totalLength()
                        )
        );
    }
}
