import net.caravidro.wayaround.security.*;
public final class VisibilityMathTest {
    private static int checks;
    private static void check(boolean pass,String reason){checks++;if(!pass)throw new AssertionError(reason);}
    public static void main(String[] args){
        VisibilityMath.Cells hidden=(x,y,z)->z>=3&&z<=5?VisibilityMath.WALL:z==10?VisibilityMath.ORE:VisibilityMath.OPEN;
        var ray=VisibilityMath.trace(.5,.5,.5,0,0,1,9.5,100,hidden);
        check(ray.known()&&ray.covered()&&ray.hiddenOre()&&ray.z()==10,"Visible depth matches ore behind three opaque blocks");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,2.5,100,hidden);
        check(ray.covered()&&!ray.missingWall()&&!ray.hiddenOre(),"Normal screenshot terminates at the wall");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,65536,100,hidden);
        check(ray.missingWall()&&!ray.hiddenOre(),"Missing geometry/sky alone never proves ore visibility");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,9.5,0,hidden);
        check(!ray.hiddenOre(),"Black screenshot cannot demonstrate seeing ore");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,9.5,100,(x,y,z)->z==10?VisibilityMath.ORE:VisibilityMath.OPEN);
        check(!ray.covered()&&!ray.hiddenOre(),"Normally exposed ore in a cave is legitimate");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,9.5,100,(x,y,z)->z==6?VisibilityMath.UNKNOWN:hidden.at(x,y,z));
        check(!ray.known()&&!ray.hiddenOre(),"Unloaded/custom/water cells invalidate a proof");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,1,Double.NaN,100,hidden);
        check(!ray.known(),"Non-finite depth rejected");
        ray=VisibilityMath.trace(.5,.5,.5,0,0,2,9.5,100,hidden);
        check(!ray.known(),"Non-unit ray rejected");
        VisibilityMath.Cells mirrored=(x,y,z)->z<=-3&&z>=-5?VisibilityMath.WALL:z==-10?VisibilityMath.ORE:VisibilityMath.OPEN;
        ray=VisibilityMath.trace(.5,.5,.5,0,0,-1,9.5,100,mirrored);
        check(ray.hiddenOre()&&ray.z()==-10,"Negative-coordinate voxel boundaries correct");
        final int[] reads={0};VisibilityMath.trace(.5,.5,.5,.577350269,.577350269,.577350269,65536,100,(x,y,z)->{reads[0]++;return 0;});
        check(reads[0]<=64,"World queries remain bounded");
        check(VisibilityMath.strong(12,8,3,2),"Multiple walls and distinct ores constitute strong geometry");
        check(!VisibilityMath.strong(32,32,0,0),"Empty or sky frame alone never convicts");
        check(!VisibilityMath.strong(32,32,10,1),"One duplicated vein/block is insufficient");
        check(VisibilityMath.delaySeconds(true,false,false,0)<VisibilityMath.delaySeconds(true,true,false,0),"Known underground base has quieter sampling");
        check(VisibilityMath.delaySeconds(false,true,true,0)<VisibilityMath.delaySeconds(true,false,false,0),"Suspicion overrides home schedule, without immunity");
        for(long seed=-1000;seed<1000;seed++){
            var pixels=new java.util.HashSet<Integer>();for(int i=0;i<32;i++){
                int p=VisibilityMath.pixel(seed,i);check(p>=0&&p<64*36,"Probe in screenshot bounds");check(p%64>=8&&p%64<56&&p/64>=6&&p/64<30,"Probe excludes margins");pixels.add(p);
            }check(pixels.size()==32,"Seed generates distinct sample pixels");
        }
        System.out.println("Visual geometry: "+checks+" assertions passed (walls, ores, clear cave, unknown, black, coordinates, budgets, base, sample bounds)");
    }
}
