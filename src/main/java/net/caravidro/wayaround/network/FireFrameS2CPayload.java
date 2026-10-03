package net.caravidro.wayaround.network;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One nearby-flame snapshot per half second, replacing individual particle packets. */
public record FireFrameS2CPayload(List<Flame> flames) implements CustomPacketPayload {
    public static final int MAX_FLAMES=128;
    public record Flame(long pos,float offsetX,float offsetY,float offsetZ,float size) {
        public boolean sane() {
            BlockPos p=BlockPos.of(pos);
            return Math.abs((long)p.getX())<=30_000_000 && Math.abs((long)p.getZ())<=30_000_000
                    && Float.isFinite(offsetX)&&offsetX>=0&&offsetX<=1 && Float.isFinite(offsetY)&&offsetY>=0&&offsetY<=1
                    && Float.isFinite(offsetZ)&&offsetZ>=0&&offsetZ<=1 && Float.isFinite(size)&&size>=.1F&&size<=3;
        }
    }
    public FireFrameS2CPayload {
        if(flames.size()>MAX_FLAMES || flames.stream().anyMatch(f->!f.sane()))throw new IllegalArgumentException("Invalid fire frame");
        flames=List.copyOf(flames);
    }
    public static final Type<FireFrameS2CPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"fire_frame"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FireFrameS2CPayload> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.flames.size());for(var f:p.flames){b.writeLong(f.pos);b.writeFloat(f.offsetX);b.writeFloat(f.offsetY);b.writeFloat(f.offsetZ);b.writeFloat(f.size);}},
            b->{int count=b.readVarInt();if(count<0||count>MAX_FLAMES)throw new IllegalArgumentException("Oversized fire frame");
                var flames=new ArrayList<Flame>(count);for(int i=0;i<count;i++)flames.add(new Flame(b.readLong(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat()));return new FireFrameS2CPayload(flames);});
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
