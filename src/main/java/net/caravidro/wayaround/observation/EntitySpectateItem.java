package net.caravidro.wayaround.observation;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class EntitySpectateItem extends Item {
    public EntitySpectateItem(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        if(p instanceof net.minecraft.server.level.ServerPlayer server)EntitySpectate.stop(server);
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),l.isClientSide);
    }
}
