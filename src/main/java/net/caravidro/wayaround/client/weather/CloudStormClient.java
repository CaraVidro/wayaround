package net.caravidro.wayaround.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.client.VoidDomainClientEffects;
import net.caravidro.wayaround.network.CloudStormS2CPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.caravidro.wayaround.worldgen.weather.local.CloudStormMath;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Partial internal flashes and delayed positional sounds, never a sound loop. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class CloudStormClient {
    private record Flash(CloudStormS2CPayload event,long start,List<Vec3> bolt) {}
    private record Thunder(Vec3 source,long due,float strength) {}
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static final List<Thunder> THUNDER=new ArrayList<>();
    private static final java.util.Map<Long,Long> GUSTS=new java.util.HashMap<>();
    private static ClientLevel owner;
    private static GustSound gust;
    private static long nextGust;
    private CloudStormClient(){}
    private static boolean enabled(){
        var mc=Minecraft.getInstance();
        return mc.level!=null&&mc.player!=null&&mc.level.dimension().equals(Level.OVERWORLD)
                &&WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                &&WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_WEATHER)
                &&!AntarcticClientLighting.isAntarctic(mc)&&VoidDomainClientEffects.localInterior()==null;
    }
    private static void clear(){
        FLASHES.clear();THUNDER.clear();GUSTS.clear();nextGust=0;
        if(gust!=null)Minecraft.getInstance().getSoundManager().stop(gust);
        gust=null;
    }
    public static void receive(CloudStormS2CPayload p){
        var mc=Minecraft.getInstance();
        if(!p.isSane()||!enabled())return;
        if(owner!=mc.level){clear();owner=mc.level;}
        Vec3 source=new Vec3(p.x(),p.y(),p.z());long now=owner.getGameTime();
        if(p.kind()==2){playGust(source,p.strength());return;}
        if(FLASHES.size()>=12)FLASHES.remove(0);
        FLASHES.add(new Flash(p,now,bolt(p,now)));
        if(THUNDER.size()<24)THUNDER.add(new Thunder(source,now+CloudStormMath.soundDelay(mc.player.getEyePosition().distanceTo(source)),p.strength()));
    }
    private static List<Vec3> bolt(CloudStormS2CPayload p,long time){
        if(p.kind()!=1)return List.of();
        Random random=new Random(p.cellId()^time);
        var points=new ArrayList<Vec3>(14);points.add(new Vec3(p.x(),p.y(),p.z()));
        for(int i=1;i<13;i++){
            double t=i/13.0;
            points.add(new Vec3(p.x()+(random.nextDouble()-.5)*22,p.y()+(p.groundY()-p.y())*t,p.z()+(random.nextDouble()-.5)*22));
        }
        points.add(new Vec3(p.x(),p.groundY()+.1,p.z()));return List.copyOf(points);
    }
    public static float glow(long cellId,double x,double y,double z,long time){
        float result=0;
        for(Flash f:FLASHES){
            if(f.event.cellId()!=cellId)continue;
            double dx=x-f.event.x(),dy=y-f.event.y(),dz=z-f.event.z();
            double radius=f.event.kind()==1?110:75;
            double patch=Math.exp(-(dx*dx+dy*dy*1.8+dz*dz)/(radius*radius*.55));
            result=Math.max(result,(float)(CloudStormMath.flash(time-f.start)*patch*f.event.strength()));
        }
        return result;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){clear();owner=mc.level;}
        if(!enabled()){clear();return;}
        if(mc.isPaused())return;
        long now=owner.getGameTime();
        FLASHES.removeIf(f->now-f.start>20);
        var it=THUNDER.iterator();
        while(it.hasNext()){
            Thunder t=it.next();if(t.due>now)continue;
            double distance=mc.player.getEyePosition().distanceTo(t.source);
            float exposure=owner.canSeeSky(mc.player.blockPosition())?1:.32F;
            if(mc.player.isUnderWater())exposure*=.25F;
            float volume=(float)(t.strength*exposure*Math.max(.04,1-distance/1000.0));
            mc.getSoundManager().play(new WeatherSound(SoundEvents.LIGHTNING_BOLT_THUNDER,t.source,volume,.70F,owner));
            it.remove();
        }
        if(now%20!=0||now<nextGust)return;
        GUSTS.entrySet().removeIf(entry->entry.getValue()<now-2400);
        if(GUSTS.size()>128)GUSTS.clear();
        for(var cell:LocalWeatherField.nearbyCells(owner,mc.player.getX(),mc.player.getZ(),now,400)){
            if(cell.storm()<.25||GUSTS.getOrDefault(cell.id(),0L)>now)continue;
            double weight=CloudStormMath.gustWeight(mc.player.getX()-cell.x(),mc.player.getZ()-cell.z(),
                    LocalWeatherField.windX(now),LocalWeatherField.windZ(now),cell.radius());
            if(weight<.22||owner.random.nextFloat()>.18F)continue;
            Vec3 source=new Vec3(cell.x()+LocalWeatherField.windX(now)*cell.radius()*.75,
                    mc.player.getEyeY(),cell.z()+LocalWeatherField.windZ(now)*cell.radius()*.75);
            playGust(source,(float)(weight*(.25+.45*cell.storm())));
            GUSTS.put(cell.id(),now+1000+owner.random.nextInt(1000));break;
        }
    }
    private static void playGust(Vec3 source,float strength){
        var mc=Minecraft.getInstance();
        if(owner==null||mc.player==null||(gust!=null&&!gust.isStopped())||owner.getGameTime()<nextGust)return;
        float exposure=owner.canSeeSky(mc.player.blockPosition())?1:.20F;
        gust=new GustSound(source,strength*exposure,owner);
        mc.getSoundManager().play(gust);
        nextGust=owner.getGameTime()+300+owner.random.nextInt(500);
    }
    private static class WeatherSound extends AbstractTickableSoundInstance {
        final ClientLevel level;
        WeatherSound(SoundEvent event,Vec3 source,float volume,float pitch,ClientLevel level){
            super(event,SoundSource.WEATHER,RandomSource.create());
            this.level=level;this.x=source.x;this.y=source.y;this.z=source.z;this.volume=volume;this.pitch=pitch;
            this.looping=false;this.relative=false;this.attenuation=SoundInstance.Attenuation.NONE;
        }
        @Override public void tick(){if(Minecraft.getInstance().level!=level||!enabled())stop();}
    }
    private static final class GustSound extends WeatherSound {
        private int age;private final float peak;
        GustSound(Vec3 source,float peak,ClientLevel level){super(WayAroundSounds.BLIZZARD_WIND.get(),source,0,.85F,level);this.peak=peak;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){super.tick();age++;volume=(float)(peak*Math.sin(Math.PI*Math.min(1,age/100.0)));if(age>=100)stop();}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||FLASHES.isEmpty()||!enabled()
                || (e.getCamera().getEntity()!=null&&e.getCamera().getEntity().isUnderWater()))return;
        var camera=e.getCamera().getPosition();var m=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);int count=0;
        long now=owner.getGameTime();
        for(Flash f:FLASHES){
            float pulse=CloudStormMath.flash(now-f.start);if(pulse<.08)continue;
            for(int i=0;i<f.bolt.size()-1;i++){
                Vec3 a=f.bolt.get(i),c=f.bolt.get(i+1);
                double width=i<3?1.1:.60;
                quad(b,m,camera,a.add(-width,0,0),a.add(width,0,0),c.add(width,0,0),c.add(-width,0,0),(int)(230*pulse));
                quad(b,m,camera,a.add(0,0,-width),a.add(0,0,width),c.add(0,0,width),c.add(0,0,-width),(int)(230*pulse));count+=2;
                if(i==6){
                    Vec3 branch=c.add(14,-18,9);
                    quad(b,m,camera,c.add(-.3,0,0),c.add(.3,0,0),branch.add(.3,0,0),branch.add(-.3,0,0),(int)(160*pulse));count++;
                }
            }
        }
        if(count==0){b.build();return;}
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();RenderSystem.depthMask(false);
        RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{BufferUploader.drawWithShader(b.buildOrThrow());}finally{RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();}
    }
    private static void quad(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int alpha){
        vertex(b,m,camera,a,alpha);vertex(b,m,camera,c,alpha);vertex(b,m,camera,d,alpha);vertex(b,m,camera,e,alpha);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 p,int alpha){
        b.addVertex(m,(float)(p.x-camera.x),(float)(p.y-camera.y),(float)(p.z-camera.z)).setColor(210,224,255,alpha);
    }
}
