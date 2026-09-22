package net.caravidro.wayaround.dream;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class DreamEvents {
    private static boolean dream(Level level){return level.dimension().equals(DreamContent.DIMENSION);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){DreamManager.tick(e.getServer());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p)DreamManager.recover(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.active(p))try{DreamManager.stop(p);}
        catch(Exception error){WayAround.LOGGER.error("Dream logout rollback retained",error);}
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){DreamManager.stopping(e.getServer());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){DreamManager.clear();}
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent e){
        if(dream(e.getEntity().level())||e.getEntity() instanceof ServerPlayer p&&DreamManager.active(p)){
            e.setCanceled(true);
            if(e.getEntity() instanceof ServerPlayer player){
                if(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)||player.fallDistance>18)DreamManager.die(player);
                else if(e.getSource().getEntity()!=null&&!DreamManager.frozen(player)){
                    Vec3 delta=player.position().subtract(e.getSource().getEntity().position()).multiply(1,0,1).normalize().scale(.25);
                    player.setDeltaMovement(delta.x,.15,delta.z);player.hurtMarked=true;
                }
            }
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.active(p)){
            e.setCanceled(true);p.setHealth(Math.max(1,p.getMaxHealth()));DreamManager.die(p);
        }else if(dream(e.getEntity().level())){e.setCanceled(true);e.getEntity().setHealth(e.getEntity().getMaxHealth());}
    }
    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent e){
        if(DreamManager.internal(e.getEntity().getUUID()))return;
        if(dream(e.getEntity().level())||e.getDimension().equals(DreamContent.DIMENSION)
                ||e.getEntity() instanceof ServerPlayer p&&DreamManager.active(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void spawn(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide&&dream(e.getLevel())&&e.getEntity() instanceof net.minecraft.world.entity.LivingEntity
                &&!(e.getEntity() instanceof Player)&&!(e.getEntity() instanceof DreamPlayerEntity))e.setCanceled(true);
    }
    @SubscribeEvent public static void command(CommandEvent e){
        var source=e.getParseResults().getContext().getSource();
        if(!dream(source.getLevel())&&!(source.getEntity() instanceof ServerPlayer p&&DreamManager.active(p)))return;
        String text=e.getParseResults().getReader().getString().replaceFirst("^/","");
        // The prototype's only permitted commands while inside the sandbox are its own admin controls.
        if(!text.matches("wayaround dream (stop|info|die|testplayer) .+"))e.setCanceled(true);
    }
    @SubscribeEvent public static void blockClick(PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void itemUse(PlayerInteractEvent.RightClickItem e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void entityUse(PlayerInteractEvent.EntityInteract e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e){
        if(e.getPlayer() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void toss(ItemTossEvent e){
        if(e.getPlayer() instanceof ServerPlayer p&&DreamManager.active(p)&&!dream(p.level()))e.setCanceled(true);
    }
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Pre e){
        if(e.getPlayer() instanceof ServerPlayer p&&DreamManager.frozen(p))e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
    }
    @SubscribeEvent public static void sleep(CanPlayerSleepEvent e){
        if(DreamManager.active(e.getEntity())&&!DreamManager.internal(e.getEntity().getUUID()))
            e.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
    }
}
