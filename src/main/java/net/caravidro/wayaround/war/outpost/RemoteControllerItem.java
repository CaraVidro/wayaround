package net.caravidro.wayaround.war.outpost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
public final class RemoteControllerItem extends Item {
    public RemoteControllerItem(Properties p){super(p);}
    public static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void bindDrone(ItemStack s,OutpostDroneEntity d){var n=new CompoundTag();n.putUUID("Drone",d.getUUID());n.putString("World",d.level().dimension().location().toString());s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
    public static void bindCharge(ItemStack s,BlockPos p,Level l){var n=new CompoundTag();n.putLong("Charge",p.asLong());n.putString("World",l.dimension().location().toString());s.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
    public static boolean holds(Player p){return p.getMainHandItem().is(OutpostContent.CONTROLLER.get())||p.getOffhandItem().is(OutpostContent.CONTROLLER.get());}
    public static boolean boundTo(Player p,OutpostDroneEntity d){for(var stack:java.util.List.of(p.getMainHandItem(),p.getOffhandItem())){if(!stack.is(OutpostContent.CONTROLLER.get()))continue;var n=data(stack);if(n.hasUUID("Drone")&&n.getUUID("Drone").equals(d.getUUID())&&n.getString("World").equals(d.level().dimension().location().toString()))return true;}return false;}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand hand){return InteractionResult.PASS;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(l.isClientSide)return InteractionResultHolder.success(stack);if(!(player instanceof ServerPlayer p))return InteractionResultHolder.pass(stack);
        if(OutpostRemote.active(p)){OutpostRemote.stop(p);return InteractionResultHolder.consume(stack);}
        var n=data(stack);if(!n.getString("World").equals(l.dimension().location().toString()))return InteractionResultHolder.fail(stack);
        if(n.hasUUID("Drone")&&p.serverLevel().getEntity(n.getUUID("Drone")) instanceof OutpostDroneEntity d){OutpostRemote.start(p,d);return InteractionResultHolder.consume(stack);}
        if(n.contains("Charge")&&p.isShiftKeyDown()){var pos=BlockPos.of(n.getLong("Charge"));if(pos.distToCenterSqr(p.position())<=96*96&&l.hasChunkAt(pos)&&l.getBlockEntity(pos) instanceof FieldDeviceBlockEntity d&&d.kind()==FieldDeviceBlock.Kind.CHARGE&&p.getUUID().equals(d.owner()))d.explode(4);return InteractionResultHolder.consume(stack);}
        return InteractionResultHolder.pass(stack);
    }
}
