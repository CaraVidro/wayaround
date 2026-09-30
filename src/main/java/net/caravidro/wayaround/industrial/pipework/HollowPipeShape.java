package net.caravidro.wayaround.industrial.pipework;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** Four thin walls, never a filled core. Branch openings cut the relevant wall. */
final class HollowPipeShape {
    static VoxelShape shape(BlockState state,float radius){
        double lo=8-radius*16,hi=8+radius*16,t=Math.min(.65,radius*4);
        VoxelShape shape=Shapes.empty();boolean any=false;
        for(Direction d:Direction.values())if(state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d))){any=true;
            double a=d.getAxisDirection()==Direction.AxisDirection.NEGATIVE?0:hi;
            double b=d.getAxisDirection()==Direction.AxisDirection.NEGATIVE?lo:16;
            shape=Shapes.or(shape,arm(d.getAxis(),a,b,lo,hi,t));
        }
        // An isolated tube has two visibly open mouths along Z.
        if(!any)return arm(Direction.Axis.Z,0,16,lo,hi,t);
        for (int axis=0;axis<3;axis++) for (double sideA:new double[]{lo,hi-t}) for(double sideB:new double[]{lo,hi-t}) {
            double[] start={lo,lo,lo},end={hi,hi,hi};int a=(axis+1)%3,b=(axis+2)%3;
            start[a]=sideA;end[a]=sideA+t;start[b]=sideB;end[b]=sideB+t;
            shape=Shapes.or(shape,Block.box(start[0],start[1],start[2],end[0],end[1],end[2]));
        }
        return shape;
    }
    private static VoxelShape arm(Direction.Axis axis,double a,double b,double lo,double hi,double t){
        if(b<=a)return Shapes.empty();
        return switch(axis){
            case X->Shapes.or(Block.box(a,lo,lo,b,lo+t,hi),Block.box(a,hi-t,lo,b,hi,hi),Block.box(a,lo+t,lo,b,hi-t,lo+t),Block.box(a,lo+t,hi-t,b,hi-t,hi));
            case Y->Shapes.or(Block.box(lo,a,lo,lo+t,b,hi),Block.box(hi-t,a,lo,hi,b,hi),Block.box(lo+t,a,lo,hi-t,b,lo+t),Block.box(lo+t,a,hi-t,hi-t,b,hi));
            case Z->Shapes.or(Block.box(lo,lo,a,lo+t,hi,b),Block.box(hi-t,lo,a,hi,hi,b),Block.box(lo+t,lo,a,hi-t,lo+t,b),Block.box(lo+t,hi-t,a,hi-t,hi,b));
        };
    }
}
