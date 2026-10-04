package net.caravidro.wayaround.security;

import java.security.SecureRandom;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server authority is a bounded voxel calculation, never a dedicated-server renderer. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class VisibilityAuditService {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final Map<UUID,Visit> VISITS=new HashMap<>();
    private record ChunkKey(ServerLevel level,long chunk) {}
    private static final LinkedHashMap<ChunkKey,Long> CHANGED=new LinkedHashMap<>();
    private static final ArrayDeque<Job> JOBS=new ArrayDeque<>();
    private record Job(ServerPlayer player,VisibilityReportPayload report,Visit visit) {}
    private static final class Visit {
        long next,nonce,sent,lastPositive;
        int streak;
        boolean pending,notified;
        ServerLevel level;
        Vec3 eye,forward;
    }
    private static final Set<Block> WALLS=Set.of(Blocks.STONE,Blocks.DEEPSLATE,Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.SAND,Blocks.GRAVEL,
            Blocks.ANDESITE,Blocks.DIORITE,Blocks.GRANITE,Blocks.TUFF,Blocks.NETHERRACK,Blocks.BLACKSTONE,Blocks.BASALT,Blocks.END_STONE,Blocks.RED_SAND,Blocks.CLAY);
    private static int cursor;
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {var v=new Visit();v.next=System.nanoTime()+15_000_000_000L;VISITS.put(player.getUUID(),v);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){VISITS.remove(event.getEntity().getUUID());JOBS.removeIf(job->job.player.getUUID().equals(event.getEntity().getUUID()));}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){VISITS.clear();CHANGED.clear();JOBS.clear();cursor=0;}

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();
        if(!AntiXrayService.enabled(server)||!AntiXrayConfig.VISUAL.get()){VISITS.clear();JOBS.clear();return;}
        long now=System.nanoTime();
        // At most ONE 32-ray job (<=2048 cells) per server tick. Queue never exceeds eight frames.
        Job job=JOBS.pollFirst();if(job!=null)process(job,now);
        List<ServerPlayer> players=server.getPlayerList().getPlayers();int size=players.size();if(size==0)return;
        int issued=0;
        for(int i=0;i<Math.min(16,size);i++) {
            ServerPlayer player=players.get(Math.floorMod(cursor++,size));Visit v=VISITS.computeIfAbsent(player.getUUID(),id->{var visit=new Visit();visit.next=now+15_000_000_000L;return visit;});
            if(v.pending&&now-v.sent>2_000_000_000L){v.pending=false;v.streak=0;}
            if(now<v.next||v.pending||issued>=2)continue;
            v.next=now+15_000_000_000L;
            if(!eligible(player)||recentlyChanged(player))continue;
            var saved=AntiXrayData.get(server).find(player.getUUID());
            boolean suspected=saved!=null&&saved.history.evidenceSeconds>0;
            boolean underground=!player.level().canSeeSky(player.blockPosition())&&player.getY()<player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,player.getBlockX(),player.getBlockZ())-4;
            var home=net.caravidro.wayaround.dream.PlayerBehaviorManager.knownHome(player);
            boolean atHome=home!=null&&home.confidence()>=.4&&home.dimension().equals(player.level().dimension().location().toString())
                    &&(home.bed()!=null||home.containers().size()>=2)&&Math.abs(player.getY()-home.preferredY())<=12
                    &&Math.abs(player.chunkPosition().x-home.center().x)<=2&&Math.abs(player.chunkPosition().z-home.center().z)<=2;
            v.next=now+VisibilityMath.delaySeconds(underground,atHome,suspected||v.streak>0,RANDOM.nextInt())*1_000_000_000L;
            v.nonce=RANDOM.nextLong();v.sent=now;v.pending=true;v.eye=player.getEyePosition();v.forward=player.getLookAngle();v.level=player.serverLevel();
            PacketDistributor.sendToPlayer(player,new VisibilityChallengePayload(v.nonce));issued++;
        }
    }
    private static boolean eligible(ServerPlayer p) {
        return p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&!p.isPassenger()&&!p.isSleeping()&&!p.isUnderWater()
                &&p.tickCount>=120&&p.getCamera()==p&&p.level().dimension().equals(Level.OVERWORLD)
                &&!net.caravidro.wayaround.observation.EntitySpectate.holding(p)&&!net.caravidro.wayaround.observation.EntitySpectate.active(p)
                &&!net.caravidro.wayaround.dream.DreamManager.active(p);
    }
    public static boolean valid(VisibilityReportPayload p) {
        if(!p.available())return true;
        if(!Double.isFinite(p.x()+p.y()+p.z())||!VisibilityMath.finiteDirection(p.fx(),p.fy(),p.fz()))return false;
        for(int i=0;i<VisibilityMath.SAMPLES;i++) {
            float x=p.directions()[i*3],y=p.directions()[i*3+1],z=p.directions()[i*3+2],d=p.distances()[i];
            if(!VisibilityMath.finiteDirection(x,y,z)||!Float.isFinite(d)||d<0||d>65536
                    ||x*p.fx()+y*p.fy()+z*p.fz()<.1)return false;
        }
        return true;
    }
    public static void receive(ServerPlayer player,VisibilityReportPayload report) {
        if(!AntiXrayService.enabled(player.server)||!AntiXrayConfig.VISUAL.get())return;
        Visit v=VISITS.get(player.getUUID());long now=System.nanoTime();if(v==null)return;
        if(!AntiXrayService.accepts(v.pending,v.nonce,report.nonce(),now-v.sent,valid(report))||now-v.sent>2_000_000_000L)return;
        v.pending=false;
        if(!report.available()||JOBS.size()>=8){v.streak=0;return;}
        JOBS.addLast(new Job(player,report,v));
    }
    private static void process(Job job,long now) {
        ServerPlayer player=job.player;Visit v=job.visit;VisibilityReportPayload p=job.report;
        if(VISITS.get(player.getUUID())!=v||player.serverLevel()!=v.level||now-v.sent>2_000_000_000L||!eligible(player)||recentlyChanged(player)){v.streak=0;return;}
        Vec3 eye=new Vec3(p.x(),p.y(),p.z()),forward=new Vec3(p.fx(),p.fy(),p.fz());
        if(eye.distanceToSqr(v.eye)>.36||eye.distanceToSqr(player.getEyePosition())>.36
                ||forward.dot(v.forward)<.996||forward.dot(player.getLookAngle())<.996){v.streak=0;return;}
        int covered=0,missing=0,ores=0,unknown=0;Set<BlockPos> distinct=new HashSet<>();
        for(int i=0;i<VisibilityMath.SAMPLES;i++) {
            var ray=VisibilityMath.trace(p.x(),p.y(),p.z(),p.directions()[i*3],p.directions()[i*3+1],p.directions()[i*3+2],p.distances()[i],Byte.toUnsignedInt(p.brightness()[i]),
                    (x,y,z)->material(player.serverLevel(),new BlockPos(x,y,z)));
            if(!ray.known()){unknown++;continue;}
            if(ray.covered())covered++;if(ray.missingWall())missing++;
            if(ray.hiddenOre()){ores++;distinct.add(new BlockPos(ray.x(),ray.y(),ray.z()));}
        }
        boolean strong=VisibilityMath.strong(covered,missing,ores,distinct.size());
        String summary="covered="+covered+"/32 missing="+missing+" oreBehind="+ores+" distinctOres="+distinct.size()+" unknown="+unknown;
        var data=AntiXrayData.get(player.server);var c=data.get(player.getUUID());c.name=player.getGameProfile().getName();c.visualFrames=Math.min(1000000,c.visualFrames+1);c.lastVisual=summary;data.setDirty();
        if(!strong){v.streak=0;v.lastPositive=0;if(covered>=12)v.notified=false;return;}
        if(v.lastPositive!=0&&now-v.lastPositive>30_000_000_000L)v.streak=0;
        v.streak++;v.lastPositive=now;c.visualContradictions=Math.min(1000000,c.visualContradictions+1);
        v.next=Math.min(v.next,now+VisibilityMath.delaySeconds(false,false,true,RANDOM.nextInt())*1_000_000_000L);
        if(!v.notified){v.notified=true;AntiXrayService.visualAudit(player,"VISUAL_SUSPICION "+summary+"; nenhuma imagem armazenada; aguardando repetição");}
        // A single screenshot, absent telemetry or a dark/empty frame can never convict.
        if(v.streak>=3)AntiXrayService.visualConfirmed(player,5,summary,now);
    }
    static int material(ServerLevel level,BlockPos pos) {
        if(!level.isInWorldBounds(pos)||!level.hasChunkAt(pos))return VisibilityMath.UNKNOWN;
        var state=level.getBlockState(pos);
        if(!state.getFluidState().isEmpty()||state.hasBlockEntity())return VisibilityMath.UNKNOWN;
        if(state.is(BlockTags.DIAMOND_ORES)||state.is(BlockTags.IRON_ORES)||state.is(BlockTags.GOLD_ORES)
                ||state.is(BlockTags.REDSTONE_ORES)||state.is(BlockTags.EMERALD_ORES)||state.is(BlockTags.LAPIS_ORES)||state.is(BlockTags.COPPER_ORES)||state.is(BlockTags.COAL_ORES))return VisibilityMath.ORE;
        if(WALLS.contains(state.getBlock()))return state.isSolidRender(level,pos)&&state.isCollisionShapeFullBlock(level,pos)?VisibilityMath.WALL:VisibilityMath.UNKNOWN;
        return state.isAir()||!state.blocksMotion()?VisibilityMath.OPEN:VisibilityMath.UNKNOWN;
    }
    private static void changed(ServerLevel level,BlockPos position) {
        if(CHANGED.size()>=2048)CHANGED.remove(CHANGED.keySet().iterator().next());
        CHANGED.put(new ChunkKey(level,net.minecraft.world.level.ChunkPos.asLong(position.getX()>>4,position.getZ()>>4)),level.getGameTime());
    }
    private static boolean recentlyChanged(ServerPlayer p) {
        var chunk=p.chunkPosition();long now=p.level().getGameTime();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++) {
            Long tick=CHANGED.get(new ChunkKey(p.serverLevel(),net.minecraft.world.level.ChunkPos.asLong(chunk.x+x,chunk.z+z)));
            if(tick!=null&&now-tick<80)return true;
        }
        return false;
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel level)changed(level,e.getPos());}
    @SubscribeEvent public static void placing(BlockEvent.EntityPlaceEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel level)changed(level,e.getPos());}
    @SubscribeEvent public static void loaded(ChunkEvent.Load e){if(e.getLevel() instanceof ServerLevel level)changed(level,e.getChunk().getPos().getWorldPosition());}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){if(e.getLevel() instanceof ServerLevel level)for(BlockPos pos:e.getAffectedBlocks().stream().limit(64).toList())changed(level,pos);}
    private VisibilityAuditService() {}
}
