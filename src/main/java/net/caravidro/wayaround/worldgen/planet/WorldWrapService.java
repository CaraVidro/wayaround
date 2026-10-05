package net.caravidro.wayaround.worldgen.planet;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class WorldWrapService {
    private static final Map<UUID,Passage> PASSAGES=new HashMap<>();
    private static final class Passage {
        Entity root;Vec3 target,velocity,hold;float fall;TravelChunks.Batch batch;boolean ready,failed,crossed;
    }
    @SubscribeEvent public static void entity(EntityTickEvent.Post event) {
        Entity e=event.getEntity();
        if(!(e.level() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD)
                ||!WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD)||e.isPassenger()||e.isRemoved()||e instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
        var passage=PASSAGES.get(e.getUUID());
        boolean outside=PlanetMath.outside(e.getX(),e.getZ());
        if(passage==null) {
            boolean nearby=PlanetMath.HALF-Math.abs(e.getX())<128||PlanetMath.HALF-Math.abs(e.getZ())<128;
            // Only players / occupied vessels prefetch. Projectiles/items request a destination on actual crossing.
            if(!outside&&(!nearby||!(e instanceof ServerPlayer)&&e.getPassengers().stream().noneMatch(p->p instanceof ServerPlayer)))return;
            if(PASSAGES.size()>=16)return;
            passage=new Passage();passage.root=e;
            double x=PlanetMath.wrap(e.getX()),z=PlanetMath.wrap(e.getZ());
            if(!outside){if(PlanetMath.HALF-Math.abs(x)<128)x=x>0?-PlanetMath.HALF+.5:PlanetMath.HALF-.5;if(PlanetMath.HALF-Math.abs(z)<128)z=z>0?-PlanetMath.HALF+.5:PlanetMath.HALF-.5;}
            passage.target=new Vec3(x,e.getY(),z);var p=passage;
            passage.batch=TravelChunks.request(level,(int)Math.floor(x),(int)Math.floor(z),1,ok->{p.ready=ok;p.failed=!ok;});
            if(passage.batch==null)return;PASSAGES.put(e.getUUID(),passage);
        }
        if(passage.failed){passage.batch.release();PASSAGES.remove(e.getUUID());if(outside)hold(e,passage);return;}
        if(!outside&&!passage.crossed)return;
        if(!passage.crossed) {
            passage.crossed=true;passage.target=new Vec3(PlanetMath.wrap(e.getX()),e.getY(),PlanetMath.wrap(e.getZ()));
            passage.velocity=e.getDeltaMovement();passage.fall=e.fallDistance;
            passage.hold=new Vec3(Math.max(-PlanetMath.HALF+.1,Math.min(PlanetMath.HALF-.1,e.getX())),e.getY(),Math.max(-PlanetMath.HALF+.1,Math.min(PlanetMath.HALF-.1,e.getZ())));
        }
        // A fast diagonal overshoot can leave the prefetched destination; wait for the exact destination too.
        if(passage.ready&&!level.hasChunkAt(net.minecraft.core.BlockPos.containing(passage.target))) {
            passage.batch.release();passage.ready=false;var p=passage;
            passage.batch=TravelChunks.request(level,(int)Math.floor(p.target.x),(int)Math.floor(p.target.z),1,ok->{p.ready=ok;p.failed=!ok;});
            if(passage.batch==null){PASSAGES.remove(e.getUUID());hold(e,passage);return;}
        }
        if(passage.ready) {
            moveTree(e,passage.target.subtract(e.position()));e.setDeltaMovement(passage.velocity);e.fallDistance=passage.fall;
            passage.batch.release();PASSAGES.remove(e.getUUID());
        } else hold(e,passage);
    }
    private static void hold(Entity e,Passage passage) {
        Vec3 hold=passage.hold;
        if(hold==null)hold=new Vec3(Math.max(-PlanetMath.HALF+.1,Math.min(PlanetMath.HALF-.1,e.getX())),e.getY(),Math.max(-PlanetMath.HALF+.1,Math.min(PlanetMath.HALF-.1,e.getZ())));
        moveTree(e,hold.subtract(e.position()));e.setDeltaMovement(Vec3.ZERO);
    }
    /** Same dimension, same entity UUID/inventory. Keep the passenger tree instead of recreating a vessel. */
    public static void moveTree(Entity root,Vec3 offset) {
        var passengers=new LinkedHashMap<Entity,Vec3>();root.getIndirectPassengers().forEach(p->passengers.put(p,p.position()));
        root.teleportTo(root.getX()+offset.x,root.getY()+offset.y,root.getZ()+offset.z);
        for(var entry:passengers.entrySet()) {
            Entity p=entry.getKey();Vec3 to=entry.getValue().add(offset);
            if(p instanceof ServerPlayer player)player.connection.teleport(to.x,to.y,to.z,p.getYRot(),p.getXRot());
            else p.teleportTo(to.x,to.y,to.z);
        }
        if(root instanceof ServerPlayer player)player.connection.teleport(root.getX(),root.getY(),root.getZ(),root.getYRot(),root.getXRot());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        TravelChunks.tick();
        for(var entry:List.copyOf(PASSAGES.entrySet())) {
            var p=entry.getValue();if(p.root.isRemoved()||p.root.level()!=p.batch.level||!WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD)
                    ||!p.crossed&&PlanetMath.HALF-Math.abs(p.root.getX())>160&&PlanetMath.HALF-Math.abs(p.root.getZ())>160) {
                p.batch.release();PASSAGES.remove(entry.getKey());
            }
        }
        LostRespawnService.tick(e.getServer());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){PASSAGES.clear();TravelChunks.clear();LostRespawnService.clear();}
    private WorldWrapService() {}
}
