package net.caravidro.wayaround.nature.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.nature.*;
import net.caravidro.wayaround.client.weather.ClientWind;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.voice.client.VoicePlayback;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class BirdAmbientClient {
    private record Chirp(long at,double x,double y,double z,float pitch){}
    private static final List<Chirp> CHIRPS=new ArrayList<>();
    private static ClientLevel owner;
    private static long nextChorus,migrationStart=-1,migrationSeed;
    private static Vec3 origin=Vec3.ZERO;
    public static void receive(NatureAmbientPayload p){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!p.sane())return;
        if(p.kind()==0){if(owner!=mc.level){owner=mc.level;CHIRPS.clear();nextChorus=0;}migrationStart=mc.level.getGameTime();migrationSeed=p.seed();origin=new Vec3(p.x(),p.y(),p.z());}
        else{
            var entity=mc.level.getEntity(p.entityId());
            if(!(entity instanceof WoodlandBirdEntity bird)||bird.species()!=2||mc.player.distanceToSqr(bird)>24*24)return;
            double gain=Math.max(0,1-mc.player.distanceTo(bird)/24.0);
            byte[] pcm=p.pcm().clone();
            for(int i=0;i+1<pcm.length;i+=2){int s=(short)((pcm[i]&255)|(pcm[i+1]<<8));s=(int)(s*gain);pcm[i]=(byte)s;pcm[i+1]=(byte)(s>>8);}
            VoicePlayback.enqueue(pcm);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){owner=mc.level;CHIRPS.clear();nextChorus=0;migrationStart=-1;}
        if(owner==null||mc.player==null||!WorldFeatureRuntime.clientEnabled(WorldFeature.LIVING_VEGETATION)){CHIRPS.clear();migrationStart=-1;return;}
        long now=owner.getGameTime();
        if(migrationStart>=0&&now-migrationStart>900)migrationStart=-1;
        var it=CHIRPS.iterator();while(it.hasNext()){var c=it.next();if(c.at<=now){owner.playLocalSound(c.x,c.y,c.z,SoundEvents.PARROT_AMBIENT,SoundSource.AMBIENT,.16F,c.pitch,false);it.remove();}}
        if(now%40==0&&now>=nextChorus&&(owner.getBiome(mc.player.blockPosition()).is(BiomeTags.IS_FOREST)||owner.getBiome(mc.player.blockPosition()).is(BiomeTags.IS_JUNGLE))){
            var birds=owner.getEntitiesOfClass(WoodlandBirdEntity.class,mc.player.getBoundingBox().inflate(28));
            if(!birds.isEmpty()){
                var bird=birds.get(0);int count=5+owner.random.nextInt(6);
                for(int i=0;i<count;i++){double angle=owner.random.nextDouble()*Math.PI*2,r=7+owner.random.nextDouble()*16;CHIRPS.add(new Chirp(now+i*10+owner.random.nextInt(20),mc.player.getX()+Math.cos(angle)*r,mc.player.getY()+3+owner.random.nextInt(8),mc.player.getZ()+Math.sin(angle)*r,bird.getVoicePitch()*(.8F+owner.random.nextFloat()*.35F)));}
                nextChorus=now+300+owner.random.nextInt(300);
            }
        }
        // Ambient falling leaves: at most one new mote per six ticks, local only.
        if(now%6==0&&!mc.player.isUnderWater())for(int tries=0;tries<8;tries++){
            BlockPos p=mc.player.blockPosition().offset(owner.random.nextInt(33)-16,owner.random.nextInt(15),owner.random.nextInt(33)-16);
            if(!owner.hasChunkAt(p)||!owner.getBlockState(p).is(BlockTags.LEAVES)||!owner.getBlockState(p.below()).isAir())continue;
            owner.addParticle(WayAroundParticles.WIND_LEAF.get(),p.getX()+.5,p.getY()-.03,p.getZ()+.5,ClientWind.getX()*.025,-.02,ClientWind.getZ()*.025);break;
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||migrationStart<0||mc.level!=owner||mc.player==null||mc.player.isUnderWater())return;
        var camera=e.getCamera().getPosition();var matrix=e.getModelViewMatrix();
        double age=owner.getGameTime()-migrationStart+e.getPartialTick().getGameTimeDeltaPartialTick(false);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        int count=24+(int)Math.floorMod(migrationSeed,13);
        for(int i=0;i<count;i++){
            int rank=i/2;double side=(i%2==0?1:-1)*rank*3;
            double x=origin.x-160+age*.48+side,z=origin.z-50+age*.16-rank*3,y=origin.y+Math.sin(i*1.9)*3;
            double flap=BirdMigrationMath.flap(age,i);
            birdQuad(b,matrix,camera,x,y,z,-.18,0,-.45,.18,0,-.45,.18,0,.45,-.18,0,.45);
            birdQuad(b,matrix,camera,x,y,z,0,0,.1,-1.2,flap,-.05,-.9,flap,-.45,0,0,-.2);
            birdQuad(b,matrix,camera,x,y,z,0,0,.1,1.2,flap,-.05,.9,flap,-.45,0,0,-.2);
        }
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.disableCull();RenderSystem.disableBlend();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();RenderSystem.applyModelViewMatrix();
        try{BufferUploader.drawWithShader(b.buildOrThrow());}finally{RenderSystem.enableCull();modelView.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void birdQuad(BufferBuilder b,org.joml.Matrix4f m,Vec3 c,double x,double y,double z,
                                 double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz) {
        quad(b,m,c,x+BirdMigrationMath.x(ax,az),y+ay,z+BirdMigrationMath.z(ax,az),
                x+BirdMigrationMath.x(bx,bz),y+by,z+BirdMigrationMath.z(bx,bz),
                x+BirdMigrationMath.x(cx,cz),y+cy,z+BirdMigrationMath.z(cx,cz),
                x+BirdMigrationMath.x(dx,dz),y+dy,z+BirdMigrationMath.z(dx,dz));
    }
    private static void quad(BufferBuilder b,org.joml.Matrix4f m,Vec3 c,double x,double y,double z,double x2,double y2,double z2,double x3,double y3,double z3,double x4,double y4,double z4){
        b.addVertex(m,(float)(x-c.x),(float)(y-c.y),(float)(z-c.z)).setColor(55,62,70,255);b.addVertex(m,(float)(x2-c.x),(float)(y2-c.y),(float)(z2-c.z)).setColor(55,62,70,255);b.addVertex(m,(float)(x3-c.x),(float)(y3-c.y),(float)(z3-c.z)).setColor(55,62,70,255);b.addVertex(m,(float)(x4-c.x),(float)(y4-c.y),(float)(z4-c.z)).setColor(55,62,70,255);
    }
}
