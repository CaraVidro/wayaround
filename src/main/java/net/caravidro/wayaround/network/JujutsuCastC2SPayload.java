package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.jujutsu.JujutsuManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JujutsuCastC2SPayload(byte action) implements CustomPacketPayload {
    public static final Type<JujutsuCastC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "jujutsu_cast_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JujutsuCastC2SPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeByte(payload.action()),
                    buf -> new JujutsuCastC2SPayload(buf.readByte())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JujutsuCastC2SPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.action() != 0) return;
            if (context.player() instanceof ServerPlayer player) {
                JujutsuManager.cast(player);
            }
        });
    }
}
