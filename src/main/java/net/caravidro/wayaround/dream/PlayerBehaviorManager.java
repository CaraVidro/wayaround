package net.caravidro.wayaround.dream;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class PlayerBehaviorManager {
    private record Sample(String dimension,Vec3 pos) {}
    private record Interaction(BlockPos pos,long tick,String kind) {}
    private static final Map<UUID,Sample> last=new HashMap<>();
    private static final Map<UUID,Interaction> interactions=new HashMap<>();
    private static final Map<UUID,HomeDetector.HomeRegion> homes=new HashMap<>();
    private static boolean observe(ServerPlayer p) { return !DreamManager.active(p)&&!p.isSpectator()&&!p.level().dimension().equals(DreamContent.DIMENSION); }
    public static PlayerBehaviorProfile profile(ServerPlayer p) { return BehaviorData.get(p.server).profile(p.getUUID()); }
    /** Read-only cached estimate; performance-sensitive systems must not rerun the quadratic detector. */
    public static HomeDetector.HomeRegion knownHome(ServerPlayer p) { return homes.get(p.getUUID()); }
    public static HomeDetector.HomeRegion home(ServerPlayer p) {
        return homes.computeIfAbsent(p.getUUID(),id->HomeDetector.detect(profile(p)));
    }
    @SubscribeEvent public static void sample(ServerTickEvent.Post event) {
        if(event.getServer().getTickCount()%20!=0) return;
        for(ServerPlayer p:event.getServer().getPlayerList().getPlayers()) {
            if(!observe(p)) { last.remove(p.getUUID());continue; }
            String dimension=p.level().dimension().location().toString();
            Sample old=last.put(p.getUUID(),new Sample(dimension,p.position()));
            double moved=old==null||!old.dimension().equals(dimension)?0:old.pos().distanceTo(p.position());
            int surface=p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p.getBlockX(),p.getBlockZ());
            profile(p).sample(dimension,p.blockPosition(),moved,p.isSprinting(),p.getY()<surface-4&&!p.level().canSeeSky(p.blockPosition()));
            BehaviorData.get(p.server).setDirty();
            if(event.getServer().getTickCount()%2400==0) homes.put(p.getUUID(),HomeDetector.detect(profile(p)));
        }
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if(event.getEntity() instanceof ServerPlayer p&&observe(p)) { profile(p).jumps++;BehaviorData.get(p.server).setDirty(); }
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||!observe(p)) return;
        Block block=p.level().getBlockState(event.getPos()).getBlock();
        boolean machine=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(WayAround.MODID)
                &&p.level().getBlockEntity(event.getPos())!=null;
        String kind=machine?"machine":block instanceof CraftingTableBlock?"craft":block instanceof AbstractFurnaceBlock?"furnace":"container";
        if(machine){
            profile(p).interact(p.level().dimension().location().toString(),event.getPos(),kind);
            BehaviorData.get(p.server).setDirty();interactions.remove(p.getUUID());return;
        }
        interactions.put(p.getUUID(),new Interaction(event.getPos().immutable(),p.level().getGameTime(),kind));
    }
    @SubscribeEvent public static void container(PlayerContainerEvent.Open event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||!observe(p)) return;
        Interaction i=interactions.remove(p.getUUID());
        if(i!=null&&p.level().getGameTime()-i.tick()<5) {
            profile(p).interact(p.level().dimension().location().toString(),i.pos(),i.kind());
            BehaviorData.get(p.server).setDirty();
        }
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if(event.getEntity() instanceof ServerPlayer p&&observe(p)) {
            profile(p).activity(p.level().dimension().location().toString(),event.getPos()).placed++;
            BehaviorData.get(p.server).setDirty();
        }
    }
    @SubscribeEvent public static void slept(PlayerWakeUpEvent event) {
        if(event.getEntity() instanceof ServerPlayer p&&observe(p)&&p.getSleepingPos().isPresent()) {
            profile(p).interact(p.level().dimension().location().toString(),p.getSleepingPos().get(),"bed");
            homes.put(p.getUUID(),HomeDetector.detect(profile(p)));BehaviorData.get(p.server).setDirty();
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id=event.getEntity().getUUID();last.remove(id);interactions.remove(id);homes.remove(id);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { last.clear();interactions.clear();homes.clear(); }
}
