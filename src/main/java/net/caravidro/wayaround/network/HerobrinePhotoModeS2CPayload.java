package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HerobrinePhotoModeS2CPayload(
        boolean enabled
) implements CustomPacketPayload {

    public static final Type<HerobrinePhotoModeS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "herobrine_photo_mode_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            HerobrinePhotoModeS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeBoolean(
                                    payload.enabled()
                            ),
                    buf ->
                            new HerobrinePhotoModeS2CPayload(
                                    buf.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            HerobrinePhotoModeS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleHerobrinePhotoMode(
                payload,
                context
        );
    }
}
