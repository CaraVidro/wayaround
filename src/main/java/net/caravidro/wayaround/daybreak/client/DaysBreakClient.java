package net.caravidro.wayaround.daybreak.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.daybreak.DaysBreakMath;
import net.caravidro.wayaround.daybreak.DaysBreakPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class DaysBreakClient {
    private static boolean enabled;
    private static long elapsed,received;
    private static Theme theme;
    public static void receive(DaysBreakPayload packet){enabled=packet.active();elapsed=packet.elapsed();var level=Minecraft.getInstance().level;received=level==null?-1:level.getGameTime();if(!enabled)stopMusic();}
    public static boolean active(){var level=Minecraft.getInstance().level;return enabled&&level!=null&&level.dimensionType().hasSkyLight()&&!level.dimensionType().hasFixedTime();}
    public static boolean day(){return active()&&DaysBreakMath.day(Minecraft.getInstance().level.getDayTime());}
    public static float sunScale(){var level=Minecraft.getInstance().level;if(level!=null&&received<0)received=level.getGameTime();return DaysBreakMath.sunScale(elapsed+(level==null?0:Math.max(0,level.getGameTime()-received)));}
    private static void stopMusic(){if(theme!=null){Minecraft.getInstance().getSoundManager().stop(theme);theme=null;}}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){enabled=false;elapsed=0;stopMusic();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(day()){var mc=Minecraft.getInstance();if(theme==null){mc.getMusicManager().stopPlaying();theme=new Theme();mc.getSoundManager().play(theme);}else if(mc.level.getGameTime()%100==0)mc.getMusicManager().stopPlaying();}
        else stopMusic();
    }
    private static final class Theme extends AbstractTickableSoundInstance {
        Theme(){super(WayAroundSounds.THEN_DAYS_BREAK.get(),SoundSource.MUSIC,SoundInstance.createUnseededRandom());looping=true;relative=true;attenuation=SoundInstance.Attenuation.NONE;volume=.85f;pitch=1;}
        @Override public void tick(){if(!day())stop();}
    }
}
