package net.caravidro.wayaround.dream;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DreamManager {
    private static final Map<UUID,DreamSession> SESSIONS=new LinkedHashMap<>();
    private static final Set<UUID> INTERNAL=new HashSet<>();
    private DreamManager(){}
    public static DreamSession session(ServerPlayer player){return SESSIONS.get(player.getUUID());}
    public static boolean active(ServerPlayer player){
        DreamSession s=session(player);return s!=null&&s.state!=DreamState.CLEANUP;
    }
    public static boolean internal(UUID id){return INTERNAL.contains(id);}
    public static boolean frozen(ServerPlayer player){
        DreamSession s=session(player);return s!=null&&s.state!=DreamState.NORMAL&&s.state!=DreamState.CLEANUP;
    }
    public static void start(ServerPlayer player) throws Exception {
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.DREAMS)) throw new IllegalStateException("Dreams are disabled in this world.");
        if(SESSIONS.containsKey(player.getUUID()))throw new IllegalStateException("Sessão ativa ou limpeza ainda em andamento.");
        if(player.getPersistentData().getBoolean(DreamJournal.MARKER))throw new IllegalStateException("Restauração pendente; reconecte antes de iniciar outro teste.");
        if(player.level().dimension().equals(DreamContent.DIMENSION))throw new IllegalStateException("Jogador já está na dimensão de teste.");
        if(SESSIONS.size()>=DreamConfig.MAX_SESSIONS.get())throw new IllegalStateException("Limite de sessões simultâneas atingido.");
        ServerLevel dream=player.server.getLevel(DreamContent.DIMENSION);
        if(dream==null)throw new IllegalStateException("Dimensão wayaround:dream não carregada. Reinicie o mundo.");
        HomeDetector.HomeRegion home=PlayerBehaviorManager.home(player);
        if(home!=null&&home.confidence()<.25)home=null;
        BlockPos bed=home==null?null:home.bed();
        String dimension=home==null?(player.getRespawnPosition()!=null?player.getRespawnDimension():player.level().dimension()).location().toString():home.dimension();
        if(bed==null&&player.getRespawnPosition()!=null&&player.getRespawnDimension().location().toString().equals(dimension))
            bed=player.getRespawnPosition();
        ServerLevel source=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(dimension)));
        if(source==null)throw new IllegalStateException("Dimensão da base não disponível.");
        ChunkPos center=home!=null?home.center():new ChunkPos(bed==null?player.blockPosition():bed);
        if(home==null)home=new HomeDetector.HomeRegion(dimension,center,List.of(center),0,bed,List.of(),List.of(),player.getBlockY());
        BaseSemanticMap semantics=new BaseSemanticMap(PlayerBehaviorManager.profile(player),home);
        player.closeContainer();
        if(player.isSleeping())player.stopSleepInBed(true,true);
        player.stopRiding();
        CompoundTag backup=DreamJournal.capture(player);
        int slot=0;while(inUse(slot))slot++;
        DreamRegion region=new DreamRegion(source,dream,center,slot,bed);
        DreamSession session=new DreamSession(player.getUUID(),slot,backup,region,player.position(),semantics);
        dream.getEntities((net.minecraft.world.entity.Entity)null,region.bounds().inflate(4),e->!(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard);
        player.getPersistentData().putBoolean(DreamJournal.MARKER,true);
        SESSIONS.put(player.getUUID(),session);
        // Also persist the marker before any temporary inventory can be saved.
        player.server.getPlayerList().saveAll();
        send(player,DreamState.PREPARING,20);
    }
    private static boolean inUse(int slot){return SESSIONS.values().stream().anyMatch(s->s.slot==slot);}
    private static void send(ServerPlayer player,DreamState state,int ticks){
        if(player.connection!=null)PacketDistributor.sendToPlayer(player,new DreamPayload(state,ticks));
    }
    private static void stage(ServerPlayer player,DreamSession s,DreamState state,int ticks){
        s.state=state;s.age=0;s.frozen=player.position();send(player,state,ticks);
    }
    public static void tick(MinecraftServer server){
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.DREAMS)) return;
        int budget=DreamConfig.COPY_BUDGET.get()/Math.max(1,SESSIONS.size());
        for(DreamSession s:new ArrayList<>(SESSIONS.values())){
            ServerPlayer player=server.getPlayerList().getPlayer(s.player);
            try {
                if(s.state==DreamState.CLEANUP){
                    if(s.region.step(budget))SESSIONS.remove(s.player);
                    continue;
                }
                if(player==null)continue;
                s.age++;
                if(frozen(player)){
                    player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
                    if(player.position().distanceToSqr(s.frozen)>.001)
                        player.connection.teleport(s.frozen.x,s.frozen.y,s.frozen.z,player.getYRot(),player.getXRot());
                }
                switch(s.state){
                    case PREPARING -> {
                        if(s.region.step(budget)){
                            if(s.region.copiedBed==null)throw new IllegalStateException("Nenhuma cama encontrada nos 5x5 chunks da base provável.");
                            // Preserve source sky time for the initial convincing copy.
                            s.region.target.setDayTime(s.region.source.getDayTime());
                            Vec3 wake=safeBeside(s.region.target,s.region.copiedBed);
                            teleport(player,s.region.target,wake,player.getYRot(),player.getXRot());
                            stage(player,s,DreamState.FALSE_AWAKENING,20);
                            spawnActor(player);
                        }
                    }
                    case FALSE_AWAKENING -> {if(s.age>=20)stage(player,s,DreamState.NORMAL,0);}
                    case NORMAL -> {
                        s.director.tick(player,s);
                        if(!player.level().dimension().equals(DreamContent.DIMENSION)){stop(player);break;}
                        if(!s.region.contains(player.getX(),player.getZ())||player.getY()<s.region.target.getMinBuildHeight()+1||player.fallDistance>18)
                        {s.director.onBoundaryAttempt();die(player);}
                    }
                    case DREAM_DEATH -> {if(s.age>=8)stage(player,s,DreamState.FAKE_MENU,DreamConfig.FAKE_MENU_TICKS.get());}
                    case FAKE_MENU -> {
                        if(s.age>=DreamConfig.FAKE_MENU_TICKS.get()){
                            teleport(player,s.region.target,safeBeside(s.region.target,s.region.copiedBed),player.getYRot(),0);
                            stage(player,s,DreamState.REAL_AWAKENING,10);
                        }
                    }
                    case REAL_AWAKENING -> {if(s.age>=10)stop(player);}
                    default -> {}
                }
            }catch(Exception error){
                WayAround.LOGGER.error("Dream session failed for {}",s.player,error);
                if(player!=null)try{stop(player);}catch(Exception restoreError){WayAround.LOGGER.error("Dream rollback retained on disk",restoreError);}
            }
        }
    }
    public static void die(ServerPlayer player){
        DreamSession s=session(player);
        if(s!=null&&(s.state==DreamState.NORMAL||s.state==DreamState.FALSE_AWAKENING))
            stage(player,s,DreamState.DREAM_DEATH,8);
    }
    public static void spawnActor(ServerPlayer player){
        DreamSession s=session(player);
        if(s==null||s.state==DreamState.PREPARING||s.state==DreamState.CLEANUP)return;
        if(s.actor!=null){var old=s.region.target.getEntity(s.actor);if(old!=null)old.discard();}
        DreamPlayerEntity actor=new DreamPlayerEntity(DreamContent.PLAYER.get(),s.region.target);
        Vec3 pos=safeBeside(s.region.target,s.region.copiedBed);
        var profile=PlayerBehaviorManager.profile(player);
        actor.setPos(pos);actor.configure(player.getUUID(),s.semantics.chooseContainer(profile,s.region),DreamPlayerProfile.from(profile));
        if(!s.region.target.addFreshEntity(actor))throw new IllegalStateException("Não foi possível criar o DreamPlayer.");
        s.actor=actor.getUUID();
    }
    public static Vec3 safeBeside(ServerLevel level,BlockPos bed){
        for(int radius=1;radius<=4;radius++)for(int dy=0;dy<=2;dy++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            BlockPos pos=bed.offset(dx,dy,dz);
            if(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()
                    &&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()
                    &&!level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty()
                    &&level.getFluidState(pos).isEmpty())return Vec3.atBottomCenterOf(pos);
        }
        throw new IllegalStateException("Sem espaço seguro para acordar ao lado da cama.");
    }
    private static void teleport(ServerPlayer p,ServerLevel level,Vec3 pos,float yaw,float pitch){
        INTERNAL.add(p.getUUID());
        try{p.teleportTo(level,pos.x,pos.y,pos.z,yaw,pitch);}finally{INTERNAL.remove(p.getUUID());}
    }
    public static void stop(ServerPlayer player)throws Exception{
        DreamSession s=session(player);
        if(s==null||s.state==DreamState.CLEANUP)return;
        try{restore(player,s.backup,s.region.originalBed,s.region.source);}
        catch(Exception failure){
            s.state=DreamState.REAL_AWAKENING;s.age=0;s.frozen=player.position();
            throw failure;
        }
        s.state=DreamState.CLEANUP;s.age=0;s.region.clear();
        send(player,DreamState.NONE,14);
    }
    private static void restore(ServerPlayer p,CompoundTag backup,BlockPos bed,ServerLevel source)throws Exception{
        CompoundTag tag=backup.getCompound("player").copy();
        ResourceKey<Level> key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(backup.getString("dimension")));
        ServerLevel real=p.server.getLevel(key);
        if(real==null)throw new IllegalStateException("Dimensão real ausente; backup mantido para recuperação.");
        INTERNAL.add(p.getUUID());
        try{
            p.closeContainer();p.stopRiding();p.removeAllEffects();
            p.getRecipeBook().removeRecipes(p.server.getRecipeManager().getRecipes(),p);
            p.load(tag);
            p.getPersistentData().remove(DreamJournal.MARKER);
            p.setGameMode(GameType.byId(tag.getInt("playerGameType")));
            p.setRespawnPosition(tag.contains("SpawnDimension")?ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(tag.getString("SpawnDimension"))):Level.OVERWORLD,
                    tag.contains("SpawnX")?new BlockPos(tag.getInt("SpawnX"),tag.getInt("SpawnY"),tag.getInt("SpawnZ")):null,
                    tag.getFloat("SpawnAngle"),tag.getBoolean("SpawnForced"),false);
            Vec3 position=p.position();
            ServerLevel destination=real;
            if(bed!=null&&source!=null&&source.getBlockState(bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock){
                try{position=safeBeside(source,bed);destination=source;}catch(IllegalStateException ignored){}
            }
            p.teleportTo(destination,position.x,position.y,position.z,p.getYRot(),p.getXRot());
            p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            p.onUpdateAbilities();p.inventoryMenu.broadcastFullState();
            p.getRecipeBook().sendInitialRecipeBook(p);
            for(var effect:p.getActiveEffects())p.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(p.getId(),effect,false));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(p.getHealth(),p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(p.experienceProgress,p.totalExperience,p.experienceLevel));
            DreamJournal.finish(p);
        }finally{INTERNAL.remove(p.getUUID());}
    }
    public static void recover(ServerPlayer player){
        try{
            CompoundTag backup=DreamJournal.read(player);
            if(backup!=null){
                if(player.getPersistentData().getBoolean(DreamJournal.MARKER)||player.level().dimension().equals(DreamContent.DIMENSION))
                    restore(player,backup,null,null);
                else DreamJournal.finish(player);
                send(player,DreamState.NONE,0);
            }
        }catch(Exception error){WayAround.LOGGER.error("Could not recover dream for {}; rollback file retained",player.getUUID(),error);}
    }
    public static void stopping(MinecraftServer server){
        for(ServerPlayer p:server.getPlayerList().getPlayers())try{if(active(p))stop(p);}catch(Exception e){WayAround.LOGGER.error("Dream shutdown rollback retained",e);}
    }
    public static void clear(){SESSIONS.values().forEach(s->s.region.releaseTickets());SESSIONS.clear();INTERNAL.clear();}
}
