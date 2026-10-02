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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Stable downward cloud coverage with persistent world-column receivers. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class CloudShadowRenderer {
    private static final int RADIUS=56, WIDTH=RADIUS*2+1, ROWS_PER_TICK=5;
    private static final class Tile {
        final int x,z;
        double y,previousAlpha,alpha,target;
        long sampled;
        Tile(int x,int z){this.x=x;this.z=z;}
    }
    private static final Tile[][] TILES=new Tile[WIDTH][WIDTH];
    private static ClientLevel owner;
    private static int row;
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
        row=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){clear();owner=mc.level;}
        if(!enabled()){clear();return;}
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        int cx=(int)Math.floor(camera.x),cz=(int)Math.floor(camera.z);
        long time=mc.level.getGameTime();
        var cells=LocalWeatherField.nearbyCells(mc.level,camera.x,camera.z,time,RADIUS+16);
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<ROWS_PER_TICK;i++){
            int rx=row;row=(row+1)%WIDTH;
            // A toroidal cache keeps unchanged world columns when the viewer moves.
            int startX=cx-RADIUS,x=startX+Math.floorMod(rx-startX,WIDTH);
            for(int z=cz-RADIUS;z<=cz+RADIUS;z++){
                int rz=Math.floorMod(z,WIDTH);
                Tile tile=TILES[rx][rz];
                if(tile==null||tile.x!=x||tile.z!=z){
                    tile=new Tile(x,z);TILES[rx][rz]=tile;
                }
                tile.target=0;tile.sampled=time;
                pos.set(x,0,z);
                if(!mc.level.hasChunkAt(pos))continue;
                int y=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)-1;
                var state=mc.level.getBlockState(pos.set(x,y,z));
                var fluid=mc.level.getFluidState(pos);
                double top;
                if(!fluid.isEmpty()){
                    top=y+fluid.getHeight(mc.level,pos);
                }else{
                    // Ignore noncolliding grass/flowers, preserving each terrain top.
                    var shape=state.getCollisionShape(mc.level,pos);
                    for(int down=0;shape.isEmpty()&&down<4;down++){
                        y--;shape=mc.level.getBlockState(pos.set(x,y,z)).getCollisionShape(mc.level,pos);
                    }
                    if(shape.isEmpty())continue;
                    top=y+shape.bounds().maxY;
                }
                tile.y=top+.012;
                double density=0;
                for(var cell:cells){
                    if(cell.y()<=top)continue;
                    // A continuous canopy footprint avoids holes that pop when a
                    // distant mesh is created, rebuilt, culled, or changes LOD.
                    double cover=CloudStormMath.downwardShadowDensity(x+.5-cell.x(),z+.5-cell.z(),cell.radius());
                    density=1-(1-density)*(1-cover);
                }
                tile.target=CloudStormMath.shadowAlpha(density);
            }
        }
        for(var tiles:TILES)for(var tile:tiles){
            if(tile==null)continue;
            tile.previousAlpha=tile.alpha;
            if(time-tile.sampled>WIDTH/ROWS_PER_TICK+8)tile.target=0;
            tile.alpha=CloudStormMath.approachShadow(tile.alpha,tile.target);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!enabled())return;
        var camera=e.getCamera().getPosition();var m=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        boolean any=false;
        float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(var tiles:TILES)for(var t:tiles){
            if(t==null)continue;
            double edge=CloudStormMath.shadowEdge(Math.max(Math.abs(t.x+.5-camera.x),Math.abs(t.z+.5-camera.z)),RADIUS);
            int alpha=(int)Math.round((t.previousAlpha+(t.alpha-t.previousAlpha)*partial)*edge);
            if(alpha<=1)continue;
            any=true;
            b.addVertex(m,(float)(t.x-camera.x),(float)(t.y-camera.y),(float)(t.z-camera.z)).setColor(4,8,16,alpha);
            b.addVertex(m,(float)(t.x-camera.x),(float)(t.y-camera.y),(float)(t.z+1-camera.z)).setColor(4,8,16,alpha);
            b.addVertex(m,(float)(t.x+1-camera.x),(float)(t.y-camera.y),(float)(t.z+1-camera.z)).setColor(4,8,16,alpha);
            b.addVertex(m,(float)(t.x+1-camera.x),(float)(t.y-camera.y),(float)(t.z-camera.z)).setColor(4,8,16,alpha);
        }
        if(!any){b.build();return;}
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}
        finally{RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();}
    }
}
