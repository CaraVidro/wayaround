package net.caravidro.wayaround.network;

import java.util.UUID;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TukunaFugaClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FugaArrowPayload(UUID owner,double x,double y,double z,double vx,double vy,double vz,int life) implements CustomPacketPayload {
    public static final Type<FugaArrowPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"fuga_arrow"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FugaArrowPayload> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.owner);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);
                b.writeDouble(p.vx);b.writeDouble(p.vy);b.writeDouble(p.vz);b.writeVarInt(p.life);},
            b->new FugaArrowPayload(b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt()));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(FugaArrowPayload p,IPayloadContext c){c.enqueueWork(()->TukunaFugaClientEffects.receiveArrow(p));}
}
