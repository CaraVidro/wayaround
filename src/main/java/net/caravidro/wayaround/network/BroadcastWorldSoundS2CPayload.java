package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroadcastWorldSoundS2CPayload(
        BlockPos receiver,
        String sound,
        String source,
        float volume,
        float pitch,
        float quality,
        int effect
) implements CustomPacketPayload {
    public static final Type<BroadcastWorldSoundS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "broadcast_world_sound_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastWorldSoundS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.receiver());
                        buf.writeUtf(payload.sound(), 160);
                        buf.writeUtf(payload.source(), 24);
                        buf.writeFloat(payload.volume());
                        buf.writeFloat(payload.pitch());
                        buf.writeFloat(payload.quality());
                        buf.writeVarInt(payload.effect());
                    },
                    buf -> new BroadcastWorldSoundS2CPayload(
                            buf.readBlockPos(),
                            buf.readUtf(160),
                            buf.readUtf(24),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(
            BroadcastWorldSoundS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBroadcastWorldSound(
                payload,
                context
        );
    }
}
