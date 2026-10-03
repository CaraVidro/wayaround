package net.caravidro.wayaround.daybreak;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.caravidro.wayaround.network.ClientPayloadBridge;

public record DaysBreakPayload(boolean active,long elapsed) implements CustomPacketPayload {
    public static final Type<DaysBreakPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround","then_days_break"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DaysBreakPayload> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.active);b.writeLong(p.elapsed);},b->new DaysBreakPayload(b.readBoolean(),Math.max(0,b.readLong())));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(DaysBreakPayload payload,IPayloadContext context){context.enqueueWork(()->ClientPayloadBridge.daysBreak(payload));}
}
