package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.blue.BlueManager;
import net.caravidro.wayaround.content.WayAroundContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VoiceIntentC2SPayload(
        byte intent
) implements CustomPacketPayload {

    public static final byte BLUE = 1;

    public static final Type<VoiceIntentC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "voice_intent_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            VoiceIntentC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    payload.intent()
                            ),
                    buf ->
                            new VoiceIntentC2SPayload(
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            VoiceIntentC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)
                            || payload.intent()
                            != BLUE) {

                        return;
                    }

                    if (!hasBlue(
                            player
                    )) {
                        return;
                    }

                    BlueManager.invokeFromVoice(
                            player
                    );
                }
        );
    }

    private static boolean hasBlue(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(
                                    slot
                            );

            if (stack.is(
                    WayAroundContent.BLUE.get()
            )) {
                return true;
            }
        }

        return false;
    }
}
