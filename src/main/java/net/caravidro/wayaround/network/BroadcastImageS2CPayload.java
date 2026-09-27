package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroadcastImageS2CPayload(
        BlockPos television,
        int width,
        int height,
        byte[] rgb,
        float quality,
        int effect
) implements CustomPacketPayload {
    public static final Type<BroadcastImageS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "broadcast_image_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastImageS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.television());
                        buf.writeVarInt(payload.width());
                        buf.writeVarInt(payload.height());
                        buf.writeByteArray(payload.rgb());
                        buf.writeFloat(payload.quality());
                        buf.writeVarInt(payload.effect());
                    },
                    buf -> new BroadcastImageS2CPayload(
                            buf.readBlockPos(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readByteArray(
                                    MediaNetworkLimits.BROADCAST_RGB_BYTES
                            ),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(
            BroadcastImageS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBroadcastImage(
                payload,
                context
        );
    }
}
