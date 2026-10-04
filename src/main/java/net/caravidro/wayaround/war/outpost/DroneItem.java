package net.caravidro.wayaround.war.outpost;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
public final class DroneItem extends Item {
    private final boolean impact;
    public DroneItem(boolean impact,Properties p){super(p);this.impact=impact;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        var stack=p.getItemInHand(hand);if(!OutpostDroneEntity.enabled(l,impact))return InteractionResultHolder.fail(stack);
        if(!l.isClientSide){if(l.getEntitiesOfClass(OutpostDroneEntity.class,p.getBoundingBox().inflate(96)).size()>=16)return InteractionResultHolder.fail(stack);var d=OutpostContent.DRONE.get().create(l);if(d==null)return InteractionResultHolder.fail(stack);var at=p.getEyePosition().add(p.getLookAngle().scale(1.2));d.setPos(at);d.configure(p.getUUID(),impact,RemoteControllerItem.data(stack).contains("Battery")?RemoteControllerItem.data(stack).getInt("Battery"):6000);d.setYRot(p.getYRot());d.setXRot(p.getXRot());if(!l.hasChunkAt(d.blockPosition())||!l.noCollision(d,d.getBoundingBox()))return InteractionResultHolder.fail(stack);l.addFreshEntity(d);d.setDeltaMovement(p.getLookAngle().scale(.15));if(!p.getAbilities().instabuild)stack.shrink(1);}
        return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
    }
}
