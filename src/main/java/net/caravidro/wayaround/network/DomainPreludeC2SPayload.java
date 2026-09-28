package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.domain.DomainIntroManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DomainPreludeC2SPayload(
        byte style
) implements CustomPacketPayload {

    public static final Type<DomainPreludeC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "domain_prelude_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DomainPreludeC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    payload.style()
                            ),
                    buf ->
                            new DomainPreludeC2SPayload(
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            DomainPreludeC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        DomainIntroManager.prepareVoice(
                                player,
                                payload.style()
                        );
                    }
                }
        );
    }
}
