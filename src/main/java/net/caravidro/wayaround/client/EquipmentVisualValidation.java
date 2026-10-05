package net.caravidro.wayaround.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Real shader/framebuffer QA for lens colors, crack visibility and the shared toque model. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class EquipmentVisualValidation {
    private static boolean ran;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("wayaround.validateEquipmentVisuals")||ran)return;
        var mc=Minecraft.getInstance();
        if(mc.getOverlay()!=null||GameRenderer.getPositionColorShader()==null)return;
        ran=true;
        try {run(mc);}catch(Exception|AssertionError failure){WayAround.LOGGER.error("EQUIPMENT GPU FAILED",failure);}
    }
    private static void run(Minecraft mc) throws java.io.IOException {
        TextureTarget target=null;
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depthClear=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        var oldProjection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSort=RenderSystem.getVertexSorting();
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();RenderSystem.applyModelViewMatrix();
        var folder=java.nio.file.Path.of("equipment-captures");java.nio.file.Files.createDirectories(folder);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDepthMask(true);
            target=new TextureTarget(960,540,true,Minecraft.ON_OSX);
            int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,width,height,0,-1000,1000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            for(int damage=0;damage<=2;damage++) {
                try(var image=lens(mc,target,AccessoryKind.ENGINEER_GOGGLES,0,damage)) {
                    int p=image.getPixelRGBA(480,25);
                    require(((p>>>16)&255)>(p&255)+(damage==2?1:4),"Blue glass actually changes framebuffer color");
                    if(damage>0)require(brightPixels(image)>400,"Broken lenses produce visible persistent fractures");
                    image.writeToFile(folder.resolve("lenses-"+damage+".png"));
                }
            }
            try(var lifted=lens(mc,target,AccessoryKind.SPECTRAL_GLASSES,1,2)) {
                int first=lifted.getPixelRGBA(0,0);
                for(int y=0;y<540;y++)for(int x=0;x<960;x++)require(lifted.getPixelRGBA(x,y)==first,"Lifted lenses leave no tint or damage mesh");
            }
            try(var removed=lens(mc,target,null,0,2)) {require(brightPixels(removed)==0,"Removed lenses leave no fracture overlay");}
            try(var violet=lens(mc,target,AccessoryKind.AERO_GOGGLES,0,0)) {
                violet.writeToFile(folder.resolve("aero-lenses.png"));
                int p=violet.getPixelRGBA(480,25);require(((p>>>16)&255)>((p>>>8)&255),"Aero tint follows violet tinted-glass model");
            }
            // The same procedural mesh used by worn and flying hats, with actual baked block textures.
            target.bindWrite(true);GL11.glClearColor(.08F,.1F,.12F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            PoseStack pose=graphics.pose();pose.pushPose();
            pose.translate(width*.5,height*.85,0);pose.scale(-width*.55F,height*.80F,80);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-12));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(28));
            AccessoryRenderer.chefHat(0,pose,mc.getBlockRenderer(),graphics.bufferSource(),0xF000F0);
            graphics.flush();pose.popPose();
            try(var image=Screenshot.takeScreenshot(target)) {
                require(brightPixels(image)>4000,"Actual shared chef model renders on GPU");
                image.writeToFile(folder.resolve("chef-toque.png"));
            }
            target.bindWrite(true);GL11.glClearColor(.08F,.1F,.12F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            pose.pushPose();pose.translate(width*.5,height*.35,0);
            float scale=height*.25F;pose.scale(-scale,scale,scale);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(155));
            AccessoryRenderer.renderChefPreview(pose,mc.getBlockRenderer(),graphics.bufferSource(),0xF000F0);
            graphics.flush();pose.popPose();
            try(var image=Screenshot.takeScreenshot(target)) {
                require(brightPixels(image)>7000,"Actual worn-set helpers render jacket, trousers, gloves, shoes and apron");
                image.writeToFile(folder.resolve("chef-outfit.png"));
            }
            WayAround.LOGGER.info("EQUIPMENT GPU PASSED: actual lens tints, 2 crack stages, lifted/removed clear, shared worn/flying chef mesh");
        } finally {
            if(target!=null)target.destroyBuffers();
            modelView.popMatrix();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection,oldSort);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);GL11.glClearDepth(depthClear);GL11.glDepthMask(mask);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    private static NativeImage lens(Minecraft mc,TextureTarget target,AccessoryKind kind,int mode,int damage) {
        target.bindWrite(true);GL11.glClearColor(.25F,.25F,.25F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
        EyewearOverlay.draw(graphics,kind,mode,damage);graphics.flush();
        return Screenshot.takeScreenshot(target);
    }
    private static int brightPixels(NativeImage image) {
        int bright=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int p=image.getPixelRGBA(x,y);if((p&255)>110&&((p>>>8)&255)>110&&((p>>>16)&255)>110)bright++;
        }return bright;
    }
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    private EquipmentVisualValidation() {}
}
