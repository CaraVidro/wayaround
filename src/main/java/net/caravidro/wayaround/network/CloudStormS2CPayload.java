package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sparse storm events; the cloud body itself remains deterministic. */
public record CloudStormS2CPayload(long cellId,double x,double y,double z,int groundY,int kind,float strength)
        implements CustomPacketPayload {
    public static final Type<CloudStormS2CPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"cloud_storm"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CloudStormS2CPayload> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeLong(p.cellId);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);
                b.writeInt(p.groundY);b.writeVarInt(p.kind);b.writeFloat(p.strength);},
            b->new CloudStormS2CPayload(b.readLong(),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt(),b.readVarInt(),b.readFloat()));
    public boolean isSane(){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)
            && Math.abs(x)<=30_000_000&&Math.abs(z)<=30_000_000&&y>=-2048&&y<=2048
            && groundY>=-2048&&groundY<=2048&&kind>=0&&kind<=2&&Float.isFinite(strength)&&strength>=0&&strength<=1;}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
