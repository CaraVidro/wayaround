package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.client.TapeLabelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecordingReadyS2CPayload(
        String recordingId,
        String defaultTitle,
        boolean vhs
) implements CustomPacketPayload {

    public static final Type<RecordingReadyS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "recording_ready_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RecordingReadyS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.recordingId(), 64);
                        buf.writeUtf(payload.defaultTitle(), 64);
                        buf.writeBoolean(payload.vhs());
                    },
                    buf ->
                            new RecordingReadyS2CPayload(
                                    buf.readUtf(64),
                                    buf.readUtf(64),
                                    buf.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            RecordingReadyS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> Minecraft.getInstance()
                        .setScreen(
                                new TapeLabelScreen(
                                        payload.recordingId(),
                                        payload.defaultTitle(),
                                        payload.vhs()
                                )
                        )
        );
    }
}
