package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.caravidro.wayaround.voice.VoiceServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VoiceFrameC2SPayload(
        byte[] pcm
) implements CustomPacketPayload {

    public static final Type<VoiceFrameC2SPayload>
            TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "voice_frame_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VoiceFrameC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByteArray(
                                    payload.pcm()
                            ),
                    buf ->
                            new VoiceFrameC2SPayload(
                                    buf.readByteArray(
                                            VoiceConstants.MAX_PACKET_BYTES
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload>
    type() {
        return TYPE;
    }

    public static void handle(
            VoiceFrameC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer
                            player) {

                        VoiceServer.relay(
                                player,
                                payload.pcm()
                        );
                    }
                }
        );
    }
}
