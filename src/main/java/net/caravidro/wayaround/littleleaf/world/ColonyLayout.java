package net.caravidro.wayaround.littleleaf.world;

/** Stable cell seed, connected functional rooms and extra galleries as the physical nest grows. */
public final class ColonyLayout {
    public static final int CELL=128,FLOOR=32,HEIGHT=128;
    public enum Material {AIR,BEDROCK,SOIL,ROOT,LIGHT,FUNGUS,STEM,CAP,EXIT,LEAF}
    public static long seed(int x,int z){long n=Math.floorDiv(x,CELL)*341873128712L+Math.floorDiv(z,CELL)*132897987541L+719;return mix(n);}
    private static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    public static Material sample(int x,int y,int z){return sample(x,y,z,0);}
    public static Material sample(int x,int y,int z,int stage){
        if(y<0||y>=HEIGHT)return Material.AIR;if(y==0||y==HEIGHT-1)return Material.BEDROCK;
        long seed=seed(x,z);stage=Math.max(0,Math.min(4,stage));x=Math.floorMod(x,CELL);z=Math.floorMod(z,CELL);
        if(y<FLOOR||y>=FLOOR+28)return Material.SOIL;
        int variant=(int)(seed&1);
        boolean room=room(x,y,z,24,64,7+stage,7+stage,7+stage)||room(x,y,z,52,64,14+stage*2,14+stage*2,18+stage*2)||room(x,y,z,88,64,9+stage*2,9+stage*2,10+stage*2)
            ||room(x,y,z,52,32,5+stage*2+variant,5+stage*2,7+stage)||room(x,y,z,52,96,5+stage*2,6+stage*2+variant,8+stage);
        boolean exitTunnel=x>=4&&x<=24&&Math.abs(z-64)<=2&&y<FLOOR+4;
        boolean tunnel=y<FLOOR+5&&((x>=24&&x<=88&&Math.abs(z-64)<=3)||(z>=32&&z<=96&&Math.abs(x-52)<=3));
        for(int i=0;i<2+stage*2;i++){
            long key=mix(seed+i*971);int cx=28+(int)Math.floorMod(key,72),cz=20+(int)Math.floorMod(key>>>12,88),rx=4+stage+(int)(key>>>24&3),rz=5+stage+(int)(key>>>32&3);
            room|=room(x,y,z,cx,cz,rx,rz,7+stage+(int)(key>>>40&3));
            tunnel|=y<FLOOR+5&&((Math.abs(x-cx)<=2&&z>=Math.min(cz,64)&&z<=Math.max(cz,64))||(Math.abs(z-64)<=2&&x>=Math.min(cx,88)&&x<=Math.max(cx,88)));
        }
        if(!room&&!tunnel&&!exitTunnel)return ((x*17+z*31+y*7)%53==0)?Material.ROOT:Material.SOIL;
        if(x==6&&Math.abs(z-64)<=2&&y<FLOOR+3)return Material.EXIT;
        if(y==FLOOR&&x>=50&&x<=54&&z>=59&&z<=61)return Material.LEAF;
        int dx=x-52,dz=z-64,r=dx*dx+dz*dz;
        if(y<=FLOOR+10&&Math.abs(dx)<=2&&Math.abs(dz)<=2)return (y<FLOOR+3&&Math.abs(dx)==2)?Material.FUNGUS:Material.STEM;
        if(y>=FLOOR+10&&y<=FLOOR+14&&r<=110-(y-FLOOR-10)*15)return Material.CAP;
        if(y==FLOOR&&((x==52&&z==28)||(x==52&&z==100)||(x==88&&z==70)||(x==40&&z==62)))return Material.LIGHT;
        return Material.AIR;
    }
    private static boolean room(int x,int y,int z,int cx,int cz,int rx,int rz,int h){return y<FLOOR+h&&((x-cx)*(x-cx)/(double)(rx*rx)+(z-cz)*(z-cz)/(double)(rz*rz)<1);}
    private ColonyLayout(){}
}
