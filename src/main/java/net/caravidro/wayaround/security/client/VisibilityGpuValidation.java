package net.caravidro.wayaround.security.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.security.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

/** Explicit CI-only fixture: actual framebuffer/depth readback, then the same server-side voxel math. */
final class VisibilityGpuValidation {
    static void run() {
        var mc=Minecraft.getInstance();TextureTarget target=null;
        int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double oldDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDepthMask(true);
            target=new TextureTarget(128,72,true,Minecraft.ON_OSX);target.bindWrite(true);
            Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(70),128F/72,.05F,100);
            Matrix4f inverse=new Matrix4f(projection).invert();
            Vector4f point=new Vector4f(0,0,-9,1).mul(projection);
            GL11.glClearColor(0,1,1,1);GL11.glClearDepth((point.z/point.w+1)/2);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            var sample=VisibilityFrameCapture.readback(target,inverse,new Vec3(.5,.5,0),new Vector3f(0,0,-1),42);
            require(sample.available()&&VisibilityAuditService.valid(sample),"GPU readback has finite normalized rays/distances");
            int covered=0,missing=0,ores=0;var distinct=new java.util.HashSet<String>();
            for(int i=0;i<32;i++) {
                var ray=VisibilityMath.trace(sample.x(),sample.y(),sample.z(),sample.directions()[i*3],sample.directions()[i*3+1],sample.directions()[i*3+2],sample.distances()[i],Byte.toUnsignedInt(sample.brightness()[i]),
                        (x,y,z)->z<=-3&&z>=-5?VisibilityMath.WALL:z==-10?VisibilityMath.ORE:VisibilityMath.OPEN);
                if(ray.covered())covered++;if(ray.missingWall())missing++;if(ray.hiddenOre()){ores++;distinct.add(ray.x()+","+ray.y()+","+ray.z());}
            }
            require(VisibilityMath.strong(covered,missing,ores,distinct.size()),"Real image/depth contradicts opaque wall and matches hidden ores: "+covered+"/"+missing+"/"+ores+"/"+distinct.size());
            target.bindWrite(true);point=new Vector4f(0,0,-2,1).mul(projection);GL11.glClearDepth((point.z/point.w+1)/2);GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            sample=VisibilityFrameCapture.readback(target,inverse,new Vec3(.5,.5,0),new Vector3f(0,0,-1),42);
            for(int i=0;i<32;i++) {
                var ray=VisibilityMath.trace(sample.x(),sample.y(),sample.z(),sample.directions()[i*3],sample.directions()[i*3+1],sample.directions()[i*3+2],sample.distances()[i],Byte.toUnsignedInt(sample.brightness()[i]),
                        (x,y,z)->z<=-3&&z>=-5?VisibilityMath.WALL:z==-10?VisibilityMath.ORE:VisibilityMath.OPEN);
                require(!ray.hiddenOre(),"Legitimate foreground wall does not convict regardless of color");
            }
            require(GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)==target.frameBufferId&&GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)==target.frameBufferId,"Readback restores framebuffer bindings");
            int[] restored=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,restored);require(restored[2]==128&&restored[3]==72,"Readback restores source viewport");
            require(Math.abs(GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE)-(point.z/point.w+1)/2)<.000001,"Readback restores depth clear state");
            float[] restoredClear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,restoredClear);require(restoredClear[0]==0&&restoredClear[1]==1&&restoredClear[2]==1&&restoredClear[3]==1,"Readback restores clear color");
            WayAround.LOGGER.info("[AntiXray] VISUAL GPU FIXTURES PASSED: real small color/depth capture, opaque wall vs hidden ores, normal view, finite rays, framebuffer/viewport restore; no image saved");
        } finally {
            if(target!=null)target.destroyBuffers();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);
            com.mojang.blaze3d.systems.RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);GL11.glClearDepth(oldDepth);GL11.glDepthMask(depthMask);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
