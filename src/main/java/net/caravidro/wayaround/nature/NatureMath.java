package net.caravidro.wayaround.nature;

/** Registry-free fruit clock and bounded audio transformation. */
public final class NatureMath {
    private NatureMath(){}
    public static int fruitStage(long age){return age<0?0:age<4000?1:age<8000?2:3;}
    public static long fruitAge(long age,long elapsed){return Math.min(12000,age+Math.max(0,Math.min(24000,elapsed)));}
    public static byte[] mimic(byte[] input){
        int samples=input.length/2,n=(int)(samples/1.22);
        byte[] output=new byte[n*2];
        for(int i=0;i<n;i++){
            int at=Math.min(samples-1,Math.max(0,(int)(i*1.22+Math.sin(i/2800.0)*20)))*2;
            int sample=(short)((input[at]&255)|(input[at+1]<<8));
            sample=(int)(sample*(.66+.12*Math.sin(i/170.0)));sample=(sample>>4)<<4;
            output[i*2]=(byte)sample;output[i*2+1]=(byte)(sample>>8);
        }
        return output;
    }
}
