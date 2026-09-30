package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client pilot input for the vertical-only abyss capsule. */
public record DeepSeaCapsuleControlC2SPayload(byte vertical) implements CustomPacketPayload {
    public static final Type<DeepSeaCapsuleControlC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "deep_sea_capsule_control_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeepSeaCapsuleControlC2SPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeByte(payload.vertical()),
                    buf -> new DeepSeaCapsuleControlC2SPayload(buf.readByte())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepSeaCapsuleControlC2SPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.getVehicle() instanceof DeepSeaCapsuleEntity capsule)) {
                return;
            }

            float input = Math.max(-1.0F, Math.min(1.0F, payload.vertical()));
            capsule.setVerticalInput(input);
        });
    }
}
