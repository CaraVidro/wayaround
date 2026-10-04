package net.caravidro.wayaround.war.outpost;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class CanisterItem extends Item {
    private final boolean flare;
    public CanisterItem(boolean flare,Properties p) {
        super(p);
        this.flare=flare;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h) {
        var stack=p.getItemInHand(h);
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.enabled(l,net.caravidro.wayaround.worldconfig.WorldFeature.WAR_WITHOUT_REASON))return InteractionResultHolder.fail(stack);
        if(!l.isClientSide) {
            if(l.getEntitiesOfClass(FieldCanisterEntity.class,p.getBoundingBox().inflate(64)).size()>=16)return InteractionResultHolder.fail(stack);
            var e=OutpostContent.CANISTER.get().create(l);
            if(e==null)return InteractionResultHolder.fail(stack);
            e.setPos(p.getEyePosition().add(p.getLookAngle().scale(.5)));
            e.configure(flare);
            e.setDeltaMovement(p.getLookAngle().scale(.7).add(0,.15,0));
            l.addFreshEntity(e);
            if(!p.getAbilities().instabuild)stack.shrink(1);
            p.getCooldowns().addCooldown(this,15);
        }
        return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
    }
}
