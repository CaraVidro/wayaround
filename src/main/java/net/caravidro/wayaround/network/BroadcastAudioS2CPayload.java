package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroadcastAudioS2CPayload(
        BlockPos receiver,
        byte[] pcm,
        float quality,
        float volume,
        int effect
) implements CustomPacketPayload {
    public static final Type<BroadcastAudioS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "broadcast_audio_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastAudioS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.receiver());
                        buf.writeByteArray(payload.pcm());
                        buf.writeFloat(payload.quality());
                        buf.writeFloat(payload.volume());
                        buf.writeVarInt(payload.effect());
                    },
                    buf -> new BroadcastAudioS2CPayload(
                            buf.readBlockPos(),
                            buf.readByteArray(VoiceConstants.MAX_PACKET_BYTES),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(
            BroadcastAudioS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBroadcastAudio(
                payload,
                context
        );
    }
}
