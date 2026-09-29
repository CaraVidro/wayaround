package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.AlexaBlock;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AlexaVoiceCommandC2SPayload(
        String transcript
) implements CustomPacketPayload {

    private static final int MAX_TEXT =
            256;

    public static final Type<AlexaVoiceCommandC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "alexa_voice_command_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AlexaVoiceCommandC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeUtf(
                                    payload.transcript(),
                                    MAX_TEXT
                            ),
                    buf ->
                            new AlexaVoiceCommandC2SPayload(
                                    buf.readUtf(
                                            MAX_TEXT
                                    )
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AlexaVoiceCommandC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        AlexaBlock.handleVoice(
                                player,
                                payload.transcript()
                        );
                    }
                }
        );
    }
}
