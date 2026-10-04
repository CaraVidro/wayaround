package net.caravidro.wayaround.war.outpost;
import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record OutpostViewPayload(int target,long mount) implements CustomPacketPayload {
    public OutpostViewPayload(int target) {
        this(target,0);
    }
    public static final Type<OutpostViewPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","outpost_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OutpostViewPayload> STREAM_CODEC=StreamCodec.of((b,p)-> {
        b.writeVarInt(p.target);
        b.writeLong(p.mount);
    }
    ,b->new OutpostViewPayload(b.readVarInt(),b.readLong()));
    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
