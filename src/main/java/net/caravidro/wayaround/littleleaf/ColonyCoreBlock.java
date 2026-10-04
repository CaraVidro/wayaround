package net.caravidro.wayaround.littleleaf;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ColonyCoreBlock extends BaseEntityBlock {
    public static final MapCodec<ColonyCoreBlock> CODEC=simpleCodec(ColonyCoreBlock::new);
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,4),SPECIES=IntegerProperty.create("species",0,3);
    public static final BooleanProperty GIANT=BooleanProperty.create("giant");
    public ColonyCoreBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(STAGE,0).setValue(SPECIES,0).setValue(GIANT,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(STAGE,SPECIES,GIANT);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ColonyCoreBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,LittleLeafContent.CORE_ENTITY.get(),ColonyCoreBlockEntity::tick);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){
        if(!l.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer sp&&l.getBlockEntity(p) instanceof ColonyCoreBlockEntity c){
            if(sp.getScale()<=.2F)ColonyTravel.enter(sp,c);
            else player.displayClientMessage(Component.translatable("message.wayaround.colony.status",Component.translatable("colony.wayaround.stage."+c.stage()),c.work(),c.giant()?"↑":"·"),true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
