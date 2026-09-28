package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrouserPocketRetrieveC2SPayload()
        implements CustomPacketPayload {

    public static final Type<TrouserPocketRetrieveC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "trouser_pocket_retrieve_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TrouserPocketRetrieveC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                    },
                    buf ->
                            new TrouserPocketRetrieveC2SPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TrouserPocketRetrieveC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        AccessoryManager.requestPocketRetrieve(
                                player
                        );
                    }
                }
        );
    }
}
