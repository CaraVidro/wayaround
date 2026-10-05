package net.caravidro.wayaround.nexus.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** CI-only: the real sky renderer, shader and GPU at multiple headings, with a short clipping plane. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class NexusSkyValidation {
    private static boolean ran;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("wayaround.validateNexusSky")||ran)return;
        var mc=Minecraft.getInstance();
        if(mc.getOverlay()!=null||GameRenderer.getPositionColorShader()==null)return;
        ran=true;
        try { run(); }
        catch(Exception|AssertionError failure){WayAround.LOGGER.error("NEXUS SKY GPU FAILED",failure);}
    }
    private static void run() throws java.io.IOException {
        TextureTarget target=null;
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depthClear=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        var folder=java.nio.file.Path.of("nexus-sky-captures");java.nio.file.Files.createDirectories(folder);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDepthMask(true);
            target=new TextureTarget(960,540,true,Minecraft.ON_OSX);
            Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(70),16F/9,.05F,64);
            Matrix4f main=new Matrix4f().rotateX(-.15F).rotateY(-.55F);
            try(NativeImage first=capture(target,main,projection)) {
                checkBand(first,.70);first.writeToFile(folder.resolve("01-rupture.png"));
                try(NativeImage moved=capture(target,new Matrix4f(main).setTranslation(4096,180,-8192),projection)) {
                    for(int y=0;y<first.getHeight();y++)for(int x=0;x<first.getWidth();x++)
                        require(first.getPixelRGBA(x,y)==moved.getPixelRGBA(x,y),"Player translation must not move the celestial backdrop");
                }
            }
            double[] turns={80,130,180,250};
            for(double turn:turns) {
                var view=new Matrix4f().rotateX(-.15F).rotateY(-.55F+(float)Math.toRadians(turn));
                try(NativeImage image=capture(target,view,projection)) {
                    checkBand(image,.94);image.writeToFile(folder.resolve("continuation-"+(int)turn+".png"));
                }
            }
            WayAround.LOGGER.info("NEXUS SKY GPU PASSED: actual shader/framebuffer, 5 headings beyond old rectangle, far=64, fixed translation, continuous luminous ribbon");
        } finally {
            if(target!=null)target.destroyBuffers();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            com.mojang.blaze3d.systems.RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);GL11.glClearDepth(depthClear);GL11.glDepthMask(mask);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    private static NativeImage capture(TextureTarget target,Matrix4f view,Matrix4f projection) {
        target.bindWrite(true);GL11.glClearColor(0,0,0,1);GL11.glClearDepth(1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        boolean beforeDepth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),beforeMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        NexusDimensionAtmosphere.drawSky(view,projection);
        require(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)==beforeDepth&&GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)==beforeMask,"Sky restores depth state for foreground/rooms");
        return Screenshot.takeScreenshot(target);
    }
    private static void checkBand(NativeImage image,double coverage) {
        int columns=0,bright=0;
        for(int x=0;x<image.getWidth();x++) {
            boolean found=false;
            for(int y=0;y<image.getHeight();y++) {
                int pixel=image.getPixelRGBA(x,y);
                if((pixel&255)>190&&((pixel>>>8)&255)>150&&((pixel>>>16)&255)>120){bright++;found=true;}
            }
            if(found)columns++;
        }
        require(bright>image.getWidth()*2&&columns>image.getWidth()*coverage,"Luminous band continues across the view: columns="+columns+", bright="+bright);
    }
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    private NexusSkyValidation() {}
}
