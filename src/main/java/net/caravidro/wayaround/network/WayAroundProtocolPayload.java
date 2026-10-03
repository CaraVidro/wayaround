package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tiny mandatory channel used only to prove that both endpoints speak the same
 * WayAround network protocol.
 *
 * <p>Feature payloads are negotiated as optional channels. Keeping this marker
 * mandatory preserves a hard protocol-version gate without making every single
 * gameplay packet an all-or-nothing login requirement.</p>
 */
public final class WayAroundProtocolPayload implements CustomPacketPayload {

    public static final WayAroundProtocolPayload INSTANCE =
            new WayAroundProtocolPayload();

    public static final Type<WayAroundProtocolPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "protocol"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WayAroundProtocolPayload
            > STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public WayAroundProtocolPayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    return INSTANCE;
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        WayAroundProtocolPayload payload
                ) {
                    // Intentionally empty. The channel/version is the contract.
                }
            };

    private WayAroundProtocolPayload() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            WayAroundProtocolPayload payload,
            IPayloadContext context
    ) {
        // Marker only; it is never intentionally sent during normal gameplay.
    }
}
