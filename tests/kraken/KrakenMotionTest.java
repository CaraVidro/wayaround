import net.caravidro.wayaround.ecology.KrakenMotion;

/** Standalone geometry invariants: no game registry or graphics context needed. */
public final class KrakenMotionTest {
    public static void main(String[] args) {
        check(KrakenMotion.smooth(-1)==0 && KrakenMotion.smooth(2)==1,"clamped phases");
        check(KrakenMotion.waterSurface(62)>63&&KrakenMotion.waterSurface(-20)>-19,"foam and contact are above the top water face");
        double peak=0;
        for(int kind:new int[]{0,2}) {
            for(int age=0;age<=KrakenMotion.duration(kind);age++) {
                var root=KrakenMotion.tentacle(0,age,kind);
                check(root.y() <= .01 && root.y() >= -115,"root stays below the surface");
                if(age<=170)check(Math.abs(root.y()+24)<1e-9,"anchored before throwing");
                for(int i=0;i<=64;i++) {
                    var p=KrakenMotion.tentacle(i/64.0,age,kind);
                    check(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z()),"finite mesh");
                    check(p.radius()>0&&p.radius()<=8,"bounded taper");
                    if(i>0)check(p.radius()<KrakenMotion.tentacle((i-1)/64.0,age,kind).radius(),"continuous taper");
                    peak=Math.max(peak,p.y());
                }
            }
            check(KrakenMotion.tentacle(1,0,kind).y()<0,"starts submerged");
            check(KrakenMotion.tentacle(1,KrakenMotion.duration(kind),kind).y()<0,"ends submerged");
            check(KrakenMotion.tentacle(1,235,kind).x()>100,"throws across ocean");
        }
        for(int kind=0;kind<3;kind++) {
            int breach=KrakenMotion.breachAge(kind), impact=KrakenMotion.impactAge(kind);
            check(breach>0 && impact>breach && impact<KrakenMotion.duration(kind),"surface crossings");
            if(kind!=1) {
                check(KrakenMotion.tentacle(1,impact-1,kind).y()>0,"splash before contact rejected");
                check(KrakenMotion.tentacle(1,impact,kind).y()<=0,"splash at contact");
            }
        }
        int touched=0;
        for(int i=1;i<=16;i++)for(int age=150;age<KrakenMotion.duration(0);age++)
            if(KrakenMotion.contact(i/16.0,age-1,age,0)) { touched++;break; }
        check(touched>=14,"splash contacts span the tentacle body, not just its tip");
        check(peak>220,"breaches well above clouds");
        check(KrakenMotion.tentacle(0,200,2).x()>40,"migrating root moves");
        System.out.println("KrakenMotion: geometry invariants passed; peak="+peak);
    }
    private static void check(boolean test,String name){if(!test)throw new AssertionError(name);}
}
