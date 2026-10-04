package net.caravidro.wayaround.littleleaf.world;

/** Chunk-local miniature world seen at player scale. Solid walls isolate each colony. */
public final class ColonyLayout {
    public static final int CELL=128,FLOOR=32,HEIGHT=128;
    public enum Material {AIR,BEDROCK,SOIL,ROOT,LIGHT,FUNGUS,STEM,CAP,EXIT}
    public static Material sample(int x,int y,int z){
        if(y<0||y>=HEIGHT)return Material.AIR;if(y==0||y==HEIGHT-1)return Material.BEDROCK;
        x=Math.floorMod(x,CELL);z=Math.floorMod(z,CELL);
        boolean room=room(x,y,z,24,64,10,10,7)||room(x,y,z,52,64,17,18,20)||room(x,y,z,88,64,14,15,12)
            ||room(x,y,z,52,32,10,11,7)||room(x,y,z,52,96,12,11,9);
        boolean exitTunnel=x>=4&&x<=24&&Math.abs(z-64)<=2&&y>=FLOOR&&y<FLOOR+4;
        boolean tunnel=y>=FLOOR&&y<FLOOR+5&&((x>=24&&x<=88&&Math.abs(z-64)<=3)||(z>=32&&z<=96&&Math.abs(x-52)<=3));
        if(!room&&!tunnel&&!exitTunnel)return y<FLOOR-4||y>FLOOR+24?Material.SOIL:((x*17+z*31+y*7)%53==0?Material.ROOT:Material.SOIL);
        if(x==6&&Math.abs(z-64)<=2&&y>=FLOOR&&y<FLOOR+3)return Material.EXIT;
        int dx=x-52,dz=z-64,r=dx*dx+dz*dz;
        if(y>=FLOOR&&y<=FLOOR+10&&Math.abs(dx)<=2&&Math.abs(dz)<=2)return (y<FLOOR+3&&Math.abs(dx)==2)?Material.FUNGUS:Material.STEM;
        if(y>=FLOOR+10&&y<=FLOOR+14&&r<=110-(y-FLOOR-10)*15)return Material.CAP;
        if(y==FLOOR&&((x==38&&z==54)||(x==67&&z==75)||(x==88&&z==76)))return Material.LIGHT;
        return Material.AIR;
    }
    private static boolean room(int x,int y,int z,int cx,int cz,int rx,int rz,int h){return y>=FLOOR&&y<FLOOR+h&&((x-cx)*(x-cx)/(double)(rx*rx)+(z-cz)*(z-cz)/(double)(rz*rz)<1);}
    private ColonyLayout(){}
}
