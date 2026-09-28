package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TopHatStateS2CPayload(
        UUID player,
        long warningUntil,
        long adjustUntil,
        float instability
) implements CustomPacketPayload {

    public static final Type<TopHatStateS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "top_hat_state_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TopHatStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.player()
                        );

                        buf.writeLong(
                                payload.warningUntil()
                        );

                        buf.writeLong(
                                payload.adjustUntil()
                        );

                        buf.writeFloat(
                                payload.instability()
                        );
                    },
                    buf ->
                            new TopHatStateS2CPayload(
                                    buf.readUUID(),
                                    buf.readLong(),
                                    buf.readLong(),
                                    buf.readFloat()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TopHatStateS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleTopHatState(
                payload,
                context
        );
    }
}
