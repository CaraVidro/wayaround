package net.caravidro.wayaround.littleleaf;

/** Pure, bounded lifecycle rules shared by simulation and verification. */
public final class ColonyRules {
    public static final int[] THRESHOLDS={0,24,96,256,640};
    public static final double TINY=.0625,GIANT=3,PLAYER_TINY=.0625;
    public static int stage(long work,boolean giant){int s=0;for(int i=1;i<THRESHOLDS.length;i++)if(work>=THRESHOLDS[i])s=i;return giant?s:Math.min(1,s);}
    public static int population(int stage,boolean inside){return inside?12:6+Math.min(4,Math.max(0,stage))*2;}
    public static long advanceWork(long work,long elapsed,boolean habitat){return Math.min(4096,Math.max(0,work)+(habitat?Math.min(Math.max(0,elapsed),24000L*30)/1200:0));}
    public static int radius(int stage,boolean termite){return (termite?4:2)+Math.max(0,Math.min(4,stage))*2;}
    public static int height(int stage,boolean termite){return (termite?5:2)+Math.max(0,Math.min(4,stage))*2;}
    private ColonyRules(){}
}
