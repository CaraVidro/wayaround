package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TukunaMarkRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A short pulse, not a persistent state. Repeated voice frames keep the mouth
 * alive; silence naturally lets it close and disappear client-side.
 */
public record TukunaSpeechVisualS2CPayload(
        UUID body
) implements CustomPacketPayload {

    public static final Type<TukunaSpeechVisualS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "tukuna_speech_visual_s2c"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, TukunaSpeechVisualS2CPayload>
            STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeUUID(
                                    payload.body()
                            ),
                    buf ->
                            new TukunaSpeechVisualS2CPayload(
                                    buf.readUUID()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TukunaSpeechVisualS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () -> TukunaMarkRenderer.receiveSpeech(
                        payload.body()
                )
        );
    }
}
