package net.caravidro.wayaround.nexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.nexus.NexusPortalManager;
import net.caravidro.wayaround.nexus.world.NexusLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A fixed celestial rupture and world-anchored bounded debris. No server entities or particle packets. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class NexusDimensionAtmosphere {
    private static final DustParticleOptions DUST=new DustParticleOptions(new Vector3f(.72F,.018F,.035F),1.25F);
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused()||!mc.level.dimension().equals(NexusPortalManager.NEXUS)
                ||mc.player.tickCount%2!=0||mc.player.isUnderWater())return;
        var p=mc.player.blockPosition();if(!mc.level.hasChunkAt(p)||mc.player.getY()<mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,p.getX(),p.getZ()))return;
        for(int i=0;i<2;i++)mc.level.addParticle(DUST,mc.player.getX()+mc.level.random.nextDouble()*24-12,
                mc.player.getY()+mc.level.random.nextDouble()*9,mc.player.getZ()+mc.level.random.nextDouble()*24-12,.16,.008,.035);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().equals(NexusPortalManager.NEXUS)||e.getCamera().getFluidInCamera()!=FogType.NONE)return;
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY)sky(e);
        else if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)debris(e);
    }
    private static VertexBuffer skyBuffer;
    private static void sky(RenderLevelStageEvent e) {
        drawSky(new Matrix4f(e.getModelViewMatrix()).setTranslation(0,0,0), e.getProjectionMatrix());
    }
    /** Also used by the opt-in offscreen GPU fixture; the projection is the actual caller's. */
    static void drawSky(Matrix4f view, Matrix4f projection) {
        if (skyBuffer == null) {
            var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (var v : NexusSkyGeometry.create()) {
                var c = v.color(); builder.addVertex(v.x(), v.y(), v.z()).setColor(c.r(), c.g(), c.b(), c.a());
            }
            skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            skyBuffer.bind(); skyBuffer.upload(builder.buildOrThrow()); VertexBuffer.unbind();
        }
        float fogStart=RenderSystem.getShaderFogStart(), fogEnd=RenderSystem.getShaderFogEnd();
        float[] color=RenderSystem.getShaderColor();float r=color[0],g=color[1],b=color[2],a=color[3];
        boolean depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        boolean cull=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        boolean blend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        boolean mask=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        int src=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB),dst=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int srcA=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA),dstA=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
            skyBuffer.bind();skyBuffer.drawWithShader(new Matrix4f(view).setTranslation(0,0,0),projection,GameRenderer.getPositionColorShader());
        } finally {
            VertexBuffer.unbind();RenderSystem.setShaderColor(r,g,b,a);
            RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);RenderSystem.depthMask(mask);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        if(skyBuffer!=null){skyBuffer.close();skyBuffer=null;}
    }
    private static void debris(RenderLevelStageEvent e) {
        var mc=Minecraft.getInstance();var c=e.getCamera().getPosition();var matrix=e.getModelViewMatrix();
        double time=mc.level.getGameTime()+e.getPartialTick().getGameTimeDeltaPartialTick(false);
        int tx=Math.floorDiv((int)Math.floor(c.x),128),tz=Math.floorDiv((int)Math.floor(c.z),128);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int i=0;i<4;i++) {
            long h=NexusLayout.hash(0x4e65787573L+i,tx+dx,tz+dz);
            double x=(tx+dx)*128+Math.floorMod(h,128),z=(tz+dz)*128+Math.floorMod(h>>>8,128);
            double phase=(time+Math.floorMod(h>>>16,420))%420;
            if(i==0&&phase<74) {
                double y=244-phase*2.2,mx=x+phase*.45,mz=z-phase*.12;
                cube(b,matrix,c,mx,y,mz,.9,245,28,18);
                for(int trail=1;trail<=5;trail++)cube(b,matrix,c,mx-trail*.7,y+trail*3,mz+trail*.2,Math.max(.2,.8-trail*.1),180-trail*22,4,9);
            } else if(i>0) {
                double y=i==1?170-phase*.24:108+Math.floorMod(h>>>24,60)+Math.sin(time*.009+i)*5;
                cube(b,matrix,c,x+Math.sin(time*.005+i)*2,y,z,.5+Math.floorMod(h>>>32,13)*.13,62,44,47);
            }
        }
        var model=RenderSystem.getModelViewStack();model.pushMatrix();model.identity();RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.disableCull();RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}finally{RenderSystem.enableCull();model.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void cube(BufferBuilder b,Matrix4f m,Vec3 c,double x,double y,double z,double r,int red,int green,int blue) {
        x-=c.x;y-=c.y;z-=c.z;
        quad(b,m,x-r,y-r,z-r,x+r,y-r,z-r,x+r,y+r,z-r,x-r,y+r,z-r,red,green,blue);
        quad(b,m,x-r,y-r,z+r,x-r,y+r,z+r,x+r,y+r,z+r,x+r,y-r,z+r,red,green,blue);
        quad(b,m,x-r,y-r,z-r,x-r,y+r,z-r,x-r,y+r,z+r,x-r,y-r,z+r,red*3/4,green*3/4,blue*3/4);
        quad(b,m,x+r,y-r,z-r,x+r,y-r,z+r,x+r,y+r,z+r,x+r,y+r,z-r,red*3/4,green*3/4,blue*3/4);
        quad(b,m,x-r,y+r,z-r,x+r,y+r,z-r,x+r,y+r,z+r,x-r,y+r,z+r,red,green,blue);
        quad(b,m,x-r,y-r,z-r,x-r,y-r,z+r,x+r,y-r,z+r,x+r,y-r,z-r,red/2,green/2,blue/2);
    }
    private static void quad(BufferBuilder b,Matrix4f m,double x,double y,double z,double a,double d,double f,double g,double h,double i,double j,double k,double l,int r,int green,int blue) {
        vertex(b,m,x,y,z,r,green,blue);vertex(b,m,a,d,f,r,green,blue);vertex(b,m,g,h,i,r,green,blue);vertex(b,m,j,k,l,r,green,blue);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,double x,double y,double z,int r,int g,int blue){b.addVertex(m,(float)x,(float)y,(float)z).setColor(r,g,blue,255);}
    private NexusDimensionAtmosphere() {}
}
