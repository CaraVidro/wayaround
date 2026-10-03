package net.caravidro.wayaround.nature;

import java.io.ByteArrayOutputStream;
import java.util.*;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ephemeral clips from already-authorized voice traffic, never microphone capture. */
public final class ParrotMimicManager {
    private static final Map<UUID,Clip> CLIPS=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final Map<UUID,Long> SEARCH=new HashMap<>();
    private static final class Clip {
        final UUID player;final WoodlandBirdEntity bird;
        final ByteArrayOutputStream data=new ByteArrayOutputStream();
        long last,start=-1;byte[] echo;int offset;
        Clip(UUID p,WoodlandBirdEntity b,long now){player=p;bird=b;last=now;}
    }
    private ParrotMimicManager(){}
    public static void capture(ServerPlayer player,byte[] pcm,boolean speech){
        if(!speech||pcm.length>4096||!player.isAlive())return;
        long now=player.serverLevel().getGameTime();
        Clip clip=CLIPS.get(player.getUUID());
        if(clip==null){
            if(CLIPS.size()>=8||now<SEARCH.getOrDefault(player.getUUID(),0L))return;
            SEARCH.put(player.getUUID(),now+20);
            var birds=player.serverLevel().getEntitiesOfClass(WoodlandBirdEntity.class,player.getBoundingBox().inflate(12),b->b.species()==2&&b.isAlive());
            if(birds.isEmpty())return;
            var bird=birds.get(0);
            if(now<COOLDOWNS.getOrDefault(bird.getUUID(),0L))return;
            clip=new Clip(player.getUUID(),bird,now);CLIPS.put(player.getUUID(),clip);COOLDOWNS.put(bird.getUUID(),now+400);
        }
        if(clip.start>=0||clip.bird.level()!=player.level()||player.distanceToSqr(clip.bird)>12*12)return;
        if(clip.data.size()+pcm.length<=96000)clip.data.write(pcm,0,pcm.length);
        clip.last=now;
        if(clip.data.size()>=92000)finish(clip,now);
    }
    private static void finish(Clip clip,long now){clip.echo=NatureMath.mimic(clip.data.toByteArray());clip.data.reset();clip.start=now+40;}
    public static void tick(MinecraftServer server){
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.VOICE_CHAT)){clear();return;}
        var it=CLIPS.values().iterator();
        while(it.hasNext()){
            Clip c=it.next();ServerPlayer owner=server.getPlayerList().getPlayer(c.player);
            if(!c.bird.isAlive()||c.bird.isRemoved()||owner==null||owner.level()!=c.bird.level()){it.remove();continue;}
            long now=c.bird.level().getGameTime();
            if(c.start<0&&now-c.last>=8)finish(c,now);
            if(c.start<0||now<c.start)continue;
            int due=(int)Math.min(c.echo.length,(now-c.start+1)*4800);
            for(int sent=0;sent<2&&c.offset<due;sent++){
                int end=Math.min(c.echo.length,c.offset+VoiceConstants.FRAME_BYTES);
                byte[] frame=Arrays.copyOfRange(c.echo,c.offset,end);c.offset=end;
                var packet=new NatureAmbientPayload(1,c.bird.getId(),0,c.bird.getX(),c.bird.getY(),c.bird.getZ(),frame);
                for(var p:((ServerLevel)c.bird.level()).players())if(p.distanceToSqr(c.bird)<24*24)PacketDistributor.sendToPlayer(p,packet);
            }
            if(c.offset>=c.echo.length)it.remove();
        }
        if(server.getTickCount()%200==0){
            SEARCH.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
            if(COOLDOWNS.size()>256)COOLDOWNS.clear();
        }
    }
    public static void clear(){CLIPS.clear();COOLDOWNS.clear();SEARCH.clear();}
}
