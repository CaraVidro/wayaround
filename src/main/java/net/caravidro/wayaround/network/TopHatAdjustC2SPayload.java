package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.TopHatManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TopHatAdjustC2SPayload()
        implements CustomPacketPayload {

    public static final Type<TopHatAdjustC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "top_hat_adjust_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TopHatAdjustC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    0
                            ),
                    buf -> {
                        buf.readByte();
                        return new TopHatAdjustC2SPayload();
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TopHatAdjustC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        TopHatManager.adjust(
                                player
                        );
                    }
                }
        );
    }
}
