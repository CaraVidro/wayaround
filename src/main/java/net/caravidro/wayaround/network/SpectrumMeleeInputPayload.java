package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.spectrum.SpectrumMeleeManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpectrumMeleeInputPayload(
        byte action
) implements CustomPacketPayload {

    public static final byte ATTACK = 0;
    public static final byte BLOCK_START = 1;
    public static final byte BLOCK_END = 2;

    public static final Type<SpectrumMeleeInputPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "spectrum_melee_input"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SpectrumMeleeInputPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    payload.action()
                            ),
                    buf ->
                            new SpectrumMeleeInputPayload(
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            SpectrumMeleeInputPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        SpectrumMeleeManager.handleInput(
                                player,
                                payload.action()
                        );
                    }
                }
        );
    }
}
