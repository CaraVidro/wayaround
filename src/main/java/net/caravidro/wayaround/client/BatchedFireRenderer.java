package net.caravidro.wayaround.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.FireFrameS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Animated vanilla atlas flames in one local mesh, with no flame entities/particles. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class BatchedFireRenderer {
    private record Flame(FireFrameS2CPayload.Flame data,float previousSize,long at) {
        float size(double now){return (float)(previousSize+(data.size()-previousSize)*Math.clamp((now-at)/10,0,1));}
    }
    private static final Map<Long,Flame> FLAMES=new LinkedHashMap<>();
    private static ClientLevel owner;
    private static long lastFrame;
    public static void receive(FireFrameS2CPayload frame) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(owner!=level){FLAMES.clear();owner=level;}
        long now=level.getGameTime();lastFrame=now;
        Set<Long> fresh=new HashSet<>();
        for(var data:frame.flames()) {
            var old=FLAMES.get(data.pos());fresh.add(data.pos());
            FLAMES.put(data.pos(),new Flame(data,old==null?data.size():old.size(now),now));
        }
        FLAMES.keySet().retainAll(fresh);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        if(Minecraft.getInstance().level!=owner || owner!=null && owner.getGameTime()-lastFrame>35){FLAMES.clear();owner=null;}
        if(owner!=null)FLAMES.keySet().removeIf(key->{var pos=BlockPos.of(key);return !owner.hasChunkAt(pos)||!owner.getBlockState(pos).is(Blocks.FIRE);});
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || FLAMES.isEmpty() || Minecraft.getInstance().level!=owner)return;
        var mc=Minecraft.getInstance();var camera=e.getCamera().getPosition();var matrix=e.getModelViewMatrix();
        double now=owner.getGameTime()+e.getPartialTick().getGameTimeDeltaPartialTick(false);
        var texture=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/fire_0"));
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);int count=0;
        for(var flame:FLAMES.values()) {
            var data=flame.data();BlockPos pos=BlockPos.of(data.pos());
            if(pos.distToCenterSqr(camera)>96*96)continue;
            double size=flame.size(now),radius=.16+size*.48,height=.44+size*1.12;
            double x=pos.getX()+data.offsetX()-camera.x,y=pos.getY()+data.offsetY()-camera.y,z=pos.getZ()+data.offsetZ()-camera.z;
            double angle=(data.pos()&255)*.0245;
            for(int plane=0;plane<3;plane++) {
                double a=angle+plane*Math.PI/3,dx=Math.cos(a)*radius,dz=Math.sin(a)*radius;
                vertex(b,matrix,x-dx,y,z-dz,texture.getU0(),texture.getV1());
                vertex(b,matrix,x+dx,y,z+dz,texture.getU1(),texture.getV1());
                vertex(b,matrix,x+dx*.65,y+height,z+dz*.65,texture.getU1(),texture.getV0());
                vertex(b,matrix,x-dx*.65,y+height,z-dz*.65,texture.getU0(),texture.getV0());count++;
            }
        }
        if(count==0){b.build();return;}
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_BLOCKS);
        try {BufferUploader.drawWithShader(b.buildOrThrow());}
        finally {RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();modelView.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void vertex(BufferBuilder b,org.joml.Matrix4f matrix,double x,double y,double z,float u,float v) {
        b.addVertex(matrix,(float)x,(float)y,(float)z).setUv(u,v).setColor(255,255,255,255);
    }
}
