package net.caravidro.wayaround.worldgen.weather.fire;
import java.util.*;
/** Bounded spatial components: distance changes detail, never connects separate fires. */
public final class FireClusterLod {
 public record Point(int x,int y,int z){}
 public record Patch(int x,int y,int z,int width,int depth,float height){}
 private record Cell(int x,int y,int z){}
 private static class Box {int x,y,z,maxX,maxZ,n;Box(Point p){x=p.x;y=p.y;z=p.z;maxX=x+1;maxZ=z+1;}void add(Point p){x=Math.min(x,p.x);y=Math.min(y,p.y);z=Math.min(z,p.z);maxX=Math.max(maxX,p.x+1);maxZ=Math.max(maxZ,p.z+1);n++;}}
 public static List<Patch> build(List<Point> source,double px,double py,double pz){
  int n=Math.min(1024,source.size());var points=source.subList(0,n);int[] root=new int[n];var buckets=new HashMap<Cell,List<Integer>>();
  for(int i=0;i<n;i++){root[i]=i;var p=points.get(i);var c=new Cell(Math.floorDiv(p.x,4),Math.floorDiv(p.y,4),Math.floorDiv(p.z,4));
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)for(int k:buckets.getOrDefault(new Cell(c.x+x,c.y+y,c.z+z),List.of())){
    var q=points.get(k);if(Math.abs(p.x-q.x)<=3&&Math.abs(p.y-q.y)<=3&&Math.abs(p.z-q.z)<=3)root[find(root,i)]=find(root,k);
   }buckets.computeIfAbsent(c,k->new ArrayList<>()).add(i);
  }
  var components=new HashMap<Integer,List<Point>>();for(int i=0;i<n;i++)components.computeIfAbsent(find(root,i),k->new ArrayList<>()).add(points.get(i));
  var output=new ArrayList<Patch>();
  for(var component:components.values()){
   double nearest=Double.POSITIVE_INFINITY;for(var p:component)nearest=Math.min(nearest,distance(p.x,p.y,p.z,px,py,pz));
   int tile=nearest<96*96?8:nearest<160*160?16:256;
   var tiles=new HashMap<Cell,Box>();for(var p:component){var cell=new Cell(Math.floorDiv(p.x,tile),Math.floorDiv(p.y,8),Math.floorDiv(p.z,tile));
    // Far away: one silhouette per connected cluster, unless it exceeds the bounded 256-block footprint.
    if(tile==256){var first=component.get(0);cell=new Cell(Math.floorDiv(p.x-first.x+128,256),0,Math.floorDiv(p.z-first.z+128,256));}
    tiles.computeIfAbsent(cell,k->new Box(p)).add(p);
   }
   for(var b:tiles.values()){int w=b.maxX-b.x,d=b.maxZ-b.z;if(b.n<2)continue;float h=height(w,d,b.n);output.add(new Patch(b.x,b.y,b.z,w,d,h));}
  }
  output.sort(Comparator.comparingDouble(p->distance(p.x+p.width*.5,p.y,p.z+p.depth*.5,px,py,pz)));
  return List.copyOf(output.subList(0,Math.min(16,output.size())));
 }
 public static float height(int width,int depth,int count){return Math.min(32,1.4F+(float)Math.sqrt(Math.max(width,depth))*1.05F+(float)Math.sqrt(count)*.08F);}
 private static double distance(double x,double y,double z,double a,double b,double c){return (x-a)*(x-a)+(y-b)*(y-b)+(z-c)*(z-c);}
 private static int find(int[] root,int i){while(root[i]!=i){root[i]=root[root[i]];i=root[i];}return i;}
}
