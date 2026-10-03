package net.caravidro.wayaround.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class AppleLeavesBlock extends LeavesBlock {
    public static final IntegerProperty FRUIT=IntegerProperty.create("fruit",0,3);
    public static final MapCodec<AppleLeavesBlock> CODEC=simpleCodec(AppleLeavesBlock::new);
    public AppleLeavesBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FRUIT,0));}
    @Override public MapCodec<? extends LeavesBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FRUIT);}
    @Override protected VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FRUIT)>0?Shapes.or(Shapes.block(),Block.box(5,-6,5,11,0,11)):Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return Shapes.block();}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(s.getValue(FRUIT)!=3)return InteractionResult.PASS;
        if(!l.isClientSide){
            l.setBlock(p,s.setValue(FRUIT,0),3);
            ItemStack apple=new ItemStack(Items.APPLE);
            if(!player.getInventory().add(apple))player.drop(apple,false);
            l.playSound(null,p,net.minecraft.sounds.SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,net.minecraft.sounds.SoundSource.BLOCKS,.6F,1.1F);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
