package net.caravidro.wayaround.industrial.mechanical;

import java.util.*;
import javax.annotation.Nullable;
import net.caravidro.wayaround.industrial.power.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded real transmission paths, tooth ratios, direction reversals and shared source budgets. */
public final class MechanicalTransmission {
    private static final int MAX_NETWORK_NODES=192;
    private record Route(BlockPos pos,List<BlockPos> path,float ratio,int sign){}
    private record Feed(BlockPos pos,IRotationalPower source,List<BlockPos> path,float ratio,int sign,float efficiency) {
        float rpm(){return source.rpm()*ratio*sign;}
        float power(){return Math.max(0,source.power())*efficiency;}
    }
    private MechanicalTransmission(){}
    @Nullable public static IRotationalPower findSource(Level level,BlockPos consumer,Direction direction){return findSourceExcluding(level,consumer,direction,null);}
    @Nullable public static IRotationalPower findSourceExcluding(Level level,BlockPos consumer,Direction direction,@Nullable BlockPos excluded){
        if(!WorldFeatureRuntime.enabled(level,WorldFeature.POWER_NETWORKS))return null;
        BlockPos start=consumer.relative(direction);
        if(!level.hasChunkAt(start))return null;
        IRotationalPower direct=sourceAt(level,start,direction.getOpposite(),excluded);
        if(direct!=null&&direct.axis()==direction.getAxis())return direct;
        if(!accepts(level.getBlockState(start),direction.getAxis()))return null;
        return network(level,start,direction.getAxis(),excluded);
    }
    @Nullable public static IRotationalPower forNode(Level level,BlockPos node){
        if(!WorldFeatureRuntime.enabled(level,WorldFeature.POWER_NETWORKS)||!level.hasChunkAt(node))return null;
        BlockState state=level.getBlockState(node);
        return transmission(state)?network(level,node,axis(state),null):null;
    }
    @Nullable private static IRotationalPower network(Level level,BlockPos start,Direction.Axis output,@Nullable BlockPos excluded){
        ArrayDeque<Route> queue=new ArrayDeque<>();Map<BlockPos,Route> visited=new HashMap<>();Map<BlockPos,Feed> feeds=new LinkedHashMap<>();
        queue.add(new Route(start,List.of(start),1,1));boolean conflict=false;
        while(!queue.isEmpty()&&visited.size()<MAX_NETWORK_NODES){
            Route route=queue.removeFirst();Route prior=visited.get(route.pos());
            if(prior!=null){if(Math.abs(prior.ratio()-route.ratio())>.01F||prior.sign()!=route.sign())conflict=true;continue;}
            if(!level.hasChunkAt(route.pos()))continue;
            BlockState state=level.getBlockState(route.pos());if(!transmission(state))continue;visited.put(route.pos(),route);
            for(Direction side:Direction.values()){
                if(!accepts(state,side.getAxis()))continue;
                BlockPos next=route.pos().relative(side);if(!level.hasChunkAt(next))continue;
                IRotationalPower source=sourceAt(level,next,side.getOpposite(),excluded);
                if(source!=null&&source.axis()==side.getAxis())feeds.putIfAbsent(next,new Feed(next,source,route.path(),route.ratio(),route.sign(),efficiency(level,route.path())));
            }
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
                int distance=Math.abs(dx)+Math.abs(dy)+Math.abs(dz);if(distance==0||distance>2)continue;
                BlockPos next=route.pos().offset(dx,dy,dz);if(!level.hasChunkAt(next))continue;
                BlockState other=level.getBlockState(next);if(!transmission(other))continue;
                float ratio=1;int sign=1;
                if(state.getBlock() instanceof GearBlock gear&&other.getBlock() instanceof GearBlock neighbor){
                    Direction.Axis a=axis(state),b=axis(other);
                    int axial=a==Direction.Axis.X?dx:a==Direction.Axis.Y?dy:dz;
                    if(a==b&&axial!=0){if(distance!=1)continue;} // coaxial shafts
                    else {
                        if(a==b&&distance==2&&gear.large()&&neighbor.large())continue;
                        if(a!=b&&distance!=1)continue; // touching bevel pair
                        ratio=(float)neighbor.teeth()/gear.teeth();sign=-1;
                    }
                }else{
                    if(distance!=1)continue;
                    Direction.Axis offset=dx!=0?Direction.Axis.X:dy!=0?Direction.Axis.Y:Direction.Axis.Z;
                    if(!accepts(state,offset)||!accepts(other,offset))continue;
                }
                float combined=route.ratio()*ratio;if(combined<1F/64||combined>64)continue;
                ArrayList<BlockPos> path=new ArrayList<>(route.path());path.add(next);
                queue.addLast(new Route(next,List.copyOf(path),combined,route.sign()*sign));
            }
        }
        if(conflict||feeds.isEmpty())return null; // contradictory tooth loops physically lock
        Feed best=feeds.values().stream().max(Comparator.comparingDouble(f->sourceScore(f.source())*f.efficiency())).orElseThrow();
        List<Feed> compatible=feeds.values().stream().filter(f->Math.abs(f.rpm()-best.rpm())<=Math.max(2,Math.abs(best.rpm())*.25F)).toList();
        return new CombinedPower(level,compatible,output);
    }
    private static boolean transmission(BlockState s){return s.getBlock() instanceof MechanicalShaftBlock||s.getBlock() instanceof MechanicalGearboxBlock||s.getBlock() instanceof GearBlock;}
    private static Direction.Axis axis(BlockState s){return s.hasProperty(MechanicalShaftBlock.AXIS)?s.getValue(MechanicalShaftBlock.AXIS):Direction.Axis.X;}
    private static boolean accepts(BlockState s,Direction.Axis side){return s.getBlock() instanceof MechanicalGearboxBlock||(transmission(s)&&axis(s)==side);}
    private static float efficiency(Level level,List<BlockPos> path){float value=1;for(BlockPos p:path)value*=level.getBlockEntity(p) instanceof MechanicalTransmissionBlockEntity part?part.transmissionEfficiency():.978F;return Math.max(.05F,value);}
    @Nullable private static IRotationalPower sourceAt(Level level,BlockPos pos,Direction side,@Nullable BlockPos excluded){
        if(!level.hasChunkAt(pos)||pos.equals(excluded)||transmission(level.getBlockState(pos)))return null;
        IRotationalPower value=level.getCapability(MechanicalCapabilities.ROTATION,pos,side);
        return value!=null?value:level.getCapability(MechanicalCapabilities.ROTATION,pos,null);
    }
    public static float sourceScore(@Nullable IRotationalPower s){return s==null?-1:Math.max(0,s.power())+Math.abs(s.rpm())*.18F+(Math.abs(s.rpm())>.05F?10:0);}
    @Nullable public static WaterWheelHubBlockEntity findVisualWheel(Level level,BlockPos pos){
        // Kept for existing integrations. Actual renderer now follows the computed ratio.
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();Set<BlockPos> seen=new HashSet<>();queue.add(pos);
        while(!queue.isEmpty()&&seen.size()<MAX_NETWORK_NODES){BlockPos p=queue.removeFirst();if(!seen.add(p)||!level.hasChunkAt(p))continue;
            BlockState s=level.getBlockState(p);if(!transmission(s))continue;
            for(Direction d:Direction.values()){BlockPos n=p.relative(d);if(!level.hasChunkAt(n)||!accepts(s,d.getAxis()))continue;
                if(level.getBlockEntity(n) instanceof WaterWheelHubBlockEntity wheel&&wheel.axleAxis()==d.getAxis())return wheel;
                if(accepts(level.getBlockState(n),d.getAxis()))queue.add(n);
            }}return null;
    }
    private static final class CombinedPower implements IRotationalPower {
        private final Level level;private final List<Feed> feeds;private final Direction.Axis axis;
        CombinedPower(Level level,List<Feed> feeds,Direction.Axis axis){this.level=level;this.feeds=feeds;this.axis=axis;}
        public float rpm(){float weight=0,value=0;for(Feed f:feeds){float w=Math.max(.001F,f.power());weight+=w;value+=f.rpm()*w;}return weight>0?value/weight:0;}
        public float power(){float value=0;for(Feed f:feeds)value+=f.power();return value;}
        public float torque(){float value=0;for(Feed f:feeds)value+=Math.max(0,f.source().torque())*f.efficiency()/f.ratio();return value;}
        public Direction.Axis axis(){return axis;}
        public int rotationDirection(){return rpm()>.01F?1:rpm()<-.01F?-1:0;}
        public float consumePower(float requested){float total=power(),granted=0;if(total<=0||requested<=0)return 0;
            for(Feed f:feeds){float share=Math.min(requested,total)*f.power()/total;float take=f.source().consumePower(share/f.efficiency());granted+=take*f.efficiency();
                for(BlockPos p:f.path())if(level.getBlockEntity(p) instanceof MechanicalTransmissionBlockEntity part)part.applyMechanicalLoad(take,f.rpm());
            }return Math.min(requested,granted);
        }
    }
}
