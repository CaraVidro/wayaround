package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.caravidro.wayaround.voice.client.VoicePlayback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VoiceFrameS2CPayload(
        byte[] pcm
) implements CustomPacketPayload {

    public static final Type<VoiceFrameS2CPayload>
            TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "voice_frame_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VoiceFrameS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByteArray(
                                    payload.pcm()
                            ),
                    buf ->
                            new VoiceFrameS2CPayload(
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
            VoiceFrameS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        VoicePlayback.enqueue(
                                payload.pcm()
                        )
        );
    }
}
