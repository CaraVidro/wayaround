import net.caravidro.wayaround.ecology.KrakenMotion;

/** Standalone geometry invariants: no game registry or graphics context needed. */
public final class KrakenMotionTest {
    public static void main(String[] args) {
        check(KrakenMotion.smooth(-1)==0 && KrakenMotion.smooth(2)==1,"clamped phases");
        double peak=0;
        for(int kind:new int[]{0,2}) {
            for(int age=0;age<=KrakenMotion.duration(kind);age++) {
                var root=KrakenMotion.tentacle(0,age,kind);
                check(Math.abs(root.y()+24)<1e-9,"anchored depth");
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
        check(peak>220,"breaches well above clouds");
        check(KrakenMotion.tentacle(0,200,2).x()>40,"migrating root moves");
        System.out.println("KrakenMotion: geometry invariants passed; peak="+peak);
    }
    private static void check(boolean test,String name){if(!test)throw new AssertionError(name);}
}
