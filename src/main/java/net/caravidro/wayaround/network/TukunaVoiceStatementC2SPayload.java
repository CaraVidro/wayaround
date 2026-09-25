package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TukunaVoiceStatementC2SPayload(
        String transcript
) implements CustomPacketPayload {

    private static final int MAX_TEXT =
            512;

    public static final Type<TukunaVoiceStatementC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "tukuna_voice_statement_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TukunaVoiceStatementC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeUtf(
                                    payload.transcript(),
                                    MAX_TEXT
                            ),
                    buf ->
                            new TukunaVoiceStatementC2SPayload(
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
            TukunaVoiceStatementC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {

                        TukunaManager.onVoiceStatement(
                                player,
                                payload.transcript()
                        );
                    }
                }
        );
    }
}
