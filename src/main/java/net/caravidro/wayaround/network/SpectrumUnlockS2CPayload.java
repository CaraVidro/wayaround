package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpectrumUnlockS2CPayload(UUID player, int mask) implements CustomPacketPayload {
    public static final Type<SpectrumUnlockS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "spectrum_unlock_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpectrumUnlockS2CPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeUUID(payload.player());
                buf.writeVarInt(payload.mask());
            }, buf -> new SpectrumUnlockS2CPayload(buf.readUUID(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpectrumUnlockS2CPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> SpectrumAccess.applyClientUnlocks(payload.player(), payload.mask()));
    }
}
