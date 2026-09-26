package net.caravidro.wayaround.spectrum;

import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cinematic.PlayerControlLockManager;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.justice.JusticeDomainManager;
import net.caravidro.wayaround.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server owns hold times, combo windows, access, burst limits and cancellation. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class SpectrumActions {
    private SpectrumActions(){}
    private static final Map<UUID,Gesture> GESTURES=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWN=new HashMap<>();
    private static final Map<UUID,Long> INPUT_DEBOUNCE=new HashMap<>();
    private static final Map<UUID,Long> FUGA_DEBOUNCE=new HashMap<>();
    private static boolean allowed(ServerPlayer p, SpectrumType type){
        return p.isAlive() && !p.isSpectator() && !TukunaManager.isSilencedHost(p)
                && !TukunaManager.isDraftingPact(p) && !TukunaManager.isPacifistPossession(p)
                && !PlayerControlLockManager.actionsLocked(p) && SpectrumAccess.has(p,type);
    }
    public static void input(ServerPlayer p,int id,byte phase){
        if(phase==SpectrumInputPayload.CANCEL){cancel(p);return;}
        SpectrumAction action=SpectrumAction.byId(id);
        if(action==null || !allowed(p,action.spectrum)) return;
        long now=p.server.getTickCount();
        if(action==SpectrumAction.SLASH){
            if(phase==SpectrumInputPayload.PRESS && now>=COOLDOWN.getOrDefault(p.getUUID(),0L)){
                Gesture g=GESTURES.get(p.getUUID());
                if(g==null){g=new Gesture(now);GESTURES.put(p.getUUID(),g);pose(p,PlayerCinematicPayload.DESMARTELAR_CHARGE,200);}
                else if(g.released && !g.firing){g.started=now;g.released=false;}
            } else if(phase==SpectrumInputPayload.RELEASE){
                Gesture g=GESTURES.get(p.getUUID());
                if(g!=null && !g.released){
                    long held=now-g.started;
                    g.shots=held<8?1:Math.min(8,2+(int)(held/10));
                    g.released=true;g.next=now+6;
                }
            }
            return;
        }
        if(phase!=SpectrumInputPayload.PRESS) return;
        Gesture g=GESTURES.get(p.getUUID());
        if(action==SpectrumAction.FUGA && g!=null && !g.firing){
            g.fire=true;
            if(g.released) g.next=now;
            pose(p,PlayerCinematicPayload.DESMARTELAR_FIRE_CHARGE,200);
            return;
        }
        if(now<INPUT_DEBOUNCE.getOrDefault(p.getUUID(),0L))return;
        INPUT_DEBOUNCE.put(p.getUUID(),now+3);
        perform(p,action);
    }
    public static void perform(ServerPlayer p,SpectrumAction action){
        if(!allowed(p,action.spectrum)) return;
        switch(action){
            case SLASH,FIRE_SLASH -> TukunaManager.castPossessedDesmartelar(p,action==SpectrumAction.FIRE_SLASH);
            case FUGA -> {
                long now=p.server.getTickCount();
                if(now<FUGA_DEBOUNCE.getOrDefault(p.getUUID(),0L)) return;
                FUGA_DEBOUNCE.put(p.getUUID(),now+6);
                if(TukunaManager.isFugaCharging(p)) TukunaManager.launchFuga(p); else TukunaManager.prepareFuga(p);
            }
            case TUKUNA_DOMAIN -> TukunaDomainPreview.start(p);
            case JUSTICE_DOMAIN -> JusticeDomainManager.beginTrialNearest(p);
            default -> VoiceIntentC2SPayload.execute(p,new VoiceIntentC2SPayload((byte)action.intent,
                    action==SpectrumAction.BLUE_MAX?3.0F:1.0F));
        }
    }
    public static void cancel(ServerPlayer p){
        if(GESTURES.remove(p.getUUID())!=null) pose(p,PlayerCinematicPayload.CLEAR,0);
    }
    public static void pose(ServerPlayer p,byte animation,int duration){
        PacketDistributor.sendToPlayersNear(p.serverLevel(),null,p.getX(),p.getY(),p.getZ(),128,
                new PlayerCinematicPayload(p.getUUID(),animation,duration,false,0));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        long now=event.getServer().getTickCount();
        GESTURES.entrySet().removeIf(e->{
            ServerPlayer p=event.getServer().getPlayerList().getPlayer(e.getKey()); Gesture g=e.getValue();
            if(p==null)return true;
            if(!allowed(p,SpectrumType.TUKUNA)||now-g.started>200){pose(p,PlayerCinematicPayload.CLEAR,0);return true;}
            if(g.released && now>=g.next){
                g.firing=true;
                TukunaManager.castRapidDesmartelar(p,g.fire);
                g.shots--;g.next=now+4;
                if(g.shots<=0){COOLDOWN.put(e.getKey(),now+28);return true;}
            }
            return false;
        });
        if(now%200==0){COOLDOWN.entrySet().removeIf(e->e.getValue()<now);FUGA_DEBOUNCE.entrySet().removeIf(e->e.getValue()<now);INPUT_DEBOUNCE.entrySet().removeIf(e->e.getValue()<now);}
    }
    private static final class Gesture{long started,next;boolean fire,released,firing;int shots;Gesture(long now){started=now;}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){GESTURES.clear();COOLDOWN.clear();FUGA_DEBOUNCE.clear();INPUT_DEBOUNCE.clear();}
}
