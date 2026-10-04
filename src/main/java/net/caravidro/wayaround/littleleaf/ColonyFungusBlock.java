package net.caravidro.wayaround.littleleaf;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Harvesting leaves the living culture in place, with a saved block-tick cooldown. */
public final class ColonyFungusBlock extends Block {
    public static final MapCodec<ColonyFungusBlock> CODEC=simpleCodec(ColonyFungusBlock::new);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ALIVE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("alive");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty RIPE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("ripe");
    public ColonyFungusBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(RIPE,true).setValue(ALIVE,true));}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> b){b.add(RIPE,ALIVE);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){
        if(!l.isClientSide&&s.getValue(ALIVE)&&s.getValue(RIPE)){player.addItem(new ItemStack(LittleLeafContent.FUNGUS_ITEM.get()));l.setBlock(p,s.setValue(RIPE,false),3);l.scheduleTick(p,this,24000);}
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(s.getValue(ALIVE))l.setBlock(p,s.setValue(RIPE,true),3);}
}
