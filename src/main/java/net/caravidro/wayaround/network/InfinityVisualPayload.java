package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InfinityVisualPayload(
        UUID owner,
        double x,
        double y,
        double z,
        float confidence,
        float radius
) implements CustomPacketPayload {

    public static final Type<InfinityVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "infinity_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            InfinityVisualPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.owner()
                        );
                        buf.writeDouble(
                                payload.x()
                        );
                        buf.writeDouble(
                                payload.y()
                        );
                        buf.writeDouble(
                                payload.z()
                        );
                        buf.writeFloat(
                                payload.confidence()
                        );
                        buf.writeFloat(
                                payload.radius()
                        );
                    },
                    buf ->
                            new InfinityVisualPayload(
                                    buf.readUUID(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readFloat(),
                                    buf.readFloat()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            InfinityVisualPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleInfinityVisual(payload, context);
    }
}
