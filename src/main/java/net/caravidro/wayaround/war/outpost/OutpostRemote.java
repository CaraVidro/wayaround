package net.caravidro.wayaround.war.outpost;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=WayAround.MODID)
public final class OutpostRemote {
    private record Session(OutpostDroneEntity drone,Vec3 anchor,float health){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Long> INPUTS=new HashMap<>();
    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID());}
    public static boolean controls(ServerPlayer p,OutpostDroneEntity d){var s=SESSIONS.get(p.getUUID());return s!=null&&s.drone==d&&p.level()==d.level()&&p.distanceToSqr(d)<=96*96&&RemoteControllerItem.holds(p)&&d.owned(p);}
    public static boolean controlled(OutpostDroneEntity d){return SESSIONS.values().stream().anyMatch(s->s.drone==d);}
    public static boolean cameraActive(ServerPlayer p){var s=SESSIONS.get(p.getUUID());return s!=null&&!s.drone.impact()&&controls(p,s.drone);}
    public static void start(ServerPlayer p,OutpostDroneEntity d){if(!d.owned(p)||d.launched()||d.battery()<=0||p.isPassenger()||p.isSpectator()||net.caravidro.wayaround.observation.EntitySpectate.holding(p)||!RemoteControllerItem.holds(p)||d.level()!=p.level()||p.distanceToSqr(d)>96*96||!OutpostDroneEntity.enabled(p.level(),d.impact()))return;stop(p);SESSIONS.put(p.getUUID(),new Session(d,p.position(),p.getHealth()));PacketDistributor.sendToPlayer(p,new OutpostViewPayload(d.getId()));}
    public static void stop(ServerPlayer p){if(SESSIONS.remove(p.getUUID())!=null){PacketDistributor.sendToPlayer(p,new OutpostViewPayload(-1));}INPUTS.remove(p.getUUID());}
    public static void input(ServerPlayer p,OutpostControlPayload c){
        long now=p.level().getGameTime();if(now-INPUTS.getOrDefault(p.getUUID(),Long.MIN_VALUE/2)<2)return;INPUTS.put(p.getUUID(),now);
        if(c.action()==3){stop(p);return;}
        var s=SESSIONS.get(p.getUUID());if(s!=null){if(!controls(p,s.drone)){stop(p);return;}if(c.action()==2)s.drone.launch(p);else if(c.action()==0)s.drone.controls(p,c.forward(),c.side(),c.vertical(),c.yaw(),c.pitch());return;}
        if(c.action()==1&&p.getVehicle() instanceof net.minecraft.world.entity.decoration.ArmorStand seat&&seat.getTags().contains(FieldDeviceBlockEntity.SEAT_TAG)){
            var pos=net.minecraft.core.BlockPos.of(seat.getPersistentData().getLong("OutpostAnchor"));if(p.level().hasChunkAt(pos)&&p.serverLevel().getBlockEntity(pos) instanceof FieldDeviceBlockEntity d)d.fire(p);
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){for(var p:e.getServer().getPlayerList().getPlayers()){var s=SESSIONS.get(p.getUUID());if(s==null)continue;if(!p.isAlive()||p.isShiftKeyDown()||!s.drone.isAlive()||s.drone.battery()<=0||!controls(p,s.drone)||!OutpostDroneEntity.enabled(p.level(),s.drone.impact())||p.getHealth()<s.health||p.position().distanceToSqr(s.anchor)>.25)stop(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void change(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void breakBlock(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&active(p)){e.setCanceled(true);stop(p);}}
    @SubscribeEvent public static void clear(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SESSIONS.clear();INPUTS.clear();}
    private OutpostRemote(){}
}
