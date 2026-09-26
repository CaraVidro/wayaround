package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.spectrum.SpectrumActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpectrumInputPayload(int action, byte phase) implements CustomPacketPayload {
    public static final byte PRESS=0, RELEASE=1, CANCEL=2, MENU_OPEN=3, MENU_CLOSE=4;
    public static final Type<SpectrumInputPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"spectrum_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpectrumInputPayload> STREAM_CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.action);b.writeByte(p.phase);}, b->new SpectrumInputPayload(b.readVarInt(),b.readByte()));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(SpectrumInputPayload p, IPayloadContext context){context.enqueueWork(()->{
        if(context.player() instanceof ServerPlayer player) {
            if(p.phase==MENU_OPEN || p.phase==MENU_CLOSE) {
                SpectrumActions.menuState(player,p.action,p.phase==MENU_OPEN);
            } else {
                SpectrumActions.input(player,p.action,p.phase);
            }
        }
    });}
}
