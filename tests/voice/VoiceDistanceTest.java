import java.util.Arrays;
import net.caravidro.wayaround.voice.VoiceDistanceMath;

public final class VoiceDistanceTest {
    public static void main(String[] args) {
        check(VoiceDistanceMath.gain(0)==1 && VoiceDistanceMath.gain(9)==1,"Near voices stay clear");
        double previous=1;
        for(double distance=3;distance<=60;distance+=.1) {
            double gain=VoiceDistanceMath.gain(distance*distance);
            check(gain>=0 && gain<=previous+1e-12,"Continuous monotone falloff");
            previous=gain;
        }
        check(VoiceDistanceMath.gain(48*48)==0 && VoiceDistanceMath.gain(10000)==0,"Out of range is silent");
        check(VoiceDistanceMath.gain(Double.NaN)==0 && VoiceDistanceMath.gain(-1)==0,"Invalid distances cannot amplify");
        byte[] pcm={(byte)0xff,0x7f,0,(byte)0x80,0,0,0x34,0x12};
        byte[] original=pcm.clone(),quiet=VoiceDistanceMath.attenuate(pcm,.5);
        check(Arrays.equals(pcm,original),"Receiver falloff never changes capture, recordings or another listener's frame");
        check(sample(quiet,0)==16384 && sample(quiet,2)==-16384 && sample(quiet,4)==0,"Signed LE samples retain polarity");
        check(Arrays.equals(VoiceDistanceMath.attenuate(pcm,0),new byte[pcm.length]),"Silent samples have no residual sound");
        check(Arrays.equals(VoiceDistanceMath.attenuate(pcm,2),original),"No clipping or amplification");
        check(Arrays.equals(VoiceDistanceMath.attenuate(pcm,Double.NaN),new byte[pcm.length]),"Invalid gain is silent");
        check(VoiceDistanceMath.gain(20*20)>VoiceDistanceMath.gain(35*35),"Two listeners receive different volumes");
        System.out.println("Voice proximity: smooth range, PCM polarity and immutable capture passed");
    }
    private static int sample(byte[] pcm,int i) { return (short)((pcm[i]&255)|(pcm[i+1]<<8)); }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
