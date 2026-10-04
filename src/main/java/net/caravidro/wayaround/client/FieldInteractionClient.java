package net.caravidro.wayaround.client;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.FieldControlPayload;
import net.caravidro.wayaround.observation.EntitySpectate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class FieldInteractionClient {
 public static int target=-1;private static int held;private static boolean reeling,clicked;
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){target=-1;held=0;reeling=false;clicked=false;return;}
  boolean use=mc.screen==null&&mc.options.keyUse.isDown();
  boolean click=use||mc.screen==null&&(mc.options.keyAttack.isDown()||mc.options.keyShift.isDown());
  if(EntitySpectate.holding(mc.player)){mc.player.noPhysics=true;mc.player.setInvisible(true);}
  if(target>=0&&click&&!clicked){PacketDistributor.sendToServer(new FieldControlPayload((byte)3));target=-1;}
  clicked=click;
  boolean rod=mc.player.fishing!=null&&(mc.player.getMainHandItem().is(Items.FISHING_ROD)||mc.player.getOffhandItem().is(Items.FISHING_ROD));
  held=rod&&use?held+1:0;boolean now=held>=5;
  if(now!=reeling||now&&held%10==0)PacketDistributor.sendToServer(new FieldControlPayload((byte)(now?1:2)));reeling=now;
 }
 @SubscribeEvent public static void hands(RenderHandEvent e){var p=Minecraft.getInstance().player;if(p!=null&&EntitySpectate.holding(p))e.setCanceled(true);}
 @SubscribeEvent public static void player(RenderPlayerEvent.Pre e){if(EntitySpectate.holding(e.getEntity()))e.setCanceled(true);}
 @SubscribeEvent public static void click(InputEvent.InteractionKeyMappingTriggered e){var mc=Minecraft.getInstance();if(mc.player==null||!EntitySpectate.holding(mc.player)||!(e.isUseItem()||e.isAttack()))return;
  e.setCanceled(true);e.setSwingHand(false);
  if(target>=0){PacketDistributor.sendToServer(new FieldControlPayload((byte)3));target=-1;clicked=true;}
  else if(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit){PacketDistributor.sendToServer(new FieldControlPayload((byte)4,hit.getEntity().getId()));}
 }
}
