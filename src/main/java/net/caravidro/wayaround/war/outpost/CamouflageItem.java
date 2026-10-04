package net.caravidro.wayaround.war.outpost;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
public final class CamouflageItem extends Item {
    public CamouflageItem(Properties p) {
        super(p);
    }
    @Override public InteractionResult useOn(UseOnContext c) {
        if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof FieldDeviceBlockEntity d)||!d.enabled())return InteractionResult.PASS;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(d.camouflage()) {
            if(c.getPlayer()==null||!c.getPlayer().getAbilities().instabuild)c.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
