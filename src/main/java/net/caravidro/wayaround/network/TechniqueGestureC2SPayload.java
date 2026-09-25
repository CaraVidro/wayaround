package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TechniqueGestureC2SPayload(
        byte action
) implements CustomPacketPayload {

    public static final byte DESMARTELAR_START = 1;
    public static final byte DESMARTELAR_RELEASE = 2;
    public static final byte DESMARTELAR_CANCEL = 3;

    public static final Type<TechniqueGestureC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "technique_gesture"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TechniqueGestureC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    payload.action()
                            ),
                    buf ->
                            new TechniqueGestureC2SPayload(
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TechniqueGestureC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {
                        return;
                    }

                    switch (payload.action()) {
                        case DESMARTELAR_START ->
                                TukunaManager.beginManualDesmartelar(
                                        player
                                );

                        case DESMARTELAR_RELEASE ->
                                TukunaManager.releaseManualDesmartelar(
                                        player
                                );

                        case DESMARTELAR_CANCEL ->
                                TukunaManager.cancelManualDesmartelar(
                                        player
                                );

                        default -> {
                        }
                    }
                }
        );
    }
}
