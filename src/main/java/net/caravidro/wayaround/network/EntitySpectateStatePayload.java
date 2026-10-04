package net.caravidro.wayaround.network;
import net.caravidro.wayaround.WayAround;
import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record EntitySpectateStatePayload(int target) implements CustomPacketPayload {
 public static final Type<EntitySpectateStatePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"entity_spectate_state"));
 public static final StreamCodec<RegistryFriendlyByteBuf,EntitySpectateStatePayload> STREAM_CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.target),b->new EntitySpectateStatePayload(b.readVarInt()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
