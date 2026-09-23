package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.blue.BlueManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlueScrollPayload(double amount)
        implements CustomPacketPayload {

    public static final Type<BlueScrollPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blue_scroll"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlueScrollPayload
    > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeDouble(payload.amount()),
                    buf ->
                            new BlueScrollPayload(
                                    buf.readDouble()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlueScrollPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        BlueManager.scroll(
                                player,
                                payload.amount()
                        );
                    }
                }
        );
    }
}
