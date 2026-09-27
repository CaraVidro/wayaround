package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ImmortalWheelVisualPayload(
        UUID owner,
        byte action,
        String family,
        int progress,
        int steps
) implements CustomPacketPayload {

    public static final byte PRESENCE = 1;
    public static final byte HIT = 2;
    public static final byte SPIN = 3;
    public static final byte REMOVE = 4;

    public static final Type<ImmortalWheelVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "immortal_wheel_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ImmortalWheelVisualPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.owner()
                        );

                        buf.writeByte(
                                payload.action()
                        );

                        buf.writeUtf(
                                payload.family(),
                                32
                        );

                        buf.writeVarInt(
                                payload.progress()
                        );

                        buf.writeVarInt(
                                payload.steps()
                        );
                    },
                    buf ->
                            new ImmortalWheelVisualPayload(
                                    buf.readUUID(),
                                    buf.readByte(),
                                    buf.readUtf(
                                            32
                                    ),
                                    buf.readVarInt(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            ImmortalWheelVisualPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleImmortalWheel(payload, context);
    }
}
