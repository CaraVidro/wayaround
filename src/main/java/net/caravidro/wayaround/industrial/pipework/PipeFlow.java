package net.caravidro.wayaround.industrial.pipework;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Fluids are removed only after a simulated drain succeeds; outlets never duplicate a bucket. */
public final class PipeFlow {
    private static final int MAX_NODES=128;
    private record Step(PipeBlockEntity pipe,Direction arrival,List<PipeBlockEntity> path){}
    private record Outlet(PipeBlockEntity pipe,Direction direction,List<PipeBlockEntity> path){}
    private PipeFlow(){}
    public static int mouthDistance(PipeBlockEntity pipe){return pipe.getBlockState().getBlock() instanceof LargePipeBlock?2:1;}
    private static boolean axisAllows(PipeBlockEntity pipe,Direction direction){return !(pipe.getBlockState().getBlock() instanceof LargePipeBlock)||pipe.getBlockState().getValue(LargePipeBlock.AXIS)==direction.getAxis();}
    private static PipeBlockEntity neighbor(ServerLevel level,PipeBlockEntity pipe,Direction direction){
        int step=pipe.getBlockState().getBlock() instanceof LargePipeBlock?3:1;
        BlockPos p=pipe.getBlockPos().relative(direction,step);
        if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof PipeBlockEntity other&&other.owner()==null&&other.complete()&&axisAllows(other,direction))return other;
        // Adapter: a regular pipe mouth touches a large pipe's open end, without entering the shell.
        int adapter=pipe.getBlockState().getBlock() instanceof LargePipeBlock?2:2;
        p=pipe.getBlockPos().relative(direction,adapter);
        if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof PipeBlockEntity other&&other.owner()==null&&other.complete()&&axisAllows(other,direction)&&((pipe.getBlockState().getBlock() instanceof LargePipeBlock)!=(other.getBlockState().getBlock() instanceof LargePipeBlock)))return other;
        return null;
    }
    private static List<Outlet> outlets(ServerLevel level,PipeBlockEntity root){
        ArrayDeque<Step> queue=new ArrayDeque<>();Set<BlockPos> seen=new HashSet<>();ArrayList<Outlet> outlets=new ArrayList<>();
        queue.add(new Step(root,root.flow(),List.of(root)));
        while(!queue.isEmpty()&&seen.size()<MAX_NODES){Step step=queue.removeFirst();PipeBlockEntity pipe=step.pipe();if(!seen.add(pipe.getBlockPos()))continue;
            if(pipe!=root&&pipe.hasValve()&&!pipe.open())continue;
            int connections=0;
            for(Direction direction:Direction.values()){
                if(!axisAllows(pipe,direction)||direction==step.arrival().getOpposite())continue;
                if(pipe==root&&direction!=root.flow())continue;
                PipeBlockEntity next=neighbor(level,pipe,direction);if(next==null)continue;connections++;
                if(seen.contains(next.getBlockPos()))continue;
                ArrayList<PipeBlockEntity> path=new ArrayList<>(step.path());path.add(next);queue.addLast(new Step(next,direction,List.copyOf(path)));
            }
            if(connections==0)outlets.add(new Outlet(pipe,step.arrival(),step.path()));
        }return outlets;
    }
    public static void pump(ServerLevel level,PipeBlockEntity valve){
        List<Outlet> outputs=outlets(level,valve);if(outputs.isEmpty())return;
        intake(level,valve);
        if(valve.getBlockState().getBlock() instanceof LargePipeBlock duct){
            int budget=duct.colossal()?16:4;
            for(BlockPos source:mouthArea(valve,valve.flow().getOpposite())){
                if(--budget<=0)break;
                intakeAt(level,valve,source);
            }
        }
        if(valve.stored().isEmpty())return;
        Outlet outlet=outputs.get(valve.nextOutlet(outputs.size()));
        BlockPos end=outlet.pipe().getBlockPos().relative(outlet.direction(),mouthDistance(outlet.pipe()));
        if(!level.hasChunkAt(end))return;
        FluidStack fluid=valve.stored();int limit=valve.getBlockState().getBlock() instanceof LargePipeBlock duct?(duct.colossal()?16000:4000):1000;
        for(PipeBlockEntity part:outlet.path())if(part.getBlockState().getBlock() instanceof IndustrialPipeBlock pipe)limit=Math.min(limit,pipe.spec().flowPerTick());
        IFluidHandler receiver=level.getCapability(Capabilities.FluidHandler.BLOCK,end,outlet.direction().getOpposite());
        int consumed=0;
        if(receiver!=null){consumed=receiver.fill(fluid.copyWithAmount(Math.min(limit,fluid.getAmount())),IFluidHandler.FluidAction.EXECUTE);}
        else if(outlet.pipe().getBlockState().getBlock() instanceof LargePipeBlock||outlet.pipe().getBlockState().is(PipeworkContent.LARGE_WATER_MAIN.get())){
            if(fluid.getAmount()>=1000&&limit>=1000){
                for(BlockPos mouth:mouthArea(outlet.pipe(),outlet.direction())){
                    if(consumed+1000>Math.min(limit,fluid.getAmount()))break;
                    consumed+=spill(level,mouth,fluid,true);
                }
            }
        }else if(level.getBlockState(end).isAir()){
            consumed=Math.min(limit,fluid.getAmount());jet(level,end,outlet.direction(),fluid,8);
        }
        if(consumed>0){FluidStack marking=fluid.copyWithAmount(1);valve.used(consumed);
            for(int i=0;i<outlet.path().size();i++){PipeBlockEntity pipe=outlet.path().get(i);Direction d=i+1<outlet.path().size()?Direction.getNearest(outlet.path().get(i+1).getBlockPos().getX()-pipe.getBlockPos().getX(),outlet.path().get(i+1).getBlockPos().getY()-pipe.getBlockPos().getY(),outlet.path().get(i+1).getBlockPos().getZ()-pipe.getBlockPos().getZ()):outlet.direction();pipe.markFlow(marking,d);}
        }
    }
    private static void intake(ServerLevel level,PipeBlockEntity pipe){
        BlockPos source=pipe.getBlockPos().relative(pipe.flow().getOpposite(),mouthDistance(pipe));if(!level.hasChunkAt(source))return;
        intakeAt(level,pipe,source);
    }
    private static List<BlockPos> mouthArea(PipeBlockEntity pipe,Direction direction){
        BlockPos mouth=pipe.getBlockPos().relative(direction,mouthDistance(pipe));
        ArrayList<BlockPos> positions=new ArrayList<>();positions.add(mouth);
        if(pipe.getBlockState().getBlock() instanceof LargePipeBlock duct){int r=duct.radius();
            for(int a=-r;a<=r;a++)for(int b=-r;b<=r;b++)if(a!=0||b!=0)positions.add(switch(direction.getAxis()){case X->mouth.offset(0,a,b);case Y->mouth.offset(a,0,b);case Z->mouth.offset(a,b,0);});
        }return positions;
    }
    private static void intakeAt(ServerLevel level,PipeBlockEntity pipe,BlockPos source){
        if(!level.hasChunkAt(source))return;
        int room=pipe.capacity()-pipe.amount();if(room<=0)return;
        IFluidHandler handler=level.getCapability(Capabilities.FluidHandler.BLOCK,source,pipe.flow());FluidStack fluid;
        if(handler!=null){
            fluid=pipe.stored().isEmpty()?handler.drain(Math.min(1000,room),IFluidHandler.FluidAction.SIMULATE):handler.drain(pipe.stored().copyWithAmount(Math.min(1000,room)),IFluidHandler.FluidAction.SIMULATE);
            if(fluid.isEmpty()||(!pipe.stored().isEmpty()&&!FluidStack.isSameFluidSameComponents(fluid,pipe.stored())))return;
            fluid=handler.drain(fluid,IFluidHandler.FluidAction.EXECUTE);if(fluid.isEmpty())return;
        }else{
            var state=level.getBlockState(source);var liquid=state.getFluidState();
            if(room<1000||!(state.getBlock() instanceof LiquidBlock)||!liquid.isSource())return;
            fluid=new FluidStack(liquid.getType(),1000);
            if(!pipe.stored().isEmpty()&&!FluidStack.isSameFluidSameComponents(fluid,pipe.stored()))return;
            if(!level.setBlock(source,Blocks.AIR.defaultBlockState(),3))return;
        }
        pipe.receive(fluid);jet(level,source,pipe.flow(),fluid,5);
    }
    public static int spill(ServerLevel level,BlockPos end,FluidStack fluid,boolean particles){
        if(!level.hasChunkAt(end)||fluid.getAmount()<1000||!level.getBlockState(end).canBeReplaced()||!level.getFluidState(end).isEmpty())return 0;
        var state=fluid.getFluid().defaultFluidState().createLegacyBlock();
        if(!(state.getBlock() instanceof LiquidBlock))return 0; // fluids without world blocks remain in the valve tank
        if(!level.setBlock(end,state,3))return 0;
        if(particles)jet(level,end,Direction.DOWN,fluid,12);return 1000;
    }
    public static ParticleOptions drip(FluidStack fluid){return !fluid.isEmpty()&&fluid.getFluid().is(FluidTags.LAVA)?ParticleTypes.DRIPPING_LAVA:ParticleTypes.DRIPPING_WATER;}
    public static ParticleOptions spray(FluidStack fluid){
        if(!fluid.isEmpty()&&fluid.getFluid().is(FluidTags.LAVA))return ParticleTypes.FALLING_LAVA;
        if(fluid.isEmpty()||fluid.getFluid().is(FluidTags.WATER))return ParticleTypes.SPLASH;
        return new BlockParticleOption(ParticleTypes.BLOCK,fluid.getFluid().defaultFluidState().createLegacyBlock());
    }
    private static void jet(ServerLevel level,BlockPos pos,Direction d,FluidStack fluid,int count){
        for(int i=0;i<count;i++)level.sendParticles(spray(fluid),pos.getX()+.5+(level.random.nextDouble()-.5)*.3,
                pos.getY()+.5+(level.random.nextDouble()-.5)*.3,pos.getZ()+.5+(level.random.nextDouble()-.5)*.3,
                0,d.getStepX(),d.getStepY(),d.getStepZ(),.18);
    }
}
