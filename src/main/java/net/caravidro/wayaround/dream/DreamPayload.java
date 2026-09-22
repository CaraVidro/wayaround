package net.caravidro.wayaround.dream;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DreamPayload(DreamState state,int ticks) implements CustomPacketPayload {
    public static final Type<DreamPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","dream_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DreamPayload> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeEnum(p.state());buf.writeVarInt(p.ticks());},
            buf->new DreamPayload(buf.readEnum(DreamState.class),buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
