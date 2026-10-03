package net.caravidro.wayaround.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.performance.PerformanceProfiler;
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
        if(net.caravidro.wayaround.daybreak.client.DaysBreakClient.active())return;
        return mc.level!=null && mc.player!=null && mc.level.dimensionType().hasSkyLight()
                && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && !net.caravidro.wayaround.client.AntarcticClientLighting.isAntarctic(mc)
                && net.caravidro.wayaround.client.VoidDomainClientEffects.localInterior()==null
                && !net.caravidro.wayaround.ecology.client.DeepOceanClientVisibility.submerged(mc.player,mc.level,mc.gameRenderer.getMainCamera().getPosition()) && WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                && WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER);
    }
    private static void clear(){
        for(var tiles:TILES)java.util.Arrays.fill(tiles,null);
        row=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        long started=PerformanceProfiler.begin(PerformanceProfiler.Section.CLOUD_SHADOW_UPDATE);
        try{update();}finally{PerformanceProfiler.end(PerformanceProfiler.Section.CLOUD_SHADOW_UPDATE,started);}
    }
    private static void update(){
        var mc=Minecraft.getInstance();
        if(net.caravidro.wayaround.daybreak.client.DaysBreakClient.active())return;
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
                    if(density==1)break;
                }
                tile.target=CloudStormMath.shadowAlpha(density);
            }
        }
        for(var tiles:TILES)for(var tile:tiles){
            if(tile==null)continue;
            tile.previousAlpha=tile.alpha;
            if(time-tile.sampled>WIDTH/ROWS_PER_TICK+8)tile.target=0;
            if(tile.alpha==0&&tile.target==0)continue;
            tile.alpha=CloudStormMath.approachShadow(tile.alpha,tile.target);
            if(tile.target==0&&tile.alpha<.01)tile.alpha=0;
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!enabled())return;
        long started=PerformanceProfiler.begin(PerformanceProfiler.Section.CLOUD_SHADOW_RENDER);
        try{draw(e);}finally{PerformanceProfiler.end(PerformanceProfiler.Section.CLOUD_SHADOW_RENDER,started);}
    }
    private static int alpha(Tile tile,Vec3 camera,float partial){
        if(tile==null||(tile.alpha<=1&&tile.previousAlpha<=1))return 0;
        double edge=CloudStormMath.shadowEdge(Math.max(Math.abs(tile.x+.5-camera.x),Math.abs(tile.z+.5-camera.z)),RADIUS);
        return (int)Math.round((tile.previousAlpha+(tile.alpha-tile.previousAlpha)*partial)*edge);
    }
    private static void draw(RenderLevelStageEvent e){
        var camera=e.getCamera().getPosition();var m=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        boolean any=false;
        float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);
        int cx=(int)Math.floor(camera.x),cz=(int)Math.floor(camera.z);
        for(int z=cz-RADIUS;z<=cz+RADIUS;z++){
            int rz=Math.floorMod(z,WIDTH);
            for(int x=cx-RADIUS;x<=cx+RADIUS;){
                Tile t=TILES[Math.floorMod(x,WIDTH)][rz];
                int shade=t!=null&&t.x==x&&t.z==z?alpha(t,camera,partial):0;
                if(shade<=1){x++;continue;}
                int end=x+1;
                // Merge only exact coplanar/equal-alpha neighbors. No lost terrain
                // detail, no approximated gradient or duplicated blended area.
                while(end<=cx+RADIUS){
                    Tile next=TILES[Math.floorMod(end,WIDTH)][rz];
                    if(next==null||next.x!=end||next.z!=z||next.y!=t.y||alpha(next,camera,partial)!=shade)break;
                    end++;
                }
                any=true;
                b.addVertex(m,(float)(x-camera.x),(float)(t.y-camera.y),(float)(z-camera.z)).setColor(4,8,16,shade);
                b.addVertex(m,(float)(x-camera.x),(float)(t.y-camera.y),(float)(z+1-camera.z)).setColor(4,8,16,shade);
                b.addVertex(m,(float)(end-camera.x),(float)(t.y-camera.y),(float)(z+1-camera.z)).setColor(4,8,16,shade);
                b.addVertex(m,(float)(end-camera.x),(float)(t.y-camera.y),(float)(z-camera.z)).setColor(4,8,16,shade);
                x=end;
            }
        }
        if(!any){b.build();return;}
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}
        finally{RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();}
    }
}
