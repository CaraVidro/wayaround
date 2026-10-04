package net.caravidro.wayaround.littleleaf;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ColonyExitBlock extends Block {
    public static final MapCodec<ColonyExitBlock> CODEC=simpleCodec(ColonyExitBlock::new);
    public ColonyExitBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(player instanceof net.minecraft.server.level.ServerPlayer sp)ColonyTravel.leave(sp);return InteractionResult.sidedSuccess(l.isClientSide);}
}
