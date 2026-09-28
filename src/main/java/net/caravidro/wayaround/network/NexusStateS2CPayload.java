package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NexusStateS2CPayload(
        boolean active,
        boolean complete,
        float progress,
        int wave,
        BlockPos reactor
) implements CustomPacketPayload {

    public static final Type<NexusStateS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "nexus_state_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            NexusStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.active());
                        buf.writeBoolean(payload.complete());
                        buf.writeFloat(payload.progress());
                        buf.writeVarInt(payload.wave());
                        buf.writeBlockPos(payload.reactor());
                    },
                    buf ->
                            new NexusStateS2CPayload(
                                    buf.readBoolean(),
                                    buf.readBoolean(),
                                    buf.readFloat(),
                                    buf.readVarInt(),
                                    buf.readBlockPos()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            NexusStateS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleNexusState(
                payload,
                context
        );
    }
}
