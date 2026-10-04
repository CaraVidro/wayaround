package net.caravidro.wayaround.observation;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.EntitySpectateStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=WayAround.MODID)
public final class EntitySpectate {
    private record Visit(Entity target,net.minecraft.server.level.ServerLevel level,Vec3 origin,float yaw,float pitch,boolean gravity,boolean physics,boolean invulnerable){}
    private static final Map<UUID,Visit> VISITS=new HashMap<>();
    private record Ghost(boolean invisible,boolean gravity,boolean physics,boolean invulnerable,boolean mayfly,boolean flying){}
    private static final Map<UUID,Ghost> HIDDEN=new HashMap<>();
    public static boolean ghost(Entity e){return e instanceof net.minecraft.world.entity.player.Player p&&holding(p);}
    public static void protect(ServerPlayer p){
        if(!HIDDEN.containsKey(p.getUUID())){
            var a=p.getAbilities();var g=new Ghost(p.isInvisible()&&!p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),p.isNoGravity(),p.noPhysics,p.isInvulnerable(),a.mayfly,a.flying);HIDDEN.put(p.getUUID(),g);
            var n=new net.minecraft.nbt.CompoundTag();n.putBoolean("Invisible",g.invisible());n.putBoolean("Gravity",g.gravity());n.putBoolean("Physics",g.physics());n.putBoolean("Invulnerable",g.invulnerable());n.putBoolean("Mayfly",g.mayfly());n.putBoolean("Flying",g.flying());p.getPersistentData().put("WayAroundSpectateGhost",n);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
        }
        p.setInvisible(true);p.noPhysics=true;p.setNoGravity(true);p.setInvulnerable(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.setAirSupply(p.getMaxAirSupply());
    }
    private static void restore(ServerPlayer p,Ghost g){
        p.setInvisible(g.invisible()||p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));p.noPhysics=g.physics();p.setNoGravity(g.gravity());p.setInvulnerable(g.invulnerable());p.getAbilities().mayfly=g.mayfly();p.getAbilities().flying=g.flying();p.fallDistance=0;p.onUpdateAbilities();p.getPersistentData().remove("WayAroundSpectateGhost");
    }
    public static Vec3 viewpoint(ServerPlayer p,Entity target){
        double radius=Math.max(3,Math.min(16,target.getBbWidth()*2+2));Vec3 center=target.position().add(0,target.getBbHeight()*.5,0);Vec3 desired=center.subtract(p.getLookAngle().scale(radius));
        var hit=target.level().clip(new net.minecraft.world.level.ClipContext(center,desired,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?desired:hit.getLocation().add(center.subtract(desired).normalize().scale(.3));
    }
    public static boolean holding(net.minecraft.world.entity.player.Player p){return p.getMainHandItem().is(WayAroundContent.ENTITY_SPECTATE.get())||p.getOffhandItem().is(WayAroundContent.ENTITY_SPECTATE.get());}
    public static boolean active(ServerPlayer p){return VISITS.containsKey(p.getUUID());}
    public static void start(ServerPlayer p,Entity target){
        if(p.getPersistentData().getLong("WayAroundSpectateStopUntil")>p.level().getGameTime())return;
        if(active(p)){stop(p);return;}
        if(!holding(p)||target==p||!target.isAlive()||target.level()!=p.level()||p.distanceToSqr(target)>36||!p.hasLineOfSight(target)||p.isPassenger())return;
        protect(p);
        VISITS.put(p.getUUID(),new Visit(target,p.serverLevel(),p.position(),p.getYRot(),p.getXRot(),p.isNoGravity(),p.noPhysics,p.isInvulnerable()));
        var saved=new net.minecraft.nbt.CompoundTag();saved.putString("Dimension",p.level().dimension().location().toString());saved.putDouble("X",p.getX());saved.putDouble("Y",p.getY());saved.putDouble("Z",p.getZ());saved.putFloat("Yaw",p.getYRot());saved.putFloat("Pitch",p.getXRot());saved.putBoolean("Gravity",p.isNoGravity());saved.putBoolean("Invulnerable",p.isInvulnerable());p.getPersistentData().put("WayAroundSpectateVisit",saved);
        // Keep vanilla camera ownership on the player: the client only orbits the target visually.
        p.setCamera(p);
        PacketDistributor.sendToPlayer(p,new EntitySpectateStatePayload(target.getId()));
    }
    public static void stop(ServerPlayer p){
        var visit=VISITS.remove(p.getUUID());if(visit==null)return;
        p.getPersistentData().remove("WayAroundSpectateVisit");p.getPersistentData().putLong("WayAroundSpectateStopUntil",p.level().getGameTime()+5);
        Vec3 exit=visit.target().level()==p.level()?viewpoint(p,visit.target()).add(0,-p.getEyeHeight(),0):p.position();
        p.setCamera(p);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        if(p.isAlive())p.teleportTo(p.serverLevel(),exit.x,exit.y,exit.z,Set.of(),p.getYRot(),p.getXRot());
        // Leaving the camera does not restore the physical body while the item is still held.
        if(holding(p))protect(p);
        PacketDistributor.sendToPlayer(p,new EntitySpectateStatePayload(-1));
    }
    public static void release(ServerPlayer p){stop(p);var old=HIDDEN.remove(p.getUUID());if(old!=null)restore(p,old);}
    @SubscribeEvent public static void specific(PlayerInteractEvent.EntityInteractSpecific e){if(holding(e.getEntity())){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getEntity() instanceof ServerPlayer p)start(p,e.getTarget());}}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){if(holding(e.getEntity())){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getEntity() instanceof ServerPlayer p)start(p,e.getTarget());}}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){for(var p:e.getServer().getPlayerList().getPlayers()){
        if(holding(p)&&p.isAlive())protect(p);
        else release(p);
        var v=VISITS.get(p.getUUID());if(v==null)continue;
        if(p.isShiftKeyDown()){stop(p);continue;}
        if(!v.target().isAlive()||v.target().level()!=v.level()||p.level()!=v.level()){stop(p);continue;}
        p.setAirSupply(p.getMaxAirSupply());
        p.setDeltaMovement(Vec3.ZERO);p.setPos(v.target().getX(),v.target().getY(),v.target().getZ());
    }}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){release(p);}}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(holding(e.getEntity())){e.setCanceled(true);if(e.getEntity() instanceof ServerPlayer p)start(p,e.getTarget());}}
    @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock e){if(holding(e.getEntity())){e.setCanceled(true);if(e.getEntity() instanceof ServerPlayer p)stop(p);}}
    @SubscribeEvent public static void breakBlock(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){if(holding(e.getPlayer()))e.setCanceled(true);}
    @SubscribeEvent public static void pickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e){if(e.getPlayer() instanceof ServerPlayer p&&holding(p))e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);}
    @SubscribeEvent public static void clear(net.neoforged.neoforge.event.server.ServerStoppedEvent e){VISITS.clear();HIDDEN.clear();}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(!(e.getEntity() instanceof ServerPlayer p))return;var data=p.getPersistentData();
        if(data.contains("WayAroundSpectateVisit")){var saved=data.getCompound("WayAroundSpectateVisit");p.setNoGravity(saved.getBoolean("Gravity"));p.setInvulnerable(saved.getBoolean("Invulnerable"));p.noPhysics=false;p.setCamera(p);
            var id=net.minecraft.resources.ResourceLocation.tryParse(saved.getString("Dimension"));if(id!=null){var l=p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));if(l!=null)p.teleportTo(l,saved.getDouble("X"),saved.getDouble("Y"),saved.getDouble("Z"),Set.of(),saved.getFloat("Yaw"),saved.getFloat("Pitch"));}data.remove("WayAroundSpectateVisit");
        }
        if(data.contains("WayAroundSpectateGhost")){var n=data.getCompound("WayAroundSpectateGhost");restore(p,new Ghost(n.getBoolean("Invisible"),n.getBoolean("Gravity"),n.getBoolean("Physics"),n.getBoolean("Invulnerable"),n.getBoolean("Mayfly"),n.getBoolean("Flying")));}
        if(data.contains("WayAroundSpectateWasInvisible")){p.setInvisible(data.getBoolean("WayAroundSpectateWasInvisible")||p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));data.remove("WayAroundSpectateWasInvisible");}
    }
}
