package net.caravidro.wayaround.thermal;

public final class TemperatureCurveTest {
    public static void main(String[] args) {
        double previous=0;
        for(int temperature=20;temperature<=3200;temperature++) {
            double chance=TemperatureCurve.chance(temperature,1900,360,.16);
            require(chance>=previous && chance>=0 && chance<=.16,"Monotonic bounded melt probability");
            if(temperature<=1900)require(chance==0,"No melting below threshold");
            previous=chance;
        }
        require(TemperatureCurve.chance(3200,1900,360,.16)<1,"Peak heat is not guaranteed lava");
        require(TemperatureCurve.chance(3200,280,500,.5)>TemperatureCurve.chance(3200,1900,360,.16),"Wood ignites more readily than stone melts");
        require(TemperatureCurve.chance(Double.NaN,280,500,.5)==0,"Reject non-finite heat");
        double initial=3000,after=TemperatureCurve.cool(initial,240);
        require(after>20 && after<initial,"Cooling approaches ambient");
        require(Math.abs(TemperatureCurve.cool(after,240)-TemperatureCurve.cool(initial,480))<1e-8,"Cooling independent of update interval");
        require(TemperatureCurve.cool(20,99999)==20,"Ambient is stable");

        double coldInitial=-80;
        double coldAfter=TemperatureCurve.relax(coldInitial,-30,240);
        require(coldAfter>-80 && coldAfter<-30,"Cold anomalies relax upward toward cold ambient");
        require(Math.abs(
                TemperatureCurve.relax(coldAfter,-30,240)
                        - TemperatureCurve.relax(coldInitial,-30,480)
        )<1e-8,"Signed thermal relaxation is update-interval independent");
        require(TemperatureCurve.clamp(-999)==TemperatureCurve.MIN,"Extreme cold is bounded");
        require(TemperatureCurve.clamp(99999)==TemperatureCurve.MAX,"Extreme heat is bounded");

        System.out.println("Thermal probability and signed relaxation regression tests passed");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
