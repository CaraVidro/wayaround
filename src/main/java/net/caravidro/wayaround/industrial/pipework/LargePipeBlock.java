package net.caravidro.wayaround.industrial.pipework;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Three-block-long hollow duct, assembled from paid sections, with real shell collision. */
public final class LargePipeBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.AXIS;
    public static final BooleanProperty SHELL=BooleanProperty.create("shell");
    private final boolean colossal;
    public LargePipeBlock(boolean colossal,Properties props){super(props);this.colossal=colossal;registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.Z).setValue(SHELL,false));}
    public int required(){return colossal?50:15;}
    public int radius(){return colossal?2:1;}
    public boolean colossal(){return colossal;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return MapCodec.unit(()->this);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AXIS,SHELL);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(AXIS,c.getClickedFace().getAxis());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PipeBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return type==PipeworkContent.PIPE_ENTITY.get()?(w,p,b,e)->PipeBlockEntity.tick(w,p,b,(PipeBlockEntity)e):null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof PipeBlockEntity part))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        PipeBlockEntity pipe=part.controller();if(pipe==null)return ItemInteractionResult.FAIL;
        if(stack.is(pipe.getBlockState().getBlock().asItem())){
            if(!l.isClientSide)pipe.assemble(stack,player);
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(stack.is(PipeworkContent.VALVE.get())){if(!l.isClientSide)pipe.installValve(stack,player,player.getNearestViewDirection());return ItemInteractionResult.sidedSuccess(l.isClientSide);}
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l.getBlockEntity(p) instanceof PipeBlockEntity part){PipeBlockEntity pipe=part.controller();if(pipe!=null)pipe.turn(player);}
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState replacement,boolean moving){
        if(!s.is(replacement.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof PipeBlockEntity pipe)pipe.dismantle();
        super.onRemove(s,l,p,replacement,moving);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext context){
        if(!s.getValue(SHELL))return Block.box(7,0,7,9,2,9); // assembly fastening point; center passage stays open
        if(!(l.getBlockEntity(p) instanceof PipeBlockEntity part)||part.owner()==null)return Shapes.empty();
        BlockPos d=p.subtract(part.owner());int r=radius();Direction.Axis axis=s.getValue(AXIS);VoxelShape shape=Shapes.empty();
        for(Direction face:Direction.values())if(face.getAxis()!=axis){int v=face.getAxis()==Direction.Axis.X?d.getX():face.getAxis()==Direction.Axis.Y?d.getY():d.getZ();
            if(v!=(face.getAxisDirection()==Direction.AxisDirection.POSITIVE?r:-r))continue;
            double lo=face.getAxisDirection()==Direction.AxisDirection.POSITIVE?13:0,hi=lo+3;
            shape=Shapes.or(shape,switch(face.getAxis()){case X->Block.box(lo,0,0,hi,16,16);case Y->Block.box(0,lo,0,16,hi,16);case Z->Block.box(0,0,lo,16,16,hi);});
        }return shape;
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(SHELL)?getShape(s,l,p,c):Shapes.empty();}
}
