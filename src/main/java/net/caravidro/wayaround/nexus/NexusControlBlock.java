package net.caravidro.wayaround.nexus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Existing transit network's physical cutoff. A deliberate crouching interaction closes every route. */
public final class NexusControlBlock extends Block {
    public static final MapCodec<NexusControlBlock> CODEC=simpleCodec(NexusControlBlock::new);
    public NexusControlBlock(Properties p){super(p);}
    @Override protected MapCodec<NexusControlBlock> codec(){return CODEC;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.dimension().equals(NexusPortalManager.NEXUS))return InteractionResult.PASS;
        if(!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel server) {
            var data=NexusTransitData.get(server.getServer());
            if(!data.open())player.displayClientMessage(Component.translatable("message.wayaround.nexus.cutoff_dead"),false);
            else if(!player.isShiftKeyDown())player.displayClientMessage(Component.translatable("message.wayaround.nexus.cutoff_warning"),false);
            else {data.shutDown();NexusPortalManager.refreshLoaded(server.getServer());
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,net.minecraft.sounds.SoundSource.BLOCKS,1.4F,.45F);
                player.displayClientMessage(Component.translatable("message.wayaround.nexus.portal_off"),false);}
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
