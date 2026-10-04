package net.caravidro.wayaround.war.outpost;
import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record OutpostControlPayload(byte action,byte forward,byte side,byte vertical,float yaw,float pitch) implements CustomPacketPayload {
    public static final Type<OutpostControlPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","outpost_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OutpostControlPayload> STREAM_CODEC=StreamCodec.of((b,p)-> {
        b.writeByte(p.action);
        b.writeByte(p.forward);
        b.writeByte(p.side);
        b.writeByte(p.vertical);
        b.writeFloat(p.yaw);
        b.writeFloat(p.pitch);
    }
    ,b->new OutpostControlPayload(b.readByte(),b.readByte(),b.readByte(),b.readByte(),b.readFloat(),b.readFloat()));
    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(OutpostControlPayload p,IPayloadContext c) {
        c.enqueueWork(()-> {
            if(c.player() instanceof ServerPlayer s)OutpostRemote.input(s,p);
        }
        );
    }
}
