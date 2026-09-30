package net.caravidro.wayaround.industrial.mechanical;

import net.caravidro.wayaround.industrial.power.MechanicalTransmissionBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.*;

/** Actual exposed gear: placement face selects the shaft axis, including vertical/bevel pairs. */
public final class GearBlock extends RotatedPillarBlock implements EntityBlock {
    private final boolean large;
    public GearBlock(boolean large, Properties properties) { super(properties); this.large=large; }
    public boolean large(){return large;}
    public int teeth(){return large?32:16;}
    public net.minecraft.world.phys.Vec3 mountOffset(BlockGetter level,BlockPos pos,BlockState state){
        if(large)return net.minecraft.world.phys.Vec3.ZERO;
        Direction.Axis axis=state.getValue(AXIS);
        for(int a=-1;a<=1;a+=2)for(int b=-1;b<=1;b+=2){
            BlockPos delta=switch(axis){case X->new BlockPos(0,a,b);case Y->new BlockPos(a,0,b);case Z->new BlockPos(a,b,0);};
            if(level instanceof Level world&&!world.hasChunkAt(pos.offset(delta)))continue;
            BlockState neighbor=level.getBlockState(pos.offset(delta));
            if(neighbor.getBlock() instanceof GearBlock other&&other.large()&&neighbor.getValue(AXIS)==axis)
                return new net.minecraft.world.phys.Vec3(delta.getX()*.24,delta.getY()*.24,delta.getZ()*.24);
        }return net.minecraft.world.phys.Vec3.ZERO;
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MechanicalTransmissionBlockEntity(pos,state);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof MechanicalTransmissionBlockEntity gear)gear.restoreFromItem(stack);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof MechanicalTransmissionBlockEntity gear)gear.dropAssembly();
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        double lo=large?0:3,hi=large?16:13;
        var offset=mountOffset(level,pos,state);
        return switch(state.getValue(AXIS)){case X->box(6,lo,lo,10,hi,hi).move(offset.x,offset.y,offset.z);case Y->box(lo,6,lo,hi,10,hi).move(offset.x,offset.y,offset.z);case Z->box(lo,lo,6,hi,hi,10).move(offset.x,offset.y,offset.z);};
    }
}
