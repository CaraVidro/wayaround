package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.KrakenMotion;
import net.caravidro.wayaround.ecology.KrakenAnimationClock;
import net.caravidro.wayaround.network.KrakenSceneS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** One continuous low-poly creature, with no world blocks, entities or collision. */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class KrakenSceneRenderer {
    private static KrakenSceneS2CPayload scene;
    private static ClientLevel owner;
    private static final KrakenAnimationClock clock = new KrakenAnimationClock();
    private static int lastAge = -1;
    private static boolean emerged, submerged;
    private static final List<Splash> splashes = new ArrayList<>();
    private record Splash(Vec3 center, long start, double radius) {}
    private KrakenSceneRenderer() {}

    public static void receive(KrakenSceneS2CPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null || !p.isSane()) return;
        if (owner != level) clear();
        owner = level;
        if (p.kind() == -1) {
            // Finish local contact/submergence even if the stop packet overtakes interpolation.
            if (scene == null || scene.kind() >= 3 || age() >= KrakenMotion.duration(scene.kind())) scene = null;
            return;
        }
        boolean fresh = scene == null || scene.kind() != p.kind() || scene.x() != p.x()
                || scene.z() != p.z() || p.age() < scene.age();
        scene = p;
        if (fresh) clock.reset(p.age(), level.getGameTime());
        else clock.synchronize(p.age(), level.getGameTime());
        if (fresh) { emerged = false; submerged = false; lastAge = p.age() - 1; }
    }
    private static void clear() { scene = null; splashes.clear(); owner = null; lastAge = -1; }
    private static double age() { return clock.age(); }
    private static Vec3 origin() { return new Vec3(scene.x()+0.5, KrakenMotion.waterSurface(scene.surface()), scene.z()+0.5); }
    private static Vec3 direction() { return new Vec3(scene.dx(),0,scene.dz()).normalize(); }
    private static Vec3 world(KrakenMotion.Point p) {
        Vec3 d = direction();
        if (d.lengthSqr() < 0.1) d = new Vec3(1,0,0);
        return origin().add(d.scale(p.x())).add(-d.z*p.z(), p.y(), d.x*p.z());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.level != owner) { clear(); return; }
        if (owner == null || mc.isPaused()) return;
        splashes.removeIf(s -> owner.getGameTime() - s.start > 70);
        if (scene == null) return;
        clock.advance(owner.getGameTime());
        int age = (int)age();
        int duration = KrakenMotion.duration(scene.kind());
        if (age > duration + 20) { scene = null; return; }
        if (scene.kind() < 3) {
            int riseAt = KrakenMotion.breachAge(scene.kind());
            int fallAt = KrakenMotion.impactAge(scene.kind());
            if (!emerged && lastAge < riseAt && age >= riseAt) {
                splashes.add(new Splash(origin(), owner.getGameTime(), scene.kind()==1?36:22)); emerged = true;
            }
            if (!submerged && lastAge < fallAt && age >= fallAt) {
                Vec3 end = scene.kind()==1 ? origin() : world(KrakenMotion.tentacle(1,fallAt,scene.kind()));
                splashes.add(new Splash(new Vec3(end.x,KrakenMotion.waterSurface(scene.surface()),end.z), owner.getGameTime(), scene.kind()==1?42:48));
                submerged = true;
            }
        } else if (age % 4 == 0 && mc.options.particles().get() != net.minecraft.client.ParticleStatus.MINIMAL) {
            // Eight particles per tick at most. Large volume is mesh, not particle count.
            for (int i=0;i<8;i++) {
                double a = age*.10+i*Math.PI/4;
                double radius = 5 + (age%36)*.45;
                owner.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                        scene.x()+Math.cos(a)*radius, scene.surface()-2,scene.z()+Math.sin(a)*radius,
                        0,.18,0);
            }
        }
        lastAge = age;
    }
    public static float boatRoll(Vec3 pos, float partial) {
        if (scene==null || owner==null || scene.kind()!=4 || pos.distanceToSqr(origin())>90*90) return 0;
        double age=clock.sample(partial);
        return (float)(Math.sin(age*.17)*12 * Math.sin(Math.PI*Math.min(1,age/200)));
    }
    private static boolean visibleScene(Vec3 camera) {
        if (camera.distanceToSqr(origin()) < 360 * 360) return true;
        if (scene.kind() == 0 || scene.kind() == 2)
            for (int i = 1; i <= 4; i++)
                if (camera.distanceToSqr(world(KrakenMotion.tentacle(i / 4.0, age(), scene.kind()))) < 360 * 360) return true;
        return false;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || owner==null
                || Minecraft.getInstance().level!=owner || (scene==null && splashes.isEmpty())) return;
        Vec3 camera=e.getCamera().getPosition();
        Matrix4f matrix=e.getPoseStack().last().pose();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        float partial=e.getPartialTick().getGameTimeDeltaPartialTick(true);
        int count=0;
        if(scene!=null && visibleScene(camera)) {
            double a=clock.sample(partial);
            Vec3 d=direction();
            Vec3 side=new Vec3(-d.z,0,d.x);
            double shade=Math.max(.25, owner.getSkyColor(camera, partial).length()/1.73);
            if(scene.kind()==5) {
                // A pair of eyes only: open, blink, then retreat into black water.
                Vec3 facing=d.scale(-1);
                double retreat=KrakenMotion.smooth((a-175)/110)*65;
                double opening=KrakenMotion.smooth(a/55)*(1-KrakenMotion.smooth((a-260)/40));
                for(double blink:new double[]{110,170}) if(a>=blink && a<blink+14) opening*=Math.abs((a-blink-7)/7);
                int alpha=(int)(220*(1-KrakenMotion.smooth((a-210)/90)));
                for(int sign:new int[]{-1,1}) {
                    Vec3 center=origin().add(d.scale(retreat)).add(side.scale(sign*4.5));
                    count+=eye(b,matrix,camera,center,side,3.2,.05+opening*1.6,171,129,53,alpha);
                    count+=eye(b,matrix,camera,center.add(facing.scale(.03)),side,.40,.04+opening*1.48,7,9,7,alpha);
                }
            } else if(scene.kind()==1) {
                double rise=KrakenMotion.emergence(a,1);
                Vec3 center=origin().add(0,-70+rise*84,0);
                // A tapered squid mantle, collar, broad fins and binocular eyes.
                List<Vec3> axis=new ArrayList<>(); List<Double> radii=new ArrayList<>();
                for(int i=0;i<=16;i++) {
                    double t=i/16.0;
                    axis.add(center.add(0,t*34,0));
                    radii.add(1.2+18*Math.pow(Math.sin(Math.PI*(.12+.88*t)),.8));
                }
                count+=tube(b,matrix,camera,axis,radii,10,shade,false);
                for(int sign:new int[]{-1,1}) {
                    Vec3 eye=center.add(side.scale(sign*12)).add(d.scale(12)).add(0,8,0);
                    count+=box(b,matrix,camera,eye,3.2,2.8,2.5,174,157,86,255);
                    count+=box(b,matrix,camera,eye.add(d.scale(2.2)),1.4,2.2,1.0,8,12,10,255);
                    Vec3 fin=center.add(side.scale(sign*18)).add(0,20,0);
                    count+=box(b,matrix,camera,fin,9,1.3,6,(int)(36*shade),(int)(65*shade),(int)(62*shade),255);
                }
                for(int arm=0;arm<8;arm++) {
                    double angle=arm*Math.PI/4;
                    List<Vec3> points=new ArrayList<>(); List<Double> widths=new ArrayList<>();
                    for(int i=0;i<=12;i++) {
                        double t=i/12.0, r=10+t*16;
                        points.add(center.add(Math.cos(angle)*r,-t*30+Math.sin(a*.055-t*4+arm)*t*4,Math.sin(angle)*r));
                        widths.add(3.2*(1-t)+.25);
                    }
                    count+=tube(b,matrix,camera,points,widths,6,shade,false);
                }
            } else if(scene.kind()<3) {
                int segments=camera.distanceToSqr(origin())>180*180?36:96;
                List<Vec3> points=new ArrayList<>(); List<Double> widths=new ArrayList<>();
                for(int i=0;i<=segments;i++) {
                    var p=KrakenMotion.tentacle(i/(double)segments,a,scene.kind());
                    points.add(world(p)); widths.add(p.radius());
                }
                count+=tube(b,matrix,camera,points,widths,8,shade,true);
                // Two rows of raised suction cups follow the inner curve.
                for(int i=3;i<segments-3;i+=(segments>36?3:2)) for(int sign:new int[]{-1,1}) {
                    double r=widths.get(i), cup=Math.max(.22,r*.29);
                    Vec3 c=points.get(i).add(side.scale(sign*r*.48)).add(d.scale(r*.78));
                    count+=box(b,matrix,camera,c,cup,cup*.65,cup,97,123,105,255);
                    count+=box(b,matrix,camera,c.add(d.scale(cup*.6)),cup*.52,cup*.4,cup*.52,24,37,33,255);
                }
            }
            // Opaque skin writes depth so rear faces/arms cannot overwrite the
            // front of the mantle. Foam and shadows use a separate transparent pass.
            if(count>0) draw(b,true); else b.build();
            b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            count=0;
            // Broad moving silhouette on the water, softened with nested bands.
            double pass=scene.kind()==4?(a/200.0-.5)*140:0;
            Vec3 shadow=origin().add(d.scale(pass)).add(0,.04,0);
            for(int ring=0;scene.kind()<5 && ring<5;ring++) count+=disc(b,matrix,camera,shadow,55-ring*7,0,5,12,18,12+ring*4);
            if(scene.kind()>=3 && scene.kind()<5) {
                for(int i=0;i<18;i++) {
                    double phase=(a*.035+i*.37)%1;
                    double angle=i*2.39996;
                    double r=5+(i%6)*3;
                    Vec3 bubble=origin().add(Math.cos(angle)*r,-7+phase*9,Math.sin(angle)*r);
                    double size=.65+(i%4)*.38;
                    count+=box(b,matrix,camera,bubble,size,size,size,138,193,202,(int)(90*(1-phase)));
                }
            }
        }
        for(Splash splash:splashes) {
            if(camera.distanceToSqr(splash.center)>360*360) continue;
            double t=(owner.getGameTime()-splash.start+partial)/70.0;
            double radius=splash.radius*(.15+.85*KrakenMotion.smooth(t));
            int alpha=(int)(190*(1-t));
            // Foam annulus and twenty-four tall, ballistic water sheets.
            for(int i=0;i<24;i++) {
                double angle=i*Math.PI/12, next=(i+1)*Math.PI/12;
                Vec3 p=splash.center.add(Math.cos(angle)*radius, .08,Math.sin(angle)*radius);
                Vec3 q=splash.center.add(Math.cos(next)*radius,.08,Math.sin(next)*radius);
                Vec3 r=splash.center.add(Math.cos(next)*(radius+4),.08,Math.sin(next)*(radius+4));
                Vec3 u=splash.center.add(Math.cos(angle)*(radius+4),.08,Math.sin(angle)*(radius+4));
                quad(b,matrix,camera,p,q,r,u,193,228,231,alpha);count++;
                double flight=Math.max(0,Math.sin(Math.PI*t));
                double height=(18+(i%5)*5)*flight;
                Vec3 center=splash.center.add(Math.cos(angle)*radius*.8,height*.5,Math.sin(angle)*radius*.8);
                count+=box(b,matrix,camera,center,1.8+2*t,height*.5+.1,1.8+2*t,160,207,217,alpha);
            }
        }
        if(count==0) { b.build(); return; }
        draw(b,false);
    }
    private static int eye(BufferBuilder b,Matrix4f matrix,Vec3 camera,Vec3 center,Vec3 side,double width,double height,int r,int g,int blue,int alpha) {
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12,n=(i+1)*Math.PI/12;
            Vec3 p=center.add(side.scale(Math.cos(a)*width)).add(0,Math.sin(a)*height,0);
            Vec3 q=center.add(side.scale(Math.cos(n)*width)).add(0,Math.sin(n)*height,0);
            quad(b,matrix,camera,center,p,q,center,r,g,blue,alpha);
        }
        return 24;
    }
    private static void draw(BufferBuilder b,boolean opaque) {
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();
        RenderSystem.depthMask(opaque);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try { BufferUploader.drawWithShader(b.buildOrThrow()); }
        finally { RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend(); }
    }
    private static int tube(BufferBuilder b,Matrix4f m,Vec3 camera,List<Vec3> points,List<Double> radii,
                            int sides,double shade,boolean inner) {
        Vec3[][] rings=new Vec3[points.size()][sides];
        for(int i=0;i<points.size();i++) {
            Vec3 tangent=points.get(Math.min(i+1,points.size()-1)).subtract(points.get(Math.max(0,i-1))).normalize();
            Vec3 normal=tangent.cross(Math.abs(tangent.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
            Vec3 other=tangent.cross(normal).normalize();
            for(int j=0;j<sides;j++) {
                double a=j*Math.PI*2/sides;
                rings[i][j]=points.get(i).add(normal.scale(Math.cos(a)*radii.get(i))).add(other.scale(Math.sin(a)*radii.get(i)));
            }
        }
        int count=0;
        for(int i=0;i<points.size()-1;i++) for(int j=0;j<sides;j++) {
            int k=(j+1)%sides;
            double light=shade*(.65+.35*Math.sin(j*Math.PI*2/sides)*Math.sin(j*Math.PI*2/sides));
            int r=(int)((inner&&j<2?62:30)*light),g=(int)((inner&&j<2?86:55)*light),bl=(int)(60*light);
            quad(b,m,camera,rings[i][j],rings[i][k],rings[i+1][k],rings[i+1][j],r,g,bl,255);count++;
        }
        return count;
    }
    private static int disc(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 c,double radius,double y,int r,int g,int bl,int alpha) {
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12,n=(i+1)*Math.PI/12;
            quad(b,m,camera,c.add(0,y,0),c.add(Math.cos(a)*radius,y,Math.sin(a)*radius),
                    c.add(Math.cos(n)*radius,y,Math.sin(n)*radius),c.add(0,y,0),r,g,bl,alpha);
        }
        return 24;
    }
    private static int box(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 c,double x,double y,double z,int r,int g,int bl,int alpha) {
        Vec3[] v=new Vec3[8];
        for(int i=0;i<8;i++)v[i]=c.add((i&1)==0?-x:x,(i&2)==0?-y:y,(i&4)==0?-z:z);
        int[][] faces={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
        for(int[] f:faces)quad(b,m,camera,v[f[0]],v[f[1]],v[f[2]],v[f[3]],r,g,bl,alpha);
        return 6;
    }
    private static void vertex(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 v,int r,int g,int blue,int alpha) {
        b.addVertex(m,(float)(v.x-camera.x),(float)(v.y-camera.y),(float)(v.z-camera.z)).setColor(r,g,blue,Math.max(0,alpha));
    }
    private static void quad(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int r,int g,int blue,int alpha) {
        vertex(b,m,camera,a,r,g,blue,alpha);
        vertex(b,m,camera,c,r,g,blue,alpha);
        vertex(b,m,camera,d,r,g,blue,alpha);
        vertex(b,m,camera,e,r,g,blue,alpha);
    }
}
