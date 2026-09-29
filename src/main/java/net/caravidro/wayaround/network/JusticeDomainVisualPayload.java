package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JusticeDomainVisualPayload(
        UUID owner,
        byte action,
        double x,
        double y,
        double z,
        float radius,
        int durationTicks
) implements CustomPacketPayload {

    public static final byte OPEN = 1;
    public static final byte ENTER = 2;
    public static final byte CLOSE = 3;

    public static final Type<JusticeDomainVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "justice_domain_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            JusticeDomainVisualPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.owner());
                        buf.writeByte(payload.action());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                        buf.writeFloat(payload.radius());
                        buf.writeVarInt(payload.durationTicks());
                    },
                    buf -> new JusticeDomainVisualPayload(
                            buf.readUUID(),
                            buf.readByte(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static JusticeDomainVisualPayload open(
            UUID owner,
            Vec3 center,
            float radius,
            int durationTicks
    ) {
        return new JusticeDomainVisualPayload(
                owner,
                OPEN,
                center.x,
                center.y,
                center.z,
                radius,
                durationTicks
        );
    }

    public static JusticeDomainVisualPayload enter(
            UUID owner
    ) {
        return new JusticeDomainVisualPayload(
                owner,
                ENTER,
                0.0,
                0.0,
                0.0,
                0.0F,
                0
        );
    }

    public static JusticeDomainVisualPayload close(
            UUID owner,
            Vec3 center,
            float radius
    ) {
        return new JusticeDomainVisualPayload(
                owner,
                CLOSE,
                center.x,
                center.y,
                center.z,
                radius,
                0
        );
    }

    public static void handle(
            JusticeDomainVisualPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleJusticeDomainVisual(
                payload,
                context
        );
    }
}
