package net.caravidro.wayaround.security.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.security.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

/** Ephemeral 64x36 world-only screenshot and depth: two reads, no full-resolution screenshot, disk or upload. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class VisibilityFrameCapture {
    private static long nonce,deadline;
    private static boolean pending,announced;
    private static net.minecraft.client.multiplayer.ClientLevel owner;
    public static void request(long challenge) {
        nonce=challenge;deadline=System.nanoTime()+1_000_000_000L;pending=true;
        var mc=Minecraft.getInstance();
        if(!announced&&mc.player!=null){announced=true;mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[WayAround Anti-Xray] Este servidor usa amostras visuais do mundo para verificar oclusão. A captura é temporária, antes do HUD: nenhuma imagem é salva ou enviada; somente medidas de profundidade/brilho são enviadas."));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){owner=mc.level;pending=false;announced=false;}
        if(pending&&System.nanoTime()>deadline){pending=false;if(mc.getConnection()!=null)PacketDistributor.sendToServer(VisibilityReportPayload.skipped(nonce));}
    }
    @SubscribeEvent public static void frame(RenderLevelStageEvent event) {
        if(!pending||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        pending=false;var mc=Minecraft.getInstance();VisibilityReportPayload response=VisibilityReportPayload.skipped(nonce);
        if(System.nanoTime()<deadline&&eligible(mc,event)) {
            try { response=capture(mc,event,nonce); }
            catch(RuntimeException failure){WayAround.LOGGER.debug("[AntiXray] Visual sample unavailable: {}",failure.toString());}
        }
        if(mc.getConnection()!=null)PacketDistributor.sendToServer(response);
    }
    private static boolean eligible(Minecraft mc,RenderLevelStageEvent event) {
        if(mc.player==null||mc.level==null||mc.screen!=null||mc.player.tickCount<120||!mc.player.isAlive()
                ||mc.player.isSpectator()||mc.player.isPassenger()||mc.player.isSleeping()||mc.player.hurtTime>0
                ||mc.player.isUnderWater()||!mc.options.getCameraType().isFirstPerson()||mc.getCameraEntity()!=mc.player)return false;
        if(ModList.get().isLoaded("iris")||ModList.get().isLoaded("oculus")||ModList.get().isLoaded("immersive_portals"))return false;
        if(net.caravidro.wayaround.observation.EntitySpectate.holding(mc.player)
                ||net.caravidro.wayaround.war.outpost.client.OutpostClient.target>=0
                ||net.caravidro.wayaround.media.client.MediaRecorder.isRecording()
                ||net.caravidro.wayaround.client.cinematic.CinematicCameraController.isActiveFor(mc.player)
                ||net.caravidro.wayaround.dream.client.DreamClientState.state()!=net.caravidro.wayaround.dream.DreamState.NONE)return false;
        return event.getCamera().getPosition().distanceToSqr(mc.player.getEyePosition())<.36
                &&!GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
                &&GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)==mc.getMainRenderTarget().frameBufferId;
    }

    private static VisibilityReportPayload capture(Minecraft mc,RenderLevelStageEvent event,long seed) {
        Matrix4f inverse=new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
        var camera=event.getCamera();
        return readback(mc.getMainRenderTarget(),inverse,camera.getPosition(),camera.getLookVector(),seed);
    }
    static VisibilityReportPayload readback(com.mojang.blaze3d.pipeline.RenderTarget source,Matrix4f inverse,net.minecraft.world.phys.Vec3 eye,Vector3f forward,long seed) {
        if(!Float.isFinite(inverse.determinant()))return VisibilityReportPayload.skipped(seed);
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int alignment=GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT),row=GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH);
        int skipRows=GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS),skipPixels=GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        TextureTarget image=null;ByteBuffer color=null;FloatBuffer depth=null;
        try {
            image=new TextureTarget(VisibilityMath.WIDTH,VisibilityMath.HEIGHT,true,Minecraft.ON_OSX);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,image.frameBufferId);
            GL30.glBlitFramebuffer(0,0,source.viewWidth,source.viewHeight,0,0,VisibilityMath.WIDTH,VisibilityMath.HEIGHT,
                    GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,GL11.GL_NEAREST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,image.frameBufferId);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0);
            color=MemoryUtil.memCalloc(VisibilityMath.WIDTH*VisibilityMath.HEIGHT*4);
            depth=MemoryUtil.memAllocFloat(VisibilityMath.WIDTH*VisibilityMath.HEIGHT);
            for(int i=0;i<depth.capacity();i++)depth.put(i,1F);
            GL11.glReadPixels(0,0,VisibilityMath.WIDTH,VisibilityMath.HEIGHT,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,color);
            GL11.glReadPixels(0,0,VisibilityMath.WIDTH,VisibilityMath.HEIGHT,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,depth);
            float[] directions=new float[VisibilityMath.SAMPLES*3],distances=new float[VisibilityMath.SAMPLES];byte[] brightness=new byte[VisibilityMath.SAMPLES];
            for(int i=0;i<VisibilityMath.SAMPLES;i++) {
                int pixel=VisibilityMath.pixel(seed,i),px=pixel%VisibilityMath.WIDTH,py=pixel/VisibilityMath.WIDTH;
                // Blit uses nearest sampling: reconstruct the exact SOURCE pixel center, not the resized center.
                int sx=(int)((px+.5)*source.viewWidth/VisibilityMath.WIDTH),sy=(int)((py+.5)*source.viewHeight/VisibilityMath.HEIGHT);
                float nx=(float)((sx+.5)*2/source.viewWidth-1),ny=(float)((sy+.5)*2/source.viewHeight-1);
                float d=depth.get(pixel);
                if(!Float.isFinite(d)||d<0||d>1)return VisibilityReportPayload.skipped(seed);
                Vector4f far=new Vector4f(nx,ny,1,1).mul(inverse);far.div(far.w);
                Vector3f direction=new Vector3f(far.x,far.y,far.z).normalize();
                directions[i*3]=direction.x;directions[i*3+1]=direction.y;directions[i*3+2]=direction.z;
                Vector4f point=new Vector4f(nx,ny,d*2-1,1).mul(inverse);point.div(point.w);
                distances[i]=d>=.999999F?65536:new Vector3f(point.x,point.y,point.z).length();
                int r=Byte.toUnsignedInt(color.get(pixel*4)),g=Byte.toUnsignedInt(color.get(pixel*4+1)),b=Byte.toUnsignedInt(color.get(pixel*4+2));
                brightness[i]=(byte)((r*54+g*183+b*19)/256);
            }
            return new VisibilityReportPayload(seed,true,eye.x,eye.y,eye.z,forward.x,forward.y,forward.z,directions,distances,brightness);
        } finally {
            if(color!=null){MemoryUtil.memSet(color,0);MemoryUtil.memFree(color);}
            if(depth!=null){MemoryUtil.memSet(MemoryUtil.memAddress(depth),0,(long)depth.capacity()*4);MemoryUtil.memFree(depth);}
            if(image!=null)image.destroyBuffers();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,alignment);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,row);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,skipRows);GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,skipPixels);
            GlStateManager._bindTexture(texture);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);GlStateManager._clearDepth(clearDepth);RenderSystem.depthMask(depthMask);
        }
    }
    private VisibilityFrameCapture() {}
}
