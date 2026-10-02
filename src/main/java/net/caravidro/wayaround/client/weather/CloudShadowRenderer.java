package net.caravidro.wayaround.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Small depth-tested tiles on flat exposed surfaces; no giant intersecting mesh. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class CloudShadowRenderer {
    private record Tile(double x,double y,double z,int alpha) {}
    private static final List<Tile> TILES=new ArrayList<>();
    private static ClientLevel owner;
    private static boolean enabled() {
        var mc=Minecraft.getInstance();
        return mc.level!=null && mc.player!=null && mc.level.dimensionType().hasSkyLight()
                && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && !net.caravidro.wayaround.client.AntarcticClientLighting.isAntarctic(mc)
                && net.caravidro.wayaround.client.VoidDomainClientEffects.localInterior()==null
                && !mc.player.isUnderWater() && WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                && WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){TILES.clear();owner=mc.level;}
        if(!enabled()){TILES.clear();return;}
        if(mc.level.getGameTime()%16!=0)return;
        TILES.clear();
        double angle=mc.level.getSunAngle(1);
        Vec3 sun=new Vec3(-Math.sin(angle),Math.cos(angle),0);
        if(sun.y<.22)return;
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        long time=mc.level.getGameTime();
        var cells=LocalWeatherField.nearbyCells(mc.level,camera.x,camera.z,time,760);
        int cx=BlockPos.containing(camera).getX(),cz=BlockPos.containing(camera).getZ();
        for(int dx=-20;dx<=20;dx+=4)for(int dz=-20;dz<=20;dz+=4){
            int x=Math.floorDiv(cx,4)*4+dx,z=Math.floorDiv(cz,4)*4+dz;
            if(!mc.level.hasChunkAt(new BlockPos(x,0,z))||!mc.level.hasChunkAt(new BlockPos(x+3,0,z+3)))continue;
            int y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z);
            // Only flat, solid full tops. Water, steep edges, leaves, caves and
            // the camera's immediate surroundings never receive these quads.
            if(Math.abs(camera.x-x)<7&&Math.abs(camera.z-z)<7)continue;
            if(!mc.level.getBlockState(new BlockPos(x,y-1,z)).isSolidRender(mc.level,new BlockPos(x,y-1,z)))continue;
            if(mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x+3,z)!=y
                    ||mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z+3)!=y
                    ||mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x+3,z+3)!=y)continue;
            Vec3 from=new Vec3(x+2,y+.12,z+2);
            // The short solar ray rejects walls over the receiving tile.
            if(mc.level.clip(new ClipContext(from,from.add(sun.scale(12)),ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,mc.player)).getType()!=HitResult.Type.MISS)continue;
            float density=0;
            for(var cell:cells){
                double distance=(cell.y()-y)/sun.y;
                if(distance<0)continue;
                density=Math.max(density,cell.densityAt(from.x+sun.x*distance,from.z));
            }
            double fade=1-Math.max(Math.abs(dx),Math.abs(dz))/24.0;
            int alpha=(int)(48*density*fade*sun.y);
            if(alpha>2)TILES.add(new Tile(x,y+.018,z,alpha));
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || TILES.isEmpty()||!enabled())return;
        var camera=e.getCamera().getPosition();var m=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(var t:TILES){
            for(int[] corner:new int[][]{{0,0},{0,4},{4,4},{4,0}})
                b.addVertex(m,(float)(t.x+corner[0]-camera.x),(float)(t.y-camera.y),(float)(t.z+corner[1]-camera.z)).setColor(4,8,16,t.alpha);
        }
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}
        finally{RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();}
    }
}
