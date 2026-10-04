import java.util.*;
import net.caravidro.wayaround.worldgen.weather.fire.FireClusterLod;
public class FireClusterLodTest {
 public static void main(String[] args){var points=new ArrayList<FireClusterLod.Point>();for(int x=0;x<60;x++)for(int z=0;z<8;z++)points.add(new FireClusterLod.Point(x,64,z));
  var mid=FireClusterLod.build(points,0,64,-80);var far=FireClusterLod.build(points,0,64,-240);if(mid.size()<=far.size()||far.size()!=1)throw new AssertionError("Distance should merge connected flames");
  points.add(new FireClusterLod.Point(80,64,0));points.add(new FireClusterLod.Point(81,64,0));if(FireClusterLod.build(points,0,64,-240).size()!=2)throw new AssertionError("Separate fires must remain separate");
  if(FireClusterLod.height(100,20,200)<=FireClusterLod.height(10,10,100))throw new AssertionError("Large footprint also grows upwards");System.out.println("Wildfire LOD contracts passed");
 }
}
