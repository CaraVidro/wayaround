package net.caravidro.wayaround.war.outpost;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class FieldDeviceBlock extends BaseEntityBlock {
    public enum Kind {CONTACT,STILL,SPIKES,GUN,ALARM,BARRICADE,CHARGE}
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active"),BARREL=BooleanProperty.create("barrel");
    public final Kind kind;
    public FieldDeviceBlock(Kind kind,Properties p){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false).setValue(BARREL,false));}
    @Override protected MapCodec<FieldDeviceBlock> codec(){return simpleCodec(p->new FieldDeviceBlock(kind,p));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ACTIVE,BARREL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(kind){case CONTACT,STILL,CHARGE->box(2,0,2,14,3,14);case SPIKES->box(1,0,1,15,10,15);case BARRICADE->box(0,0,0,16,14,16);case ALARM->box(5,0,5,11,12,11);default->box(3,0,3,13,12,13);};
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new FieldDeviceBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,OutpostContent.DEVICE.get(),FieldDeviceBlockEntity::tick);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){if(l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d)d.placed(e);}
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){touch(l,p,e);}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){touch(l,p,e);super.stepOn(l,p,s,e);}
    private void touch(Level l,BlockPos p,Entity e){if(!l.isClientSide&&l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d)d.touch(e);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof CamouflageItem){if(!l.isClientSide&&l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d&&d.enabled()&&d.camouflage()&&!player.getAbilities().instabuild)stack.shrink(1);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
        if(l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d){if(!l.isClientSide)d.interact(player,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d)d.interact(player,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected boolean isSignalSource(BlockState s){return kind==Kind.ALARM;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return kind==Kind.ALARM&&s.getValue(ACTIVE)?15:0;}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof FieldDeviceBlockEntity d)d.removed();super.onRemove(s,l,p,next,moved);}
}
