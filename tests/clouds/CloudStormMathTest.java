import net.caravidro.wayaround.worldgen.weather.local.CloudStormMath;

public final class CloudStormMathTest {
    public static void main(String[] args){
        check(CloudStormMath.solarOffset(100,0,1)==0,"noon projection below cloud");
        check(Math.abs(CloudStormMath.solarOffset(100,.6,.8)-75)<.001,"angled solar projection");
        check(CloudStormMath.solarOffset(100,-.6,.8)<0,"opposite sun displacement");
        check(CloudStormMath.shadowAlpha(.6,1,0,40)>90,"visible shadow at viewer, no camera exclusion");
        check(CloudStormMath.shadowAlpha(.6,1,40,40)==0,"soft bounded shadow edge");
        check(CloudStormMath.shadowAlpha(1,.1,0,40)==0,"no daytime shadow below horizon");
        int wet=0,dry=0;
        for(int i=0;i<10000;i++){
            double roll=i/10000.0;
            if(roll<CloudStormMath.cover(.96))wet++;
            if(roll<CloudStormMath.cover(.10))dry++;
        }
        check(wet>dry*2,"wet regions carry more clouds");
        check(CloudStormMath.storm(.9F,.1)<CloudStormMath.storm(.9F,.96),"desert storm suppression");
        check(CloudStormMath.soundDelay(343)==20,"one second sound travel at 343 blocks");
        check(CloudStormMath.soundDelay(0)==0,"no minimum artificial delay");
        check(CloudStormMath.soundDelay(686)==40,"distance scales arrival");
        check(CloudStormMath.flash(0)>CloudStormMath.flash(3),"flash decay");
        check(CloudStormMath.flash(6)>CloudStormMath.flash(4),"internal secondary flicker");
        check(CloudStormMath.flash(20)==0,"flash expires");
        check(CloudStormMath.gustWeight(80,0,1,0,160)>CloudStormMath.gustWeight(-80,0,1,0,160),"downwind arrival strongest");
        check(CloudStormMath.gustWeight(1000,0,1,0,160)==0,"distant wind inaudible");
        for(int i=-100;i<=200;i++){
            double h=i/100.0;
            check(CloudStormMath.cover(h)>=.22&&CloudStormMath.cover(h)<=.951,"cover bounds");
            check(Float.isFinite(CloudStormMath.storm(.7F,h)),"finite storm strength");
        }
        System.out.println("CloudStormMath passed: wet="+wet+", desert="+dry+", sound and gust propagation valid");
    }
    private static void check(boolean test,String name){if(!test)throw new AssertionError(name);}
}
