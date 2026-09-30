package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlueprintLabelOpenS2CPayload(
        String blueprintId,
        String title,
        boolean showCoordinates,
        boolean showDateTime
) implements CustomPacketPayload {

    public static final Type<BlueprintLabelOpenS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blueprint_label_open_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlueprintLabelOpenS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(
                                payload.blueprintId(),
                                64
                        );
                        buf.writeUtf(
                                payload.title(),
                                64
                        );
                        buf.writeBoolean(
                                payload.showCoordinates()
                        );
                        buf.writeBoolean(
                                payload.showDateTime()
                        );
                    },
                    buf -> new BlueprintLabelOpenS2CPayload(
                            buf.readUtf(64),
                            buf.readUtf(64),
                            buf.readBoolean(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlueprintLabelOpenS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBlueprintLabelOpen(
                payload,
                context
        );
    }
}
