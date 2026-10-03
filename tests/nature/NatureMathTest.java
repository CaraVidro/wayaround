import net.caravidro.wayaround.nature.NatureMath;
import java.util.Arrays;
public final class NatureMathTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        long age=-4000;
        age=NatureMath.fruitAge(age,3999);check(NatureMath.fruitStage(age)==0,"Cooldown must complete before visible regrowth");
        age=NatureMath.fruitAge(age,1);check(NatureMath.fruitStage(age)==1,"Green regrowth");
        for(int i=0;i<100;i++)age=NatureMath.fruitAge(age,80);
        check(NatureMath.fruitStage(age)==3,"Staggered pulses eventually ripen fruit");
        check(NatureMath.fruitAge(age,-500)==age,"Reversed clock cannot unripe fruit");
        check(NatureMath.fruitAge(0,Long.MAX_VALUE)==12000,"Unloaded world catch-up is bounded");
        check(NatureMath.mimic(new byte[0]).length==0,"Empty audio safe");
        byte[] input=new byte[96000];for(int i=0;i<input.length;i+=2){int sample=(int)(30000*Math.sin(i*.07));input[i]=(byte)sample;input[i+1]=(byte)(sample>>8);}
        byte[] original=input.clone(),echo=NatureMath.mimic(input);
        check(Arrays.equals(input,original),"Never mutate player voice frames");
        check(echo.length>70000&&echo.length<96000&&(echo.length&1)==0,"Pitch shift keeps complete PCM samples");
        for(int i=0;i<echo.length;i+=2){int sample=(short)((echo[i]&255)|(echo[i+1]<<8));check(Math.abs(sample)<=24000,"Distortion must not clip PCM");}
        System.out.println("Fruit lifecycle and bounded parrot PCM checks passed");
    }
}
