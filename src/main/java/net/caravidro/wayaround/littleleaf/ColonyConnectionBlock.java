package net.caravidro.wayaround.littleleaf;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

/** Satellite entrance shares one queen and one colony; it is never a second independent nest. */
public final class ColonyConnectionBlock extends BaseEntityBlock {
    public static final MapCodec<ColonyConnectionBlock> CODEC=simpleCodec(ColonyConnectionBlock::new);
    public ColonyConnectionBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(ColonyCoreBlock.SPECIES,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ColonyCoreBlock.SPECIES);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ColonyConnectionBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,LittleLeafContent.CONNECTION_ENTITY.get(),ColonyConnectionBlockEntity::tick);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity actor,net.minecraft.world.item.ItemStack stack){
        if(!(l instanceof net.minecraft.server.level.ServerLevel server)||!(l.getBlockEntity(p) instanceof ColonyConnectionBlockEntity satellite))return;
        ColonyCoreBlockEntity best=null;double distance=24*24;int checked=0;
        for(int x=(p.getX()>>4)-2;x<=(p.getX()>>4)+2;x++)for(int z=(p.getZ()>>4)-2;z<=(p.getZ()>>4)+2;z++){
            var chunk=server.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;for(var be:chunk.getBlockEntities().values()){if(++checked>128)break;if(be instanceof ColonyCoreBlockEntity core&&core.giant()&&!core.abandoned()&&!core.interior()&&core.getBlockPos().distSqr(p)<distance){best=core;distance=core.getBlockPos().distSqr(p);}}
        }if(best!=null)satellite.bind(best);
    }
}
