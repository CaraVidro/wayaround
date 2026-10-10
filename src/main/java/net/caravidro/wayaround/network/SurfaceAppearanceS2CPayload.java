package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.appearance.SurfaceAppearanceClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sparse, bounded surface appearance sync; never transmit textures or pixels. */
public record SurfaceAppearanceS2CPayload(long[] positions, byte[] corrosion)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 96;
    public static final Type<SurfaceAppearanceS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID, "surface_appearance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SurfaceAppearanceS2CPayload> STREAM_CODEC =
            StreamCodec.of(SurfaceAppearanceS2CPayload::write, SurfaceAppearanceS2CPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, SurfaceAppearanceS2CPayload p) {
        int size = Math.min(MAX_ENTRIES, Math.min(p.positions.length, p.corrosion.length));
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeLong(p.positions[i]);
            buf.writeByte(p.corrosion[i]);
        }
    }

    private static SurfaceAppearanceS2CPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid surface appearance size: " + size);
        }
        long[] positions = new long[size];
        byte[] corrosion = new byte[size];
        for (int i = 0; i < size; i++) {
            positions[i] = buf.readLong();
            corrosion[i] = buf.readByte();
        }
        return new SurfaceAppearanceS2CPayload(positions, corrosion);
    }

    public static void handle(SurfaceAppearanceS2CPayload payload, IPayloadContext ctx) {
        if (payload.positions.length != payload.corrosion.length
                || payload.positions.length > MAX_ENTRIES) return;
        ctx.enqueueWork(() -> SurfaceAppearanceClientCache.receive(
                payload.positions, payload.corrosion));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
