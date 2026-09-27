package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlizzardStatePayload(
        float intensity
) implements CustomPacketPayload {

    public static final Type<BlizzardStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blizzard_state"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlizzardStatePayload
    > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) ->
                            buffer.writeFloat(
                                    payload.intensity()
                            ),

                    buffer ->
                            new BlizzardStatePayload(
                                    buffer.readFloat()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlizzardStatePayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBlizzard(payload, context);
    }
}