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

public record VoidDomainVisualPayload(
        UUID owner,
        byte action,
        double x,
        double y,
        double z,
        float radius,
        int durationTicks,
        boolean trapped
) implements CustomPacketPayload {

    public static final byte OPEN = 1;
    public static final byte ENTER = 2;
    public static final byte CLOSE = 3;
    public static final byte PREPARE = 4;

    public static final Type<VoidDomainVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "void_domain_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VoidDomainVisualPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.owner()
                        );
                        buf.writeByte(
                                payload.action()
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
                                payload.radius()
                        );
                        buf.writeVarInt(
                                payload.durationTicks()
                        );
                        buf.writeBoolean(
                                payload.trapped()
                        );
                    },
                    buf ->
                            new VoidDomainVisualPayload(
                                    buf.readUUID(),
                                    buf.readByte(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readFloat(),
                                    buf.readVarInt(),
                                    buf.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VoidDomainVisualPayload open(
            UUID owner,
            Vec3 center,
            float radius,
            int durationTicks
    ) {
        return new VoidDomainVisualPayload(
                owner,
                OPEN,
                center.x,
                center.y,
                center.z,
                radius,
                durationTicks,
                false
        );
    }

    public static VoidDomainVisualPayload prepare(
            UUID owner,
            int formationTicks,
            boolean trapped
    ) {
        return new VoidDomainVisualPayload(
                owner,
                PREPARE,
                0.0,
                0.0,
                0.0,
                0.0F,
                formationTicks,
                trapped
        );
    }

    public static VoidDomainVisualPayload enter(
            UUID owner,
            Vec3 center,
            float radius,
            int durationTicks,
            boolean trapped
    ) {
        return new VoidDomainVisualPayload(
                owner,
                ENTER,
                center.x,
                center.y,
                center.z,
                radius,
                durationTicks,
                trapped
        );
    }

    public static VoidDomainVisualPayload close(
            UUID owner
    ) {
        return new VoidDomainVisualPayload(
                owner,
                CLOSE,
                0.0,
                0.0,
                0.0,
                0.0F,
                0,
                false
        );
    }

    public static void handle(
            VoidDomainVisualPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleVoidDomainVisual(
                payload,
                context
        );
    }
}
