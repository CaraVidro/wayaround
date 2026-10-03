package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.ecology.TreeWoodSegmentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class BranchConnectionGameTests {
    @GameTest(template="assembly_test",batch="nature",timeoutTicks=80)
    public static void branchTurnsAreContinuousAtEveryThickness(GameTestHelper h) {
        var level=h.getLevel();BlockPos center=h.absolutePos(new BlockPos(5,5,5));
        for(Direction direction:Direction.values())level.setBlock(center.relative(direction),Blocks.AIR.defaultBlockState(),3);
        for(Direction.Axis axis:Direction.Axis.values())for(Direction direction:Direction.values()) {
            if(direction.getAxis()==axis)continue;
            for(int thickness=1;thickness<=4;thickness++) {
                var a=EcologyContent.OAK_TREE_SEGMENT.get().defaultBlockState().setValue(TreeWoodSegmentBlock.AXIS,axis).setValue(TreeWoodSegmentBlock.THICKNESS,thickness);
                var b=a.setValue(TreeWoodSegmentBlock.AXIS,direction.getAxis()).setValue(TreeWoodSegmentBlock.THICKNESS,1);
                BlockPos next=center.relative(direction);level.setBlock(center,a,3);level.setBlock(next,b,3);
                var shapeA=a.getCollisionShape(level,center,CollisionContext.empty());
                var shapeB=b.getCollisionShape(level,next,CollisionContext.empty());
                for(int step=0;step<=20;step++) {
                    double t=step/20.0,x=.5+direction.getStepX()*t,y=.5+direction.getStepY()*t,z=.5+direction.getStepZ()*t;
                    boolean inside=shapeA.toAabbs().stream().anyMatch(box->box.inflate(1e-7).contains(x,y,z))
                            ||shapeB.toAabbs().stream().anyMatch(box->box.inflate(1e-7).contains(x-direction.getStepX(),y-direction.getStepY(),z-direction.getStepZ()));
                    h.assertTrue(inside,"Face-connected branch centers must have no air gap, including bends and taper");
                }
                level.setBlock(next,Blocks.AIR.defaultBlockState(),3);
                h.assertTrue(TreeWoodSegmentBlock.branchConnections(a,level,center)==0,"Removing wood removes the joint without stale shape data");
            }
        }
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="nature",timeoutTicks=80)
    public static void branchJointsRequireWoodAndPreserveRoots(GameTestHelper h) {
        var level=h.getLevel();BlockPos center=h.absolutePos(new BlockPos(5,5,5));
        for(Direction direction:Direction.values())level.setBlock(center.relative(direction),Blocks.AIR.defaultBlockState(),3);
        var branch=EcologyContent.BIRCH_TREE_SEGMENT.get().defaultBlockState().setValue(TreeWoodSegmentBlock.AXIS,Direction.Axis.X).setValue(TreeWoodSegmentBlock.THICKNESS,1);
        level.setBlock(center,branch,3);level.setBlock(center.above(),Blocks.BIRCH_LEAVES.defaultBlockState(),3);
        h.assertTrue(TreeWoodSegmentBlock.branchConnections(branch,level,center)==0,"Leaves never become wooden links");
        level.setBlock(center.above(),Blocks.BIRCH_LOG.defaultBlockState(),3);
        h.assertTrue(TreeWoodSegmentBlock.branchConnections(branch,level,center)==1<<Direction.UP.ordinal(),"A trunk receives only the actual adjacent limb");
        var root=branch.setValue(TreeWoodSegmentBlock.ROOT,true);
        h.assertTrue(TreeWoodSegmentBlock.branchConnections(root,level,center)==0,"Terrain-following root models keep their existing joints");
        h.succeed();
    }
}
