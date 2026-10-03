import net.caravidro.wayaround.nexus.world.NexusLayout;
import static net.caravidro.wayaround.nexus.world.NexusLayout.Material.*;

public final class NexusLayoutTest {
    public static void main(String[] args) {
        int large=0,broken=0,floating=0,trees=0,empty=0,tvs=0,chairs=0;
        for(int cx=-10;cx<=10;cx++)for(int cz=-10;cz<=10;cz++) {
            var r=NexusLayout.room(42,cx,cz);if(r.area()>=1500)large++;if(r.breach()>=0)broken++;
            if(NexusLayout.floating(42,cx,cz)!=null)floating++;if(r.decor()==0)empty++;
            if(r.decor()==2)tvs++;if(r.decor()==1)chairs++;
            check(r.equals(NexusLayout.room(42,cx,cz)),"Order-independent rooms");
            for(int step=-40;step<40;step++) {
                check(NexusLayout.column(42,r.x()+step,r.z()).sample(80)==AIR,"Horizontal corridors connect adjacent room doors");
                check(NexusLayout.column(42,r.x(),r.z()+step).sample(80)==AIR,"Vertical corridors connect adjacent room doors");
                check(NexusLayout.column(42,r.x()+step,r.z()).sample(79)!=AIR,"No gaps in corridor floors");
            }
            if(NexusLayout.column(42,cx*80+4,cz*80+4).tree())trees++;
            check(NexusLayout.column(42,r.x(),r.z()).sample(0)==BEDROCK,"Safe bottom boundary");
            check(NexusLayout.column(42,r.x(),r.z()).sample(50)==AIR,"Rock slab is suspended above the lower void");
        }
        check(large>280,"Large rooms are favored");check(broken>250&&floating>60&&trees>10,"Broken walls, suspended rooms and lost trees exist");
        check(empty>40&&tvs>40&&chairs>40,"Decor varies between empty, chairs and televisions");
        for(int rx=-2;rx<=2;rx++)for(int rz=-2;rz<=2;rz++) {
            int controls=0;
            for(int x=0;x<5;x++)for(int z=0;z<5;z++)if(NexusLayout.controlCell(42,rx*5+x,rz*5+z))controls++;
            check(controls==1,"One discoverable cutoff room per region, including negative coordinates");
        }
        boolean differs=false;
        for(int i=0;i<20;i++)if(!NexusLayout.room(42,i,0).equals(NexusLayout.room(43,i,0)))differs=true;
        check(differs,"World seed changes the complex");
        System.out.println("Nexus layout: connected floors/doorways, negative coordinates, seed variance, large rooms, ruins, floating rooms, trees and control distribution passed");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
