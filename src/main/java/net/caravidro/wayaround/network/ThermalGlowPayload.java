package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ThermalGlowPayload(BlockPos pos, int ticks) implements CustomPacketPayload {
    public static final Type<ThermalGlowPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "thermal_glow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThermalGlowPayload> STREAM_CODEC = StreamCodec.of(
            (b,p) -> { b.writeBlockPos(p.pos); b.writeVarInt(p.ticks); }, b -> new ThermalGlowPayload(b.readBlockPos(), b.readVarInt()));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(
            ThermalGlowPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleThermalGlow(
                payload,
                context
        );
    }
}
