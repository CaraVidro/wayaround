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
 private List<BlockPos> path=List.of();private BlockPos goal;private int at,visited,stationary,replans;private boolean failed,pointGoal;
 private Vec3 progress;private final Set<BlockPos> blocked=new HashSet<>();
 public void reset(){open.clear();cost.clear();parent.clear();path=List.of();goal=null;at=visited=stationary=replans=0;failed=false;progress=null;blocked.clear();}
 public String status(){return "nodes="+visited+", queue="+open.size()+", path="+at+"/"+path.size()+", failed="+failed;}
 public boolean failed(){return failed;}
 /** Arrival is measured at the same voxel surface used by the route, not the integer block floor. */
 public boolean reached(ColonyInsectEntity e,BlockPos target,double tolerance){
  if(!(e.level() instanceof ServerLevel l))return false;
  var surface=point(l,e,target,false);
  return surface!=null&&e.position().distanceToSqr(surface)<tolerance*tolerance;
 }

 public void followPoint(ColonyInsectEntity e,BlockPos target){follow(e,target,true);}
 public void follow(ColonyInsectEntity e,BlockPos target){follow(e,target,false);}
 private void follow(ColonyInsectEntity e,BlockPos target,boolean walkToCenter){
  if(!(e.level() instanceof ServerLevel l))return;if(e.yielding())return;
  if(!target.equals(goal)||pointGoal!=walkToCenter){reset();pointGoal=walkToCenter;goal=target.immutable();var start=e.blockPosition();cost.put(start,0.0);open.add(new Node(start,0,heuristic(start)));}
  for(int k=0;path.isEmpty()&&!open.isEmpty()&&k<12&&ColonyBudget.search(l);k++){
   var n=open.remove();if(n.cost>cost.getOrDefault(n.pos,Double.MAX_VALUE))continue;
   if(++visited>384){failed=true;open.clear();break;}
   var nodePoint=point(l,e,n.pos,false);if(nodePoint==null)continue;var box=e.getBoundingBox().move(nodePoint.subtract(e.position()));
   if(pointGoal?n.pos.equals(goal):box.inflate(e.carryingMaterial()?.85:e.enlarged()?.65:.20).intersects(new AABB(goal))&&(!e.carryingMaterial()||!box.intersects(new AABB(goal)))){var route=new ArrayList<BlockPos>();for(var p=n.pos;p!=null;p=parent.get(p))route.add(p);Collections.reverse(route);path=route;at=0;break;}
   for(var d:Direction.values()){
    var p=n.pos.relative(d);if(Math.abs(p.getX()-goal.getX())>(e.inside()?96:32)||Math.abs(p.getZ()-goal.getZ())>(e.inside()?96:32)||Math.abs(p.getY()-goal.getY())>40||blocked.contains(p)||!valid(l,e,p))continue;
    double next=n.cost+(d.getAxis()==Direction.Axis.Y?1.2:1);if(next>=cost.getOrDefault(p,Double.MAX_VALUE))continue;
    cost.put(p,next);parent.put(p,n.pos);open.add(new Node(p,next,next+heuristic(p)));
   }
  }
  if(path.isEmpty()){if(open.isEmpty())failed=true;return;}
  while(at<path.size()){
   var p=path.get(at);boolean vertical=(at>0&&path.get(at-1).getY()!=p.getY())||(at+1<path.size()&&path.get(at+1).getY()!=p.getY());Vec3 point=point(l,e,p,vertical);if(point==null){retry(e,p);return;}double tolerance=e.enlarged()?.06:.055;
   if(e.position().distanceToSqr(point)<tolerance*tolerance){at++;stationary=0;progress=e.position();continue;}
   if(progress==null||e.position().distanceToSqr(progress)>.025*.025){progress=e.position();stationary=0;}else if(++stationary>60){retry(e,p);return;}
   if(!ColonyTraffic.permit(e,point))return;
   var delta=point.subtract(e.position());boolean up=Math.abs(delta.y)>.015;
   double speed=e.enlarged()?.13:.045;Vec3 horizontal=new Vec3(delta.x,0,delta.z);if(horizontal.length()>speed)horizontal=horizontal.normalize().scale(speed);
   double dy=up?Math.clamp(delta.y+(delta.y>0?.08:0),-.12,.20):e.getDeltaMovement().y;
   if(horizontal.lengthSqr()>1e-6)e.setYRot((float)(Math.atan2(-horizontal.x,horizontal.z)*180/Math.PI));
   e.routeClimbing(up);e.getNavigation().stop();e.setDeltaMovement(horizontal.x,dy,horizontal.z);e.getLookControl().setLookAt(point.x,point.y+e.getEyeHeight(),point.z,30,30);return;
  }
  if(!e.enlarged()&&!pointGoal){
   var p=e.position();double x=Math.clamp(p.x,goal.getX(),goal.getX()+1),y=Math.clamp(p.y,goal.getY(),goal.getY()+1),z=Math.clamp(p.z,goal.getZ(),goal.getZ()+1);var nearest=new Vec3(x,y,z);var away=p.subtract(nearest);
   if(away.lengthSqr()>1e-5){var desired=nearest.add(away.normalize().scale(e.getBbWidth()*.5+.055));var d=desired.subtract(p);if(d.length()>.02){var motion=d.normalize().scale(Math.min(.045,d.length()));e.setDeltaMovement(motion);e.routeClimbing(Math.abs(motion.y)>.01);if(motion.horizontalDistanceSqr()>1e-6)e.setYRot((float)(Math.atan2(-motion.x,motion.z)*180/Math.PI));return;}}
  }
  e.routeClimbing(false);e.setDeltaMovement(0,e.getDeltaMovement().y,0);
 }
 private void retry(ColonyInsectEntity e,BlockPos obstruction){
  if(++replans>8){failed=true;return;}if(at>0)blocked.add(obstruction);if(blocked.size()>24)blocked.clear();
  open.clear();cost.clear();parent.clear();path=List.of();at=visited=stationary=0;progress=null;
  var start=e.blockPosition();cost.put(start,0.0);open.add(new Node(start,0,heuristic(start)));e.setDeltaMovement(0,e.getDeltaMovement().y,0);
 }
 /** Feet follow the actual voxel top: roots/slabs are not imaginary full-height cubes. */
 private static Vec3 point(ServerLevel l,ColonyInsectEntity e,BlockPos p,boolean vertical){
  if(e.enlarged())return waypoint(l,e,p,vertical);
  if(!ColonyCoreBlockEntity.loaded(l,p)||!ColonyCoreBlockEntity.loaded(l,p.below())||!l.getFluidState(p).isEmpty())return null;
  double x=p.getX()+.5,z=p.getZ()+.5,top=Double.NEGATIVE_INFINITY,half=e.getBbWidth()*.5;
  for(int down=0;down<=1;down++){var q=p.below(down);for(var b:l.getBlockState(q).getCollisionShape(l,q).toAabbs()){
   if(b.minX+q.getX()<x+half&&b.maxX+q.getX()>x-half&&b.minZ+q.getZ()<z+half&&b.maxZ+q.getZ()>z-half)top=Math.max(top,q.getY()+b.maxY);
  }}
  if(Double.isFinite(top)){var v=new Vec3(x,top,z);if(l.noCollision(e,e.getBoundingBox().move(v.subtract(e.position()))))return v;}
  var v=waypoint(l,e,p,true);return l.noCollision(e,e.getBoundingBox().move(v.subtract(e.position())))&&(wall(l,p,1)||wall(l,p.below(),1))?v:null;
 }
 private static Vec3 waypoint(ServerLevel l,ColonyInsectEntity e,BlockPos p,boolean vertical){
  var center=Vec3.atBottomCenterOf(p);if(!vertical)return center;int range=Math.max(1,(int)Math.ceil(e.getBbWidth()*.5));
  for(int down=0;down<=1;down++)for(var d:Direction.Plane.HORIZONTAL)for(int step=1;step<=range;step++){
   var q=p.below(down).relative(d,step);if(!ColonyCoreBlockEntity.loaded(l,q))continue;var shape=l.getBlockState(q).getCollisionShape(l,q);if(shape.isEmpty())continue;var wall=shape.bounds().move(q.getX(),q.getY(),q.getZ());double gap=e.getBbWidth()*.5+.03;
   var point=switch(d){case EAST->new Vec3(wall.minX-gap,center.y,center.z);case WEST->new Vec3(wall.maxX+gap,center.y,center.z);case SOUTH->new Vec3(center.x,center.y,wall.minZ-gap);case NORTH->new Vec3(center.x,center.y,wall.maxZ+gap);default->center;};
   if(l.noCollision(e,e.getBoundingBox().move(point.subtract(e.position()))))return point;
  }return center;
 }
 private double heuristic(BlockPos p){return Math.abs(p.getX()-goal.getX())+Math.abs(p.getY()-goal.getY())+Math.abs(p.getZ()-goal.getZ());}
 private static boolean wall(ServerLevel l,BlockPos p,int range){for(var d:Direction.Plane.HORIZONTAL)for(int step=1;step<=range;step++){var q=p.relative(d,step);if(ColonyCoreBlockEntity.loaded(l,q)&&!l.getBlockState(q).getCollisionShape(l,q).isEmpty())return true;}return false;}
 private static boolean valid(ServerLevel l,ColonyInsectEntity e,BlockPos p){
  if(!ColonyCoreBlockEntity.loaded(l,p)||!ColonyCoreBlockEntity.loaded(l,p.above(2))||!l.getFluidState(p).isEmpty()||!l.getFluidState(p.below()).isEmpty())return false;
  if(!e.enlarged())return point(l,e,p,false)!=null;
  var b=e.getBoundingBox().move(p.getX()+.5-e.getX(),p.getY()-e.getY(),p.getZ()+.5-e.getZ());if(!l.noCollision(e,b))return false;
  var below=p.below();return ColonyCoreBlockEntity.loaded(l,below)&&(!l.getBlockState(below).getCollisionShape(l,below).isEmpty()||wall(l,p,Math.max(1,(int)Math.ceil(e.getBbWidth()*.5)))||wall(l,below,Math.max(1,(int)Math.ceil(e.getBbWidth()*.5))));
 }
}
