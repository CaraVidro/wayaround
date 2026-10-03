package net.caravidro.wayaround.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.tags.BlockTags;

public final class AppleTreeBlock extends BaseEntityBlock {
    public static final MapCodec<AppleTreeBlock> CODEC=simpleCodec(AppleTreeBlock::new);
    public AppleTreeBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.is(NatureContent.APPLE_SAPLING.get())?Block.box(2,0,2,14,13,14):Shapes.block();}
    @Override protected boolean canSurvive(BlockState s,net.minecraft.world.level.LevelReader l,BlockPos p){return !s.is(NatureContent.APPLE_SAPLING.get())||l.getBlockState(p.below()).is(BlockTags.DIRT)||l.getBlockState(p.below()).is(Blocks.FARMLAND);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AppleTreeBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,NatureContent.APPLE_TREE_ENTITY.get(),AppleTreeBlockEntity::tick);}
}
