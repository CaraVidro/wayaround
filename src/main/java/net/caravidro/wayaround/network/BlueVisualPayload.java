package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Lightweight client snapshot for one summoned Blue.
 *
 * The server owns physics/destruction. Clients only use this for rendering,
 * spatial audio, camera shake, darkness and cloud holes.
 */
public record BlueVisualPayload(
        UUID owner,
        double x,
        double y,
        double z,
        float power,
        float radius,
        byte mode
) implements CustomPacketPayload {

    public static final byte ACTIVE = 1;
    public static final byte LAUNCHED = 2;
    public static final byte COLLAPSING = 3;

    public static final Type<BlueVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blue_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlueVisualPayload
    > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.owner());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeFloat(payload.power());
                        buf.writeFloat(payload.radius());
                        buf.writeByte(payload.mode());
                    },
                    buf ->
                            new BlueVisualPayload(
                                    buf.readUUID(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readFloat(),
                                    buf.readFloat(),
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlueVisualPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBlueVisual(payload, context);
    }
}
