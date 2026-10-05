package net.caravidro.wayaround.worldgen.planet;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Prepare remote safe land asynchronously while the death screen is open; never teleport via the bed. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class LostRespawnService {
    private static final Map<UUID,Search> SEARCHES=new LinkedHashMap<>();
    private static int cursor;
    private static final class Search {
        ServerPlayer original;Vec3 death;BlockPos bed,destination;int attempts;boolean requested,waiting,failed;
        TravelChunks.Batch batch;
    }
    private static boolean enabled(ServerPlayer p){return WorldFeatureRuntime.serverEnabled(WorldFeature.RANDOM_RESPAWN)
            &&p.serverLevel().dimension().equals(Level.OVERWORLD)&&!net.caravidro.wayaround.dream.DreamManager.active(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p&&enabled(p)&&!p.server.isHardcore())start(p);
    }
    private static Search start(ServerPlayer p) {
        Search s=SEARCHES.get(p.getUUID());if(s!=null)return s;
        s=new Search();s.original=p;s.death=p.position();s.bed=p.getRespawnPosition();SEARCHES.put(p.getUUID(),s);return s;
    }
    /** Called only for the player's own explicit respawn request. Stay on the death screen until ready. */
    public static boolean waitForDestination(ServerPlayer p) {
        if(!enabled(p)||p.isAlive()||p.server.isHardcore())return false;
        Search s=start(p);if(s.destination!=null||s.failed)return false;s.requested=true;return true;
    }
    public static void tick(MinecraftServer server) {
        var entries=List.copyOf(SEARCHES.entrySet());int candidates=2;
        if(entries.isEmpty()){cursor=0;return;}
        int count=Math.min(16,entries.size()),begin=Math.floorMod(cursor,entries.size());cursor=begin+count;
        for(int visited=0;visited<count;visited++) {
            var entry=entries.get((begin+visited)%entries.size());
            Search s=entry.getValue();ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey());
            if(p==null||p!=s.original||!WorldFeatureRuntime.serverEnabled(WorldFeature.RANDOM_RESPAWN)) {release(s);SEARCHES.remove(entry.getKey());continue;}
            if(s.destination!=null) {
                if(s.requested) {s.requested=false;p.connection.player=server.getPlayerList().respawn(p,false,net.minecraft.world.entity.Entity.RemovalReason.KILLED);}
                continue;
            }
            if(s.failed) {
                if(s.requested){s.requested=false;p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[WayAround] Não foi possível encontrar terra segura neste mundo; o renascimento normal será usado."));p.connection.player=server.getPlayerList().respawn(p,false,net.minecraft.world.entity.Entity.RemovalReason.KILLED);}
                continue;
            }
            if(s.waiting)continue;
            if(s.attempts>=32){s.failed=true;continue;}
            if(candidates==0)continue;candidates--;
            // Global cap: two candidate rounds (64 climate evaluations) per tick, fair rotation.
            int radius=WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD)?PlanetMath.HALF-256:30000;
            BlockPos candidate=null;
            for(int i=0;i<32;i++) {
                int x=p.serverLevel().random.nextInt(radius*2)-radius,z=p.serverLevel().random.nextInt(radius*2)-radius;
                if(distance(s.death.x,s.death.z,x,z)<2048||s.bed!=null&&distance(s.bed.getX(),s.bed.getZ(),x,z)<2048)continue;
                var c=p.serverLevel().getChunkSource().randomState().sampler().sample(x>>2,16,z>>2);
                if(PlanetMath.height(net.minecraft.world.level.biome.Climate.unquantizeCoord(c.continentalness()),net.minecraft.world.level.biome.Climate.unquantizeCoord(c.erosion()),net.minecraft.world.level.biome.Climate.unquantizeCoord(c.weirdness()))<72)continue;
                candidate=new BlockPos(x,64,z);break;
            }
            if(candidate==null){s.attempts++;continue;}
            BlockPos point=candidate;s.waiting=true;s.attempts++;
            s.batch=TravelChunks.request(p.serverLevel(),point.getX(),point.getZ(),1,ok->{
                s.waiting=false;
                if(ok)s.destination=safeLand(p.serverLevel(),point);
                if(s.destination==null){if(s.batch!=null)s.batch.release();s.batch=null;}
            });
            if(s.batch==null){s.waiting=false;s.attempts--;}
        }
    }
    private static double distance(double ax,double az,double bx,double bz) {
        return WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD)?Math.hypot(PlanetMath.delta(ax,bx),PlanetMath.delta(az,bz)):Math.hypot(ax-bx,az-bz);
    }
    public static BlockPos safeLand(ServerLevel level,BlockPos center) {
        for(int i=0;i<81;i++) {
            int x=center.getX()+(i%9-4)*2,z=center.getZ()+(i/9-4)*2;
            if(!level.hasChunk(x>>4,z>>4))continue;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos p=new BlockPos(x,y,z);var ground=level.getBlockState(p.below());
            if(y<=level.getMinBuildHeight()+5||y>=level.getMaxBuildHeight()-3||!level.getWorldBorder().isWithinBounds(p)
                    ||!ground.getFluidState().isEmpty()||!ground.isCollisionShapeFullBlock(level,p.below())
                    ||ground.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)||ground.is(net.minecraft.world.level.block.Blocks.CACTUS)
                    ||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir())continue;
            return p;
        }
        return null;
    }
    public static DimensionTransition preparedTransition(ServerPlayer player) {
        Search s=SEARCHES.get(player.getUUID());
        return !enabled(player)||player.isAlive()||s==null||s.destination==null?null:new DimensionTransition(player.serverLevel(),Vec3.atBottomCenterOf(s.destination),Vec3.ZERO,player.getYRot(),0,DimensionTransition.DO_NOTHING);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void position(PlayerRespawnPositionEvent e) {
        if(e.isFromEndFight()||!(e.getEntity() instanceof ServerPlayer p)||!enabled(p))return;
        Search s=SEARCHES.get(p.getUUID());if(s==null||s.destination==null)return;
        e.setDimensionTransition(new DimensionTransition(p.serverLevel(),Vec3.atBottomCenterOf(s.destination),Vec3.ZERO,p.getYRot(),0,DimensionTransition.DO_NOTHING));
        e.setCopyOriginalSpawnPosition(true); // Preserve bed for sleeping / restoring the option later.
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {Search s=SEARCHES.remove(e.getEntity().getUUID());if(s!=null)release(s);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {Search s=SEARCHES.remove(e.getEntity().getUUID());if(s!=null)release(s);}
    private static void release(Search s){if(s.batch!=null)s.batch.release();}
    public static void clear(){for(var s:SEARCHES.values())release(s);SEARCHES.clear();cursor=0;}
    private LostRespawnService() {}
}
