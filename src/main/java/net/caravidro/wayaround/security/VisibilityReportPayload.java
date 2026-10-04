package net.caravidro.wayaround.security;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
/** No image/PNG, world ore coordinates, file paths or selectable player identity are transmitted. */
public record VisibilityReportPayload(long nonce,boolean available,double x,double y,double z,float fx,float fy,float fz,
        float[] directions,float[] distances,byte[] brightness) implements CustomPacketPayload {
    public VisibilityReportPayload {
        if(available&&(directions.length!=VisibilityMath.SAMPLES*3||distances.length!=VisibilityMath.SAMPLES||brightness.length!=VisibilityMath.SAMPLES))throw new IllegalArgumentException("Fixed probe count");
    }
    public static VisibilityReportPayload skipped(long nonce){return new VisibilityReportPayload(nonce,false,0,0,0,0,0,0,new float[0],new float[0],new byte[0]);}
    public static final Type<VisibilityReportPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","visibility_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf,VisibilityReportPayload> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeLong(p.nonce);b.writeBoolean(p.available);if(!p.available)return;
        b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeFloat(p.fx);b.writeFloat(p.fy);b.writeFloat(p.fz);
        for(float f:p.directions)b.writeFloat(f);for(float f:p.distances)b.writeFloat(f);b.writeBytes(p.brightness);
    },b->{long nonce=b.readLong();if(!b.readBoolean())return skipped(nonce);
        double x=b.readDouble(),y=b.readDouble(),z=b.readDouble();float fx=b.readFloat(),fy=b.readFloat(),fz=b.readFloat();
        float[] directions=new float[VisibilityMath.SAMPLES*3],distances=new float[VisibilityMath.SAMPLES];byte[] brightness=new byte[VisibilityMath.SAMPLES];
        for(int i=0;i<directions.length;i++)directions[i]=b.readFloat();for(int i=0;i<distances.length;i++)distances[i]=b.readFloat();b.readBytes(brightness);
        return new VisibilityReportPayload(nonce,true,x,y,z,fx,fy,fz,directions,distances,brightness);
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(VisibilityReportPayload p,IPayloadContext c){c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)VisibilityAuditService.receive(player,p);});}
}
