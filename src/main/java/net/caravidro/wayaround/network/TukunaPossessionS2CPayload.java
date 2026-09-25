package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TukunaPossessionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TukunaPossessionS2CPayload(
        boolean active,
        boolean contractMusic
) implements CustomPacketPayload {

    public static final Type<TukunaPossessionS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "tukuna_possession_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TukunaPossessionS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                            buf.writeBoolean(
                                    payload.active()
                            );
                            buf.writeBoolean(payload.contractMusic());
                    },
                    buf ->
                            new TukunaPossessionS2CPayload(
                                    buf.readBoolean(), buf.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TukunaPossessionS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        TukunaPossessionClient.setPossessed(
                                payload.active(), payload.contractMusic()
                        )
        );
    }
}
