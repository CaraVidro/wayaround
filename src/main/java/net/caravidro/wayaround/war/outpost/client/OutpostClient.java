package net.caravidro.wayaround.war.outpost.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.war.outpost.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class OutpostClient {
    public static net.minecraft.core.BlockPos mountedAt;
    public static int target=-1;private static int ticks,mountUntil;private static float smokeDensity;private static net.minecraft.client.multiplayer.ClientLevel owner;
    private static final KeyMapping PHOTO=new KeyMapping("key.wayaround.drone_photo",GLFW.GLFW_KEY_P,"key.categories.wayaround");
    private static final KeyMapping RECORD=new KeyMapping("key.wayaround.drone_record",GLFW.GLFW_KEY_V,"key.categories.wayaround");
    public static void view(int id,long mount){mountUntil=ticks+40;mountedAt=id==-2?net.minecraft.core.BlockPos.of(mount):null;if(id<0&&target>=0&&net.caravidro.wayaround.media.client.MediaRecorder.isRecording())net.caravidro.wayaround.media.client.MediaRecorder.handleLongPress();target=id>=0?id:-1;if(mountedAt!=null){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.level!=null&&mc.level.getBlockState(mountedAt).hasProperty(FieldDeviceBlock.FACING)){mc.player.setYRot(mc.level.getBlockState(mountedAt).getValue(FieldDeviceBlock.FACING).toYRot());mc.player.setXRot(0);}}}
    public static OutpostDroneEntity drone(){var mc=Minecraft.getInstance();return mc.level!=null&&mc.level.getEntity(target) instanceof OutpostDroneEntity d&&d.isAlive()?d:null;}
    public static boolean cameraAvailable(){var d=drone();return d!=null&&!d.impact()&&target>=0;}
    private static boolean mounted(){var p=Minecraft.getInstance().player;return p!=null&&p.getVehicle()!=null&&mountedAt!=null;}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(PHOTO);e.register(RECORD);}
    @SubscribeEvent public static void registrations(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(OutpostContent.DRONE.get(),DroneRenderer::new);e.registerEntityRenderer(OutpostContent.CANISTER.get(),CanisterRenderer::new);e.registerBlockEntityRenderer(OutpostContent.DEVICE.get(),FieldDeviceRenderer::new);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(owner!=mc.level){target=-1;mountedAt=null;smokeDensity=0;owner=mc.level;}if(mc.player==null||mc.level==null)return;ticks++;smokeDensity+=(rawSmoke()-smokeDensity)*.25F;if(mountedAt!=null&&!mounted()&&ticks>mountUntil)mountedAt=null;
        if(target>=0){var d=drone();if(d==null){if(ticks%10==0)send((byte)3);return;}
            if(mc.options.keyShift.isDown()||!RemoteControllerItem.holds(mc.player)||mc.screen!=null){send((byte)3);return;}
            if(ticks%2==0){byte f=(byte)((mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0)),side=(byte)((mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0)),v=(byte)((mc.options.keyJump.isDown()?1:0)-(mc.options.keySprint.isDown()?1:0));PacketDistributor.sendToServer(new OutpostControlPayload((byte)0,f,side,v,mc.player.getYRot(),mc.player.getXRot()));}
            if(d.impact()&&mc.options.keyAttack.isDown())send((byte)2);
            if(cameraAvailable()){while(PHOTO.consumeClick())net.caravidro.wayaround.media.client.MediaRecorder.handleShortPress();while(RECORD.consumeClick())net.caravidro.wayaround.media.client.MediaRecorder.handleLongPress();}
        }else {while(PHOTO.consumeClick()){}while(RECORD.consumeClick()){}if(mounted()&&mc.screen==null&&mc.options.keyAttack.isDown()&&ticks%2==0)send((byte)1);}
    }
    private static void send(byte action){PacketDistributor.sendToServer(new OutpostControlPayload(action,(byte)0,(byte)0,(byte)0,0,0));}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){if(target<0)return;var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;}
    @SubscribeEvent public static void click(InputEvent.InteractionKeyMappingTriggered e){if(target>=0||mountedAt!=null){e.setCanceled(true);e.setSwingHand(false);}}
    @SubscribeEvent public static void hands(RenderHandEvent e){if(target>=0||mountedAt!=null)e.setCanceled(true);}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();var d=drone();var g=e.getGuiGraphics();int y=mc.getWindow().getGuiScaledHeight()-44;
        if(d==null){if(mounted()&&mc.level.getBlockEntity(mountedAt) instanceof FieldDeviceBlockEntity gun){g.fill(8,y-5,270,y+25,0xA5101512);g.drawString(mc.font,Component.translatable("hud.wayaround.gatling",gun.ammo(),Math.min(100,gun.heat()*100/80)),14,y,0xD8EDC2);g.drawString(mc.font,Component.translatable("message.wayaround.outpost.mounted"),14,y+12,0xFFFFFF);}return;}
        g.fill(8,y-5,Math.min(380,mc.getWindow().getGuiScaledWidth()-8),y+30,0xA5101512);g.drawString(mc.font,Component.translatable("hud.wayaround.drone",Math.max(0,d.battery()/20)),14,y,0xD8EDC2);g.drawString(mc.font,Component.translatable(d.impact()?"hud.wayaround.drone.impact":"hud.wayaround.drone.camera"),14,y+14,0xFFFFFF);
    }
    private static float smoke(){return smokeDensity;}
    private static float rawSmoke(){var mc=Minecraft.getInstance();if(mc.level==null)return 0;Vec3 at=mc.gameRenderer.getMainCamera().getPosition();float density=0;int n=0;for(var c:mc.level.getEntitiesOfClass(FieldCanisterEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(7))){if(n++>=16)break;if(c.active()&&!c.flare())density=Math.max(density,(float)Math.max(0,1-at.distanceTo(c.position().add(0,1,0))/6));}return density;}
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog e){if(e.getType()!=net.minecraft.world.level.material.FogType.NONE)return;float s=smoke();if(s<=.01)return;e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(),.5F));e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(),Math.max(3,14-11*s)));e.setCanceled(true);}
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor e){if(e.getCamera().getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE)return;float s=smoke();if(s<=.01)return;e.setRed(e.getRed()*(1-s)+.34F*s);e.setGreen(e.getGreen()*(1-s)+.36F*s);e.setBlue(e.getBlue()*(1-s)+.32F*s);}
    private OutpostClient(){}
}
