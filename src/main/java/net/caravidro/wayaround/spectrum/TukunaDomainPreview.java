package net.caravidro.wayaround.spectrum;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cinematic.PlayerControlLockManager;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** A short, harmless pocket-room preview, with persistent emergency return coordinates. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class TukunaDomainPreview {
    private static final String RETURN="WayAroundDomainPreviewReturn";
    private static final ResourceKey<Level> ROOM=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(WayAround.MODID,"spectrum_preview"));
    private static final Map<UUID,Preview> ACTIVE=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWN=new HashMap<>();
    public static void start(ServerPlayer p){
        long now=p.server.getTickCount();
        if(ACTIVE.containsKey(p.getUUID())||now<COOLDOWN.getOrDefault(p.getUUID(),0L)||p.server.getLevel(ROOM)==null
                ||p.isPassenger())return;
        int slot=0;Set<Integer> used=new HashSet<>();for(var v:ACTIVE.values())used.add(v.slot);
        while(used.contains(slot))slot++;
        if(slot>=64)return;
        ServerPlayer host=TukunaManager.possessedHostForSpirit(p);
        capture(p);
        if(host!=null)capture(host);
        ACTIVE.put(p.getUUID(),new Preview(now,slot,host==null?null:host.getUUID()));COOLDOWN.put(p.getUUID(),now+240);
        PlayerControlLockManager.lockMovement(p,90);PlayerControlLockManager.lockActions(p,90);
        SpectrumActions.pose(p,PlayerCinematicPayload.TUKUNA_DOMAIN_PREVIEW,90);
    }
    private static void capture(ServerPlayer p){
        CompoundTag saved=new CompoundTag();
        saved.putString("dimension",p.serverLevel().dimension().location().toString());
        saved.putDouble("x",p.getX());saved.putDouble("y",p.getY());saved.putDouble("z",p.getZ());
        saved.putFloat("yaw",p.getYRot());saved.putFloat("pitch",p.getXRot());
        p.getPersistentData().put(RETURN,saved);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        long now=event.getServer().getTickCount();
        ACTIVE.entrySet().removeIf(e->{
            var p=event.getServer().getPlayerList().getPlayer(e.getKey());var v=e.getValue();
            if(p==null)return true;
            if(!p.isAlive()||now-v.start>=90){restoreGroup(p,v);return true;}
            if(!v.entered&&now-v.start>=30){
                var level=event.getServer().getLevel(ROOM);if(level==null){restoreGroup(p,v);return true;}
                int cx=v.slot*32;
                for(int x=-5;x<=5;x++)for(int y=64;y<=72;y++)for(int z=-5;z<=5;z++){
                    if(Math.abs(x)==5||Math.abs(z)==5||y==64||y==72)
                        level.setBlock(new BlockPos(cx+x,y,z),Blocks.BLACK_CONCRETE.defaultBlockState(),2);
                }
                PlayerControlLockManager.clearMovement(p);
                p.teleportTo(level,cx+.5,65,.5,Set.of(),p.getYRot(),0);
                PlayerControlLockManager.lockMovement(p,60);v.entered=true;
                if(v.host!=null){
                    var host=event.getServer().getPlayerList().getPlayer(v.host);
                    if(host!=null){host.teleportTo(level,cx+.5,65,.5,Set.of(),p.getYRot(),0);host.setCamera(p);}
                }
                SpectrumActions.pose(p,PlayerCinematicPayload.TUKUNA_DOMAIN_PREVIEW,60);
            }
            return false;
        });
        if(now%200==0)COOLDOWN.entrySet().removeIf(e->e.getValue()<now);
    }
    private static void restoreGroup(ServerPlayer p,Preview v){
        restore(p);
        if(v.host!=null){var host=p.server.getPlayerList().getPlayer(v.host);if(host!=null)restore(host);}
    }
    private static void restore(ServerPlayer p){
        PlayerControlLockManager.clearMovement(p);PlayerControlLockManager.clearActions(p);
        if(p.getPersistentData().contains(RETURN)){
            var tag=p.getPersistentData().getCompound(RETURN);
            var id=ResourceLocation.tryParse(tag.getString("dimension"));
            var level=id==null?null:p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
            if(level!=null)p.teleportTo(level,tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z"),Set.of(),tag.getFloat("yaw"),tag.getFloat("pitch"));
            else p.teleportTo(p.server.overworld(),p.server.overworld().getSharedSpawnPos().getX()+.5,
                    p.server.overworld().getSharedSpawnPos().getY()+1,p.server.overworld().getSharedSpawnPos().getZ()+.5,Set.of(),0,0);
            p.getPersistentData().remove(RETURN);
        }
        SpectrumActions.pose(p,PlayerCinematicPayload.CLEAR,0);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(RETURN))restore(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var v=ACTIVE.remove(p.getUUID());if(v!=null)restoreGroup(p,v);else if(p.getPersistentData().contains(RETURN))restore(p);}}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){if(e.getEntity() instanceof ServerPlayer p && e.getOriginal().getPersistentData().contains(RETURN))p.getPersistentData().put(RETURN,e.getOriginal().getPersistentData().getCompound(RETURN).copy());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(RETURN))restore(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){ACTIVE.clear();COOLDOWN.clear();}
    private static final class Preview{final long start;final int slot;final UUID host;boolean entered;Preview(long start,int slot,UUID host){this.start=start;this.slot=slot;this.host=host;}}
}
