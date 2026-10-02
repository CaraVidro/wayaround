package net.caravidro.wayaround.worldgen.weather.local;

/** Registry-free rules for cloud cover, propagation and flash envelopes. */
public final class CloudStormMath {
    private CloudStormMath() {}
    public static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    public static double cover(double humidity){return .22+.73*clamp(humidity,0,1);}
    public static double size(double humidity){return .72+.32*clamp(humidity,0,1);}
    public static float storm(float base,double humidity){return (float)clamp(base*(.55+.55*humidity)+.12*humidity-.12,0,1);}
    public static int soundDelay(double distance){return (int)Math.ceil(clamp(distance,0,2000)/343.0*20);}
    public static float flash(double age){
        if(age<0||age>18)return 0;
        return (float)clamp(Math.exp(-age*.35)+.65*Math.exp(-Math.pow((age-6)/1.7,2)),0,1);
    }
    public static double gustWeight(double dx,double dz,double wx,double wz,double radius){
        double distance=Math.sqrt(dx*dx+dz*dz);
        double near=clamp(1-distance/(radius+100),0,1);
        double downwind=distance<1?1:clamp((dx*wx+dz*wz)/distance,0,1);
        return near*(.35+.65*downwind);
    }
}
