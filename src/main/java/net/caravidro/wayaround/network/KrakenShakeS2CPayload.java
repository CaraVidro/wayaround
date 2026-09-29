package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Common-safe camera impulse for Kraken environmental events.
 */
public record KrakenShakeS2CPayload(
        int ticks,
        float strength
) implements CustomPacketPayload {

    public static final Type<KrakenShakeS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "kraken_shake"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            KrakenShakeS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.ticks());
                        buf.writeFloat(payload.strength());
                    },
                    buf -> new KrakenShakeS2CPayload(
                            buf.readVarInt(),
                            buf.readFloat()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            KrakenShakeS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleKrakenShake(
                payload,
                context
        );
    }
}
