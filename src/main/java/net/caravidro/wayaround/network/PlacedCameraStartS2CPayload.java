package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlacedCameraStartS2CPayload(
        BlockPos position,
        Direction facing
) implements CustomPacketPayload {

    public static final Type<PlacedCameraStartS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "placed_camera_start_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlacedCameraStartS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(
                                payload.position()
                        );

                        buf.writeEnum(
                                payload.facing()
                        );
                    },
                    buf ->
                            new PlacedCameraStartS2CPayload(
                                    buf.readBlockPos(),
                                    buf.readEnum(
                                            Direction.class
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            PlacedCameraStartS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handlePlacedCameraStart(payload, context);
    }
}
