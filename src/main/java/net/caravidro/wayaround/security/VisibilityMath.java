package net.caravidro.wayaround.security;

/** Pure voxel/range math. Rendering and server world queries deliberately remain separate. */
public final class VisibilityMath {
    public static final int SAMPLES=32, WIDTH=64, HEIGHT=36;
    public static final int UNKNOWN=-1, OPEN=0, WALL=1, ORE=2;
    public static final double RANGE=24;
    public interface Cells { int at(int x,int y,int z); }
    public record Ray(boolean known,boolean covered,boolean missingWall,boolean hiddenOre,int x,int y,int z) {}
    public static int pixel(long seed,int sample) {
        // Coprime permutation, restricted to the central world image (no hand/HUD).
        int index=Math.floorMod((int)(seed^(seed>>>32))+sample*37,48*24);
        return (6+index/48)*WIDTH+8+index%48;
    }
    public static boolean finiteDirection(double x,double y,double z) {
        double squared=x*x+y*y+z*z;return Double.isFinite(squared)&&Math.abs(squared-1)<.015;
    }
    public static Ray trace(double x,double y,double z,double dx,double dy,double dz,double rendered,int brightness,Cells cells) {
        if(!finiteDirection(dx,dy,dz)||!Double.isFinite(rendered)||rendered<0||rendered>65536)return new Ray(false,false,false,false,0,0,0);
        int cx=(int)Math.floor(x),cy=(int)Math.floor(y),cz=(int)Math.floor(z);
        int sx=dx>0?1:-1,sy=dy>0?1:-1,sz=dz>0?1:-1;
        double tx=boundary(x,cx,dx),ty=boundary(y,cy,dy),tz=boundary(z,cz,dz);
        double ix=dx==0?Double.POSITIVE_INFINITY:Math.abs(1/dx),iy=dy==0?Double.POSITIVE_INFINITY:Math.abs(1/dy),iz=dz==0?Double.POSITIVE_INFINITY:Math.abs(1/dz);
        double entry=0,wall=-1;boolean hidden=false;int ox=0,oy=0,oz=0;
        double limit=Math.min(RANGE,Math.max(3,rendered+.75));
        for(int step=0;step<64&&entry<limit;step++) {
            double exit=Math.min(limit,Math.min(tx,Math.min(ty,tz)));
            int material=cells.at(cx,cy,cz);
            if(material==UNKNOWN)return new Ray(false,false,false,false,0,0,0);
            // Ignore grazing voxels and walls too close to the eye: interpolation/bobbing can move those rays.
            if(material==WALL&&wall<0&&entry>=1.5&&exit-entry>=.65)wall=entry;
            if(material==ORE&&wall>=0&&entry-wall>=2 && brightness>=5
                    &&Math.abs(rendered-entry)<=.45&&exit-entry>=.35) { return new Ray(true,true,rendered-wall>=2.5,true,cx,cy,cz); }
            if(tx<=exit+1e-8){cx+=sx;tx+=ix;}
            if(ty<=exit+1e-8){cy+=sy;ty+=iy;}
            if(tz<=exit+1e-8){cz+=sz;tz+=iz;}
            entry=exit;
        }
        return new Ray(true,wall>=0,wall>=0&&rendered-wall>=2.5,hidden,ox,oy,oz);
    }
    private static double boundary(double position,int cell,double direction) {
        if(direction==0)return Double.POSITIVE_INFINITY;
        return ((direction>0?cell+1:cell)-position)/direction;
    }
    public static boolean strong(int covered,int missing,int hidden,int distinctOres) {
        return covered>=12&&missing>=8&&hidden>=3&&distinctOres>=2;
    }
    public static int delaySeconds(boolean underground,boolean home,boolean suspected,int jitter) {
        int base=suspected?8:underground?(home?35:10):90;
        return base+Math.floorMod(jitter,Math.max(4,base/2));
    }
    private VisibilityMath() {}
}
