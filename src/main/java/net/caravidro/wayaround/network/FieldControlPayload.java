package net.caravidro.wayaround.network;
import net.caravidro.wayaround.WayAround;
import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record FieldControlPayload(byte action) implements CustomPacketPayload {
 public static final Type<FieldControlPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"field_control"));
 public static final StreamCodec<RegistryFriendlyByteBuf,FieldControlPayload> STREAM_CODEC=StreamCodec.of((b,p)->b.writeByte(p.action),b->new FieldControlPayload(b.readByte()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void handle(FieldControlPayload p,IPayloadContext ctx){ctx.enqueueWork(()->{if(!(ctx.player() instanceof ServerPlayer player))return;
  if(p.action==3){net.caravidro.wayaround.observation.EntitySpectate.stop(player);return;}
  if((p.action==1||p.action==2)&&player.fishing!=null&&(player.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD)||player.getOffhandItem().is(net.minecraft.world.item.Items.FISHING_ROD))){
   if(p.action==1)player.getPersistentData().putLong("WayAroundReelUntil",player.level().getGameTime()+15);else player.getPersistentData().remove("WayAroundReelUntil");
  }
 });}
}
