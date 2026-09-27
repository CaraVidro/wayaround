package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecordingStartResultS2CPayload(
        boolean allowed,
        String messageKey
) implements CustomPacketPayload {

    public static final Type<RecordingStartResultS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "recording_start_result_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RecordingStartResultS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(
                                payload.allowed()
                        );
                        buf.writeUtf(
                                payload.messageKey(),
                                128
                        );
                    },
                    buf ->
                            new RecordingStartResultS2CPayload(
                                    buf.readBoolean(),
                                    buf.readUtf(128)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            RecordingStartResultS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleRecordingStart(payload, context);
    }
}
