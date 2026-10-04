package net.caravidro.wayaround.littleleaf;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
/** Incremental surface A*: floor and wall waypoints, bounded across the entire level. */
public final class ColonySurfaceRoute {
 private record Node(BlockPos pos,double cost,double score){}
 private final PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::score));
 private final Map<BlockPos,Double> cost=new HashMap<>();private final Map<BlockPos,BlockPos> parent=new HashMap<>();
 private List<BlockPos> path=List.of();private BlockPos goal;private int at,visited;private boolean failed;
 public void reset(){open.clear();cost.clear();parent.clear();path=List.of();goal=null;at=visited=0;failed=false;}
 public boolean failed(){return failed;}
 public void follow(ColonyInsectEntity e,BlockPos target){
  if(!(e.level() instanceof ServerLevel l))return;
  if(!target.equals(goal)){reset();goal=target.immutable();var start=e.blockPosition();cost.put(start,0.0);open.add(new Node(start,0,heuristic(start)));}
  for(int k=0;path.isEmpty()&&!open.isEmpty()&&k<12&&ColonyBudget.search(l);k++){
   var n=open.remove();if(n.cost>cost.getOrDefault(n.pos,Double.MAX_VALUE))continue;
   if(++visited>384){failed=true;open.clear();break;}
   var box=e.getBoundingBox().move(n.pos.getX()+.5-e.getX(),n.pos.getY()-e.getY(),n.pos.getZ()+.5-e.getZ());
   if(box.inflate(e.enlarged()?.75:.62).intersects(new AABB(goal))){var route=new ArrayList<BlockPos>();for(var p=n.pos;p!=null;p=parent.get(p))route.add(p);Collections.reverse(route);path=route;at=0;break;}
   for(var d:Direction.values()){
    var p=n.pos.relative(d);if(Math.abs(p.getX()-goal.getX())>32||Math.abs(p.getZ()-goal.getZ())>32||Math.abs(p.getY()-goal.getY())>40||!valid(l,e,p))continue;
    double next=n.cost+(d.getAxis()==Direction.Axis.Y?1.2:1);if(next>=cost.getOrDefault(p,Double.MAX_VALUE))continue;
    cost.put(p,next);parent.put(p,n.pos);open.add(new Node(p,next,next+heuristic(p)));
   }
  }
  if(path.isEmpty()){if(open.isEmpty())failed=true;return;}
  while(at<path.size()){
   var p=path.get(at);Vec3 point=Vec3.atBottomCenterOf(p);double tolerance=e.enlarged()?.45:.14;
   if(e.position().distanceToSqr(point)<tolerance*tolerance){at++;continue;}
   var delta=point.subtract(e.position());boolean up=Math.abs(delta.y)>.15;
   double speed=e.enlarged()?.13:.045;Vec3 horizontal=new Vec3(delta.x,0,delta.z);if(horizontal.length()>speed)horizontal=horizontal.normalize().scale(speed);
   double dy=up?Math.clamp(delta.y,-.12,.12):e.getDeltaMovement().y;
   e.routeClimbing(up);e.getNavigation().stop();e.setDeltaMovement(horizontal.x,dy,horizontal.z);e.getLookControl().setLookAt(point.x,point.y+e.getEyeHeight(),point.z,30,30);return;
  }
  e.routeClimbing(false);e.setDeltaMovement(0,e.getDeltaMovement().y,0);
 }
 private double heuristic(BlockPos p){return Math.abs(p.getX()-goal.getX())+Math.abs(p.getY()-goal.getY())+Math.abs(p.getZ()-goal.getZ());}
 private static boolean wall(ServerLevel l,BlockPos p){for(var d:Direction.Plane.HORIZONTAL){var q=p.relative(d);if(ColonyCoreBlockEntity.loaded(l,q)&&!l.getBlockState(q).getCollisionShape(l,q).isEmpty())return true;}return false;}
 private static boolean valid(ServerLevel l,ColonyInsectEntity e,BlockPos p){
  if(!ColonyCoreBlockEntity.loaded(l,p)||!ColonyCoreBlockEntity.loaded(l,p.above(2))||!l.getFluidState(p).isEmpty())return false;
  var b=e.getBoundingBox().move(p.getX()+.5-e.getX(),p.getY()-e.getY(),p.getZ()+.5-e.getZ());if(!l.noCollision(e,b))return false;
  var below=p.below();return ColonyCoreBlockEntity.loaded(l,below)&&(!l.getBlockState(below).getCollisionShape(l,below).isEmpty()||wall(l,p)||wall(l,below));
 }
}
