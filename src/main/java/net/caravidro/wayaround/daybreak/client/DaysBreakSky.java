package net.caravidro.wayaround.daybreak.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Constant mesh budget: 64 sun sectors plus one moon quad, independent of scale. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class DaysBreakSky {
    private static final ResourceLocation MOON=ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY||!DaysBreakClient.active()
                ||event.getCamera().getFluidInCamera()!=FogType.NONE)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Matrix4f matrix=new Matrix4f(event.getModelViewMatrix()).rotateY(-(float)Math.PI/2)
                .rotateX(mc.level.getTimeOfDay(partial)*(float)(Math.PI*2));
        float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
        try {
            if(DaysBreakClient.day())sun(matrix,30*DaysBreakClient.sunScale());
            else moon(matrix,mc.level.getMoonPhase());
        } finally {
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();RenderSystem.enableCull();
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();
            RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);
        }
    }
    private static void sun(Matrix4f matrix,float radius) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
            // Dark red outer rim, hotter red center. No texture or per-frame allocation per sector.
            vertex(buffer,matrix,0,0,255,30,8);
            vertex(buffer,matrix,(float)Math.cos(a)*radius,(float)Math.sin(a)*radius,185,3,2);
            vertex(buffer,matrix,(float)Math.cos(b)*radius,(float)Math.sin(b)*radius,185,3,2);
            float outer=radius*1.13f;
            vertex(buffer,matrix,(float)Math.cos(a)*radius,(float)Math.sin(a)*radius,80,1,2);
            vertex(buffer,matrix,(float)Math.cos(a)*outer,(float)Math.sin(a)*outer,55,0,1);
            vertex(buffer,matrix,(float)Math.cos(b)*outer,(float)Math.sin(b)*outer,55,0,1);
            vertex(buffer,matrix,(float)Math.cos(a)*radius,(float)Math.sin(a)*radius,80,1,2);
            vertex(buffer,matrix,(float)Math.cos(b)*outer,(float)Math.sin(b)*outer,55,0,1);
            vertex(buffer,matrix,(float)Math.cos(b)*radius,(float)Math.sin(b)*radius,80,1,2);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
    private static void vertex(BufferBuilder b,Matrix4f m,float x,float z,int r,int g,int blue){b.addVertex(m,x,100,z).setColor(r,g,blue,255);}
    private static void moon(Matrix4f matrix,int phase) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,MOON);
        RenderSystem.setShaderColor(.85f,.22f,.16f,1);
        float u=(phase%4)/4f,v=(phase/4)/2f;
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        b.addVertex(matrix,-20,-100,20).setUv(u+.25f,v+.5f);
        b.addVertex(matrix,20,-100,20).setUv(u,v+.5f);
        b.addVertex(matrix,20,-100,-20).setUv(u,v);
        b.addVertex(matrix,-20,-100,-20).setUv(u+.25f,v);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }
}
