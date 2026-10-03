import net.caravidro.wayaround.ecology.OceanPressure;
public final class OceanPressureTest {
    public static void main(String[] args) {
        int exposure=0;for(int i=0;i<3599;i++)exposure=OceanPressure.advance(exposure,110,true,false);
        if(exposure!=3599)throw new AssertionError("Three-minute exposure must precede implosion");
        if(OceanPressure.advance(exposure,110,true,false)!=3600)throw new AssertionError("Pressure failure time");
        if(OceanPressure.advance(exposure,55,true,false)!=3595)throw new AssertionError("Ascent relieves pressure");
        if(OceanPressure.advance(0,78,true,false)!=0 || OceanPressure.advance(0,78,true,true)!=1)throw new AssertionError("Distinct safe depths");
        if(OceanPressure.advance(0,200,false,true)!=0)throw new AssertionError("Dry vehicle never accumulates water pressure");
        System.out.println("Pressure warning, recovery and failure contracts passed");
    }
}
