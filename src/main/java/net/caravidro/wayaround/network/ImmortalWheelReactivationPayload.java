package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ImmortalWheelReactivationPayload(
        UUID owner,
        double x,
        double y,
        double z,
        int steps
) implements CustomPacketPayload {

    public static final Type<ImmortalWheelReactivationPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "immortal_wheel_reactivation"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ImmortalWheelReactivationPayload
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
                        buf.writeVarInt(
                                payload.steps()
                        );
                    },
                    buf ->
                            new ImmortalWheelReactivationPayload(
                                    buf.readUUID(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            ImmortalWheelReactivationPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleImmortalWheelReactivation(
                payload,
                context
        );
    }
}
