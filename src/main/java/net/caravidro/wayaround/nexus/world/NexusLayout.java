package net.caravidro.wayaround.nexus.world;

/** Pure, seed-based geometry. No world reads, mutable caches or cross-chunk writes. */
public final class NexusLayout {
    public static final int CELL=80, FLOOR=80, HEIGHT=256;
    public enum Material { AIR, BEDROCK, ROCK, ANDESITE, CONCRETE, PALE_CONCRETE, RED_CONCRETE, LIGHT, CHAIR, TV, CONTROL, LOG, LEAVES }
    public record Room(int x,int z,int halfX,int halfZ,int floor,int height,int breach,int decor,boolean control,long salt) {
        public int area(){return (halfX*2-1)*(halfZ*2-1);}
        public Material sample(int px,int y,int pz) {
            int dx=px-x,dz=pz-z,ax=Math.abs(dx),az=Math.abs(dz);
            if(ax>halfX||az>halfZ||y<floor-3||y>floor+height)return Material.AIR;
            if(y<floor-1)return Material.ROCK;
            if(y==floor-1)return Math.floorMod(px+pz,9)==0?Material.PALE_CONCRETE:Material.ANDESITE;
            boolean wall=ax==halfX||az==halfZ,roof=y==floor+height;
            int lateral=breach<2?dx:dz,reach=(breach<2?halfX:halfZ)/2;
            boolean broken=breach>=0 && (breach==0?dz<=-halfZ+5:breach==1?dz>=halfZ-5:breach==2?dx<=-halfX+5:dx>=halfX-5)
                    && Math.abs(lateral)<reach+(int)Math.floorMod(hash(salt,lateral,y/3),4)-1 && y<floor+height-1;
            if(broken && (wall||roof))return Material.AIR;
            // Broken roof edges expose the sky; intact beams retain a jagged outline.
            if(roof && brokenSide(dx,dz) && Math.abs(lateral)<reach-2)return Material.AIR;
            if(wall) {
                if(y<floor+5 && (ax==halfX&&az<=2||az==halfZ&&ax<=2))return Material.AIR;
                if(y==floor+2)return Material.RED_CONCRETE;
                return Math.floorMod(hash(salt,px,pz),13)==0?Material.PALE_CONCRETE:Material.CONCRETE;
            }
            if(roof)return Material.CONCRETE;
            if(y==floor+height-1 && (dx==0&&Math.abs(dz)%11==0))return Material.LIGHT;
            if(ax==halfX-2 && az==halfZ-2)return Material.CONCRETE;
            if(y==floor) {
                if(control && dx==halfX-3&&dz==-4)return Material.CONTROL;
                if((decor==1||decor==2) && (dx==-7||dx==7) && (dz==5||dz==9))return Material.CHAIR;
                if(decor==2&&dx==6&&dz==-halfZ+3)return Material.TV;
                if(decor==3 && ax>2&&az>2 && brokenSide(dx,dz) && Math.floorMod(hash(salt,px,pz),9)==0)return Material.ANDESITE;
            }
            return Material.AIR;
        }
        private boolean brokenSide(int dx,int dz){return breach>=0 && (breach==0?dz<=-halfZ+5:breach==1?dz>=halfZ-5:breach==2?dx<=-halfX+5:dx>=halfX-5);}
    }
    public static Room room(long seed,int cellX,int cellZ) {
        long s=hash(seed,cellX,cellZ);boolean control=controlCell(seed,cellX,cellZ);
        int hx=control?32:half(s),hz=control?30:half(s>>>8);
        return new Room(cellX*CELL+40,cellZ*CELL+40,hx,hz,FLOOR,8+(int)Math.floorMod(s>>>16,12),
                Math.floorMod(s>>>24,5)==4?-1:(int)Math.floorMod(s>>>24,4),(int)Math.floorMod(s>>>32,5),control,s);
    }
    private static int half(long s){return switch((int)Math.floorMod(s,5)){case 0->14;case 1->22;case 2->27;default->33;};}
    public static boolean controlCell(long seed,int x,int z) {
        int rx=Math.floorDiv(x,5),rz=Math.floorDiv(z,5);long h=hash(seed^0x537574646f776eL,rx,rz);
        return Math.floorMod(x,5)==Math.floorMod(h,5)&&Math.floorMod(z,5)==Math.floorMod(h>>>8,5);
    }
    public static Room floating(long seed,int cellX,int cellZ) {
        long s=hash(seed^0x466c6f6174L,cellX,cellZ);
        if(Math.floorMod(s,4)!=0)return null;
        return new Room(cellX*CELL+25+(int)Math.floorMod(s>>>8,30),cellZ*CELL+25+(int)Math.floorMod(s>>>16,30),
                10+(int)Math.floorMod(s>>>24,10),10+(int)Math.floorMod(s>>>32,10),124+(int)Math.floorMod(s>>>40,44),
                7+(int)Math.floorMod(s>>>48,9),(int)Math.floorMod(s>>>56,4),(int)Math.floorMod(s>>>28,4),false,s);
    }
    public record Column(long seed,int x,int z,Room room,Room suspended,boolean tree,int treeX,int treeZ) {
        public Material sample(int y) {
            if(y<0||y>=HEIGHT)return Material.AIR;
            if(y==0)return Material.BEDROCK;
            if(y<56)return Material.AIR;
            if(y<FLOOR-3)return Material.ROCK;
            var material=room.sample(x,y,z);if(material!=Material.AIR)return material;
            if(y<FLOOR-1)return Material.ROCK;
            if(y==FLOOR-1)return Math.floorMod(hash(seed,x,z),7)==0?Material.ANDESITE:Material.ROCK;
            // The hollow corridor grid joins all four open doorways, without doors.
            int lx=Math.floorMod(x,CELL),lz=Math.floorMod(z,CELL);
            boolean passage=Math.abs(lx-40)<=2||Math.abs(lz-40)<=2;
            boolean edge=(Math.abs(lx-40)==3&&Math.abs(lz-40)>3)||(Math.abs(lz-40)==3&&Math.abs(lx-40)>3);
            boolean insideRoom=Math.abs(x-room.x)<room.halfX&&Math.abs(z-room.z)<room.halfZ;
            boolean beyondRoom=Math.abs(x-room.x)>room.halfX||Math.abs(z-room.z)>room.halfZ;
            if(!insideRoom && beyondRoom && y<=FLOOR+6) {
                if(edge)return y==FLOOR+2?Material.RED_CONCRETE:Material.CONCRETE;
                if(passage)return y==FLOOR+6?Material.CONCRETE:Material.AIR;
            }
            if(suspended!=null){material=suspended.sample(x,y,z);if(material!=Material.AIR)return material;}
            if(tree) {
                int dx=Math.abs(x-treeX),dz=Math.abs(z-treeZ);
                if(dx==0&&dz==0&&y<FLOOR+6)return Material.LOG;
                if(Math.floorMod(room.salt,3)!=0 && y>=FLOOR+4&&y<=FLOOR+8&&dx+dz<=4&&dx<=2&&dz<=2)return Material.LEAVES;
            }
            return Material.AIR;
        }
        public int top(){return suspended==null?FLOOR+room.height:Math.max(FLOOR+room.height,suspended.floor+suspended.height);}
    }
    public static Column column(long seed,int x,int z) {
        int cx=Math.floorDiv(x,CELL),cz=Math.floorDiv(z,CELL);Room r=room(seed,cx,cz);
        // Tree crowns never cross cell boundaries and never intersect corridors.
        return new Column(seed,x,z,r,floating(seed,cx,cz),Math.floorMod(r.salt>>>7,11)==0,cx*CELL+4,cz*CELL+4);
    }
    public static long hash(long seed,int x,int z) {
        long h=seed^(x*0x632BE59BD9B4E019L)^(z*0x9E3779B97F4A7C15L);
        h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);
    }
    private NexusLayout() {}
}
