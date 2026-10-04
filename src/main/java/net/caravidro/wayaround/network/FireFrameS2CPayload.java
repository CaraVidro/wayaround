package net.caravidro.wayaround.network;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One nearby-flame snapshot per half second, replacing individual particle packets. */
public record FireFrameS2CPayload(List<Flame> flames,List<Blaze> blazes) implements CustomPacketPayload {
    public static final int MAX_FLAMES=128;
    public record Blaze(long pos,float width,float depth,float height){
        public boolean sane(){return Float.isFinite(width)&&width>0&&width<=256&&Float.isFinite(depth)&&depth>0&&depth<=256&&Float.isFinite(height)&&height>0&&height<=32&&Math.abs((long)BlockPos.of(pos).getX())<=30_000_000&&Math.abs((long)BlockPos.of(pos).getZ())<=30_000_000;}
    }
    public FireFrameS2CPayload(List<Flame> flames){this(flames,List.of());}
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
        if(blazes.size()>16||blazes.stream().anyMatch(b->!b.sane()))throw new IllegalArgumentException("Invalid wildfire frame");
        flames=List.copyOf(flames);blazes=List.copyOf(blazes);
    }
    public static final Type<FireFrameS2CPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"fire_frame"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FireFrameS2CPayload> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.flames.size());for(var f:p.flames){b.writeLong(f.pos);b.writeFloat(f.offsetX);b.writeFloat(f.offsetY);b.writeFloat(f.offsetZ);b.writeFloat(f.size);}b.writeVarInt(p.blazes.size());for(var patch:p.blazes){b.writeLong(patch.pos);b.writeFloat(patch.width);b.writeFloat(patch.depth);b.writeFloat(patch.height);}},
            b->{int count=b.readVarInt();if(count<0||count>MAX_FLAMES)throw new IllegalArgumentException("Oversized fire frame");
                var flames=new ArrayList<Flame>(count);for(int i=0;i<count;i++)flames.add(new Flame(b.readLong(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat()));int patches=b.readVarInt();if(patches<0||patches>16)throw new IllegalArgumentException("Oversized wildfire frame");var blazes=new ArrayList<Blaze>(patches);for(int i=0;i<patches;i++)blazes.add(new Blaze(b.readLong(),b.readFloat(),b.readFloat(),b.readFloat()));return new FireFrameS2CPayload(flames,blazes);});
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
