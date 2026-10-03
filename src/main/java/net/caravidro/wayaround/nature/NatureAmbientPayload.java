package net.caravidro.wayaround.nature;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sparse scene start, or bounded 40 ms parrot voice frame. */
public record NatureAmbientPayload(int kind,int entityId,long seed,double x,double y,double z,byte[] pcm) implements CustomPacketPayload {
    public static final Type<NatureAmbientPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","nature_ambient"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NatureAmbientPayload> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.kind);b.writeVarInt(p.entityId);b.writeLong(p.seed);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeByteArray(p.pcm);},b->new NatureAmbientPayload(b.readVarInt(),b.readVarInt(),b.readLong(),b.readDouble(),b.readDouble(),b.readDouble(),b.readByteArray(4096)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public boolean sane(){return kind>=0&&kind<=1&&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&pcm.length<=4096&&(pcm.length&1)==0;}
}
