import net.caravidro.wayaround.ecology.EcologicalHistory;
public final class EcologicalHistoryTest {
    private static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
    public static void main(String[] args){
        int active=0;
        for(int i=-500;i<500;i++){
            long phase=EcologicalHistory.phase(42,i,900000,8000);
            check(phase>=0&&phase<8000,"Bounded historical phase");
            check(phase==EcologicalHistory.phase(42,i,900000+8000*100L,8000),"History independent of observer visits or intermediate ticks");
            check((phase+79)%8000==EcologicalHistory.phase(42,i,900079,8000),"Smooth calendar progression");
            if(phase<1100)active++;
        }
        check(active>70&&active<220,"Different places have distinct activity phases");
        check(EcologicalHistory.elapsed(90000,1,24000)==24000,"Bounded catch-up");
        check(EcologicalHistory.elapsed(1,100,24000)==0,"Clock reversal safe");
        System.out.println("Observer-independent ecological history contracts passed");
    }
}
