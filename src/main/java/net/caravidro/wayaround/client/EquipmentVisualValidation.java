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
    private static void run(Minecraft mc) throws Exception {
        TextureTarget target=null;
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depthClear=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        var oldProjection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSort=RenderSystem.getVertexSorting();
        float oldFogStart=RenderSystem.getShaderFogStart(),oldFogEnd=RenderSystem.getShaderFogEnd();
        float[] oldColor=RenderSystem.getShaderColor().clone();
        var lightsField=java.util.Arrays.stream(RenderSystem.class.getDeclaredFields())
                .filter(f->f.getType()==org.joml.Vector3f[].class).findFirst().orElseThrow();
        lightsField.setAccessible(true);
        var oldLights=(org.joml.Vector3f[])lightsField.get(null);
        var firstLight=new org.joml.Vector3f(oldLights[0]);
        var secondLight=new org.joml.Vector3f(oldLights[1]);
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();RenderSystem.applyModelViewMatrix();
        var folder=java.nio.file.Path.of("equipment-captures");java.nio.file.Files.createDirectories(folder);
        try (var lightmap=new PreviewLightmap(mc)) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDepthMask(true);
            RenderSystem.setShaderFogStart(9999);RenderSystem.setShaderFogEnd(99999);
            RenderSystem.setShaderColor(1,1,1,1);
            // Our preview faces +Z; vanilla item lighting is aligned to item GUI transforms instead.
            RenderSystem.setShaderLights(new org.joml.Vector3f(.3F,-.8F,1).normalize(),
                    new org.joml.Vector3f(-.5F,.3F,1).normalize());
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
            try(var darkness=lens(mc,target,AccessoryKind.ENGINEER_GOGGLES,0,0,0)) {
                for(int y=0;y<540;y++)for(int x=0;x<960;x++)
                    require((darkness.getPixelRGBA(x,y)&0xFFFFFF)==0,"Colored lenses never illuminate a pitch-black scene");
            }
            // The same procedural mesh used by worn and flying hats, with actual baked block textures.
            target.bindWrite(true);GL11.glClearColor(.08F,.1F,.12F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            PoseStack pose=graphics.pose();pose.pushPose();
            pose.translate(width*.5,height*.95,0);pose.scale(-width*.55F,height*.70F,80);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-12));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(28));
            AccessoryRenderer.chefHat(0,pose,mc.getBlockRenderer(),graphics.bufferSource(),0xF000F0);
            graphics.flush();pose.popPose();
            int hatPixels,outfitPixels;
            try(var image=Screenshot.takeScreenshot(target)) {
                image.writeToFile(folder.resolve("chef-toque.png"));
                hatPixels=clothPixels(image);
            }
            target.bindWrite(true);GL11.glClearColor(.08F,.1F,.12F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            pose.pushPose();pose.translate(width*.5,height*.35,0);
            float scale=height*.25F;pose.scale(-scale,scale,scale);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(155));
            AccessoryRenderer.renderChefPreview(pose,mc.getBlockRenderer(),graphics.bufferSource(),0xF000F0);
            graphics.flush();pose.popPose();
            try(var image=Screenshot.takeScreenshot(target)) {
                image.writeToFile(folder.resolve("chef-outfit.png"));
                outfitPixels=clothPixels(image);
            }
            require(hatPixels>4000,"Actual shared chef model has visible textured cloth coverage: "+hatPixels);
            require(outfitPixels>7000,"Actual worn-set helpers render jacket, trousers, gloves, shoes and apron: "+outfitPixels);
            WayAround.LOGGER.info("EQUIPMENT GPU PASSED: actual lens tints, 2 crack stages, lifted/removed clear, shared worn/flying chef mesh");
        } finally {
            if(target!=null)target.destroyBuffers();
            RenderSystem.setShaderFogStart(oldFogStart);RenderSystem.setShaderFogEnd(oldFogEnd);
            RenderSystem.setShaderColor(oldColor[0],oldColor[1],oldColor[2],oldColor[3]);
            RenderSystem.setShaderLights(firstLight,secondLight);
            modelView.popMatrix();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection,oldSort);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);GL11.glClearDepth(depthClear);GL11.glDepthMask(mask);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    /** Menu QA has no world to update the lightmap; supply full-bright inputs and restore them. */
    private static final class PreviewLightmap implements AutoCloseable {
        private final net.minecraft.client.renderer.texture.DynamicTexture texture;
        private final NativeImage image;
        private final int[] pixels;
        private PreviewLightmap(Minecraft mc) throws ReflectiveOperationException {
            java.lang.reflect.Field field=java.util.Arrays.stream(net.minecraft.client.renderer.LightTexture.class.getDeclaredFields())
                    .filter(f->f.getType()==net.minecraft.client.renderer.texture.DynamicTexture.class).findFirst().orElseThrow();
            field.setAccessible(true);
            texture=(net.minecraft.client.renderer.texture.DynamicTexture)field.get(mc.gameRenderer.lightTexture());
            image=java.util.Objects.requireNonNull(texture.getPixels());
            pixels=new int[image.getWidth()*image.getHeight()];
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                pixels[y*image.getWidth()+x]=image.getPixelRGBA(x,y);image.setPixelRGBA(x,y,0xFFFFFFFF);
            }
            texture.upload();
        }
        @Override public void close() {
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setPixelRGBA(x,y,pixels[y*image.getWidth()+x]);
            texture.upload();
        }
    }
    private static NativeImage lens(Minecraft mc,TextureTarget target,AccessoryKind kind,int mode,int damage) {
        return lens(mc,target,kind,mode,damage,.25F);
    }
    private static NativeImage lens(Minecraft mc,TextureTarget target,AccessoryKind kind,int mode,int damage,float background) {
        target.bindWrite(true);GL11.glClearColor(background,background,background,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
        EyewearOverlay.draw(graphics,kind,mode,damage);graphics.flush();
        return Screenshot.takeScreenshot(target);
    }
    private static int brightPixels(NativeImage image) {
        int bright=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int p=image.getPixelRGBA(x,y);if((p&255)>110&&((p>>>8)&255)>110&&((p>>>16)&255)>110)bright++;
        }return bright;
    }
    private static int clothPixels(NativeImage image) {
        // Actual diffuse wool can be gray under block shading; measure contrast against the dark backdrop.
        int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int p=image.getPixelRGBA(x,y);if((p&255)>55&&((p>>>8)&255)>55&&((p>>>16)&255)>55)count++;
        }return count;
    }
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    private EquipmentVisualValidation() {}
}
