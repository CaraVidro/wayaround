package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlacedCameraPickupS2CPayload()
        implements CustomPacketPayload {

    public static final Type<PlacedCameraPickupS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "placed_camera_pickup_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlacedCameraPickupS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                    },
                    buf ->
                            new PlacedCameraPickupS2CPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            PlacedCameraPickupS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handlePlacedCameraPickup(payload, context);
    }
}
