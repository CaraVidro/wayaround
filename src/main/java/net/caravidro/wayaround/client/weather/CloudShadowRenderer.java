package net.caravidro.wayaround.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.CloudStormMath;
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

/** Bounded rolling solar projection onto individual loaded terrain tops. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class CloudShadowRenderer {
    private static final int RADIUS=40, WIDTH=RADIUS*2+1, ROWS_PER_TICK=5;
    private record Tile(double x,double y,double z,int alpha) {}
    private static final Tile[][] TILES=new Tile[WIDTH][WIDTH];
    private static ClientLevel owner;
    private static int anchorX=Integer.MIN_VALUE,anchorZ,row;
    private static boolean enabled() {
        var mc=Minecraft.getInstance();
        return mc.level!=null && mc.player!=null && mc.level.dimensionType().hasSkyLight()
                && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && !net.caravidro.wayaround.client.AntarcticClientLighting.isAntarctic(mc)
                && net.caravidro.wayaround.client.VoidDomainClientEffects.localInterior()==null
                && !mc.player.isUnderWater() && WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                && WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER);
    }
    private static void clear(){
        for(var tiles:TILES)java.util.Arrays.fill(tiles,null);
        anchorX=Integer.MIN_VALUE;row=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){clear();owner=mc.level;}
        if(!enabled()){clear();return;}
        double angle=mc.level.getSunAngle(1);
        Vec3 sun=new Vec3(-Math.sin(angle),Math.cos(angle),0);
        if(sun.y<.18){clear();return;}
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        int cx=(int)Math.floor(camera.x),cz=(int)Math.floor(camera.z);
        if(anchorX==Integer.MIN_VALUE || Math.abs(cx-anchorX)>8 || Math.abs(cz-anchorZ)>8){
            clear();anchorX=cx;anchorZ=cz;
        }
        long time=mc.level.getGameTime();
        // Fetch around the solar-projected footprint, not just the viewer.
        double projectedX=camera.x+CloudStormMath.solarOffset(220-camera.y,sun.x,sun.y);
        var cells=LocalWeatherField.nearbyCells(mc.level,projectedX,camera.z,time,760);
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<ROWS_PER_TICK;i++){
            int rx=row;row=(row+1)%WIDTH;
            for(int rz=0;rz<WIDTH;rz++){
                TILES[rx][rz]=null;
                int x=anchorX+rx-RADIUS,z=anchorZ+rz-RADIUS;
                pos.set(x,0,z);
                if(!mc.level.hasChunkAt(pos))continue;
                int y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)-1;
                // Ignore thin grass/flowers above a solid receiving top.
                var shape=mc.level.getBlockState(pos.set(x,y,z)).getCollisionShape(mc.level,pos);
                for(int down=0;shape.isEmpty()&&down<4;down++){
                    y--;shape=mc.level.getBlockState(pos.set(x,y,z)).getCollisionShape(mc.level,pos);
                }
                if(shape.isEmpty() || !mc.level.getFluidState(pos).isEmpty())continue;
                var box=shape.bounds();
                double top=y+box.maxY;
                Vec3 from=new Vec3(x+.5,top+.06,z+.5);
                if(mc.level.clip(new ClipContext(from,from.add(sun.scale(12)),ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,mc.player)).getType()!=HitResult.Type.MISS)continue;
                float density=0;
                for(var cell:cells){
                    if(cell.y()<=top)continue;
                    double offset=CloudStormMath.solarOffset(cell.y()-top,sun.x,sun.y);
                    density=Math.max(density,LivingCloudRenderer.shadowDensity(cell,from.x+offset,from.z));
                }
                int alpha=CloudStormMath.shadowAlpha(density,sun.y,
                        Math.max(Math.abs(x-camera.x),Math.abs(z-camera.z)),RADIUS);
                if(alpha>2)TILES[rx][rz]=new Tile(x,top+.012,z,alpha);
            }
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!enabled())return;
        var camera=e.getCamera().getPosition();var m=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        boolean any=false;
        for(var tiles:TILES)for(var t:tiles){
            if(t==null)continue;
            any=true;
            b.addVertex(m,(float)(t.x-camera.x),(float)(t.y-camera.y),(float)(t.z-camera.z)).setColor(4,8,16,t.alpha);
            b.addVertex(m,(float)(t.x-camera.x),(float)(t.y-camera.y),(float)(t.z+1-camera.z)).setColor(4,8,16,t.alpha);
            b.addVertex(m,(float)(t.x+1-camera.x),(float)(t.y-camera.y),(float)(t.z+1-camera.z)).setColor(4,8,16,t.alpha);
            b.addVertex(m,(float)(t.x+1-camera.x),(float)(t.y-camera.y),(float)(t.z-camera.z)).setColor(4,8,16,t.alpha);
        }
        if(!any){b.build();return;}
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}
        finally{RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();}
    }
}
