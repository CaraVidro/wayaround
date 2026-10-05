package net.caravidro.wayaround.worldgen.planet;

/** Pure geography/topology, shared by generation and server travel. Y never wraps. */
public final class PlanetMath {
    public static final int SIZE=65536, HALF=SIZE/2;
    public static int wrap(int coordinate) { return Math.floorMod(coordinate+HALF,SIZE)-HALF; }
    public static double wrap(double coordinate) {
        if(!Double.isFinite(coordinate))return 0;
        return coordinate-Math.floor((coordinate+HALF)/SIZE)*SIZE;
    }
    public static double delta(double from,double to) { return wrap(to-from); }
    public static boolean outside(double x,double z) { return x<-HALF||x>=HALF||z<-HALF||z>=HALF; }
    public static double smooth(double x) { x=Math.max(0,Math.min(1,x));return x*x*(3-2*x); }
    public static double edgeFade(int x,int z) { return smooth((HALF-Math.max(Math.abs((long)wrap(x)),Math.abs((long)wrap(z))))/2048.0); }
    public static double latitude(int z) { return Math.sin(wrap(z)*Math.PI*2/SIZE); }
    public static double polarRadius(int x,int z) {
        double dx=delta(0,x)/14500, dz=delta(22528,z)/6200;
        double coast=1+.055*Math.sin(wrap(x)*Math.PI*2*5/SIZE)+.025*Math.cos(wrap(z)*Math.PI*2*9/SIZE);
        return Math.sqrt(dx*dx+dz*dz)/coast;
    }
    public static double antarctica(int x,int z) { return smooth((1-polarRadius(x,z))/.24); }
    public static double southernOcean(int x,int z) { return smooth((2.1-polarRadius(x,z))/.55); }
    /** Sea shelf -> uplands -> long massifs. Climate fields are seed-dependent Minecraft noises. */
    public static double height(double continent,double erosion,double ridge) {
        if(continent<-.19) return 61-114*smooth((-.19-continent)/.47);
        if(continent<-.11) return 61+9*smooth((continent+.19)/.08);
        double land=smooth((continent+.11)/.40);
        double mountain=land*smooth((.18-erosion)/.66);
        double spine=smooth(1-Math.abs(Math.abs(ridge)-.62)/.68);
        double height=70+land*31+land*(1+ridge)*9+mountain*(104+80*spine);
        // Continuous low valleys, rather than isolated pools atop high mountains.
        double valley=smooth(1-Math.abs(ridge)/.055)*land;
        return Math.max(-54,Math.min(286,height+(60-height)*valley));
    }
    private PlanetMath() {}
}
