package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Seven small integers per second, no terrain edits or per-segment entities. */
public record KrakenSceneS2CPayload(int kind, int x, int surface, int z, int dx, int dz, int age)
        implements CustomPacketPayload {
    public static final Type<KrakenSceneS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "kraken_scene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KrakenSceneS2CPayload> STREAM_CODEC = StreamCodec.of(
            (b,p) -> { b.writeVarInt(p.kind); b.writeInt(p.x); b.writeInt(p.surface); b.writeInt(p.z);
                b.writeVarInt(p.dx); b.writeVarInt(p.dz); b.writeVarInt(p.age); },
            b -> new KrakenSceneS2CPayload(b.readVarInt(),b.readInt(),b.readInt(),b.readInt(),
                    b.readVarInt(),b.readVarInt(),b.readVarInt()));
    public boolean isSane() { return kind >= -1 && kind <= 4 && Math.abs((long)x) <= 30_000_000
            && Math.abs((long)z) <= 30_000_000 && surface >= -2048 && surface <= 2048
            && dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1 && age >= 0 && age <= 340; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
