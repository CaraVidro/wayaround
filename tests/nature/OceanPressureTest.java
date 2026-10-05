import net.caravidro.wayaround.ecology.OceanPressure;
import net.caravidro.wayaround.pressure.PressureMath;

public final class OceanPressureTest {
    public static void main(String[] args) {
        int exposure=0;
        for(int i=0;i<3599;i++) {
            exposure=OceanPressure.advance(exposure,110,true,false);
        }

        if(exposure!=3599) {
            throw new AssertionError("Three-minute exposure must precede implosion");
        }

        if(OceanPressure.advance(exposure,110,true,false)!=3600) {
            throw new AssertionError("Pressure failure time");
        }

        if(OceanPressure.advance(exposure,55,true,false)!=3595) {
            throw new AssertionError("Ascent relieves pressure");
        }

        if(OceanPressure.advance(0,78,true,false)!=0
                || OceanPressure.advance(0,78,true,true)!=1) {
            throw new AssertionError("Distinct safe vessel pressure envelopes");
        }

        if(OceanPressure.advance(0,200,false,true)!=0) {
            throw new AssertionError("Dry vehicle never accumulates water pressure");
        }

        double hundredMetresKPa =
                PressureMath.hydrostaticGaugeKPa(
                        OceanPressure.SEAWATER_DENSITY_KG_M3,
                        100.0
                );

        if(hundredMetresKPa < 1000.0
                || hundredMetresKPa > 1010.0) {
            throw new AssertionError(
                    "100 m seawater pressure should be about 1005 kPa gauge, got "
                            + hundredMetresKPa
            );
        }

        double outside =
                PressureMath.STANDARD_ATMOSPHERE_KPA
                        + hundredMetresKPa;

        if(OceanPressure.advancePressure(
                0,
                outside,
                PressureMath.STANDARD_ATMOSPHERE_KPA,
                true,
                false
        ) != 1) {
            throw new AssertionError(
                    "Runtime pressure API must accumulate exposure from delta-P"
            );
        }

        System.out.println(
                "Pressure warning, recovery, hydrostatics and failure contracts passed"
        );
    }
}
