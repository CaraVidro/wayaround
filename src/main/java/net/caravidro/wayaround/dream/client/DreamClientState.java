package net.caravidro.wayaround.dream.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.dream.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class DreamClientState {
    private static DreamState state=DreamState.NONE;
    private static int elapsed,wakeTicks;
    public static DreamState state(){return state;}
    public static void receive(DreamPayload payload){
        state=payload.state();elapsed=0;
        Minecraft mc=Minecraft.getInstance();
        if(state==DreamState.NONE){
            if(mc.screen instanceof DreamTransitionScreen||mc.screen instanceof FakeDeathScreen)mc.setScreen(null);
            wakeTicks=payload.ticks();
        }else if(state==DreamState.NORMAL){
            if(mc.screen instanceof DreamTransitionScreen||mc.screen instanceof FakeDeathScreen)mc.setScreen(null);
        }else if(state==DreamState.FAKE_MENU)mc.setScreen(new FakeDeathScreen());
        else mc.setScreen(new DreamTransitionScreen());
    }
    public static float fade(){
        return switch(state){
            case PREPARING -> 1;
            case REAL_AWAKENING -> Math.min(1,elapsed/3F);
            case FALSE_AWAKENING -> Math.max(0,1-elapsed/20F);
            case DREAM_DEATH -> Math.min(1,elapsed/8F);
            default -> 0;
        };
    }
    public static float wakeOffset(float partialTick){
        if(wakeTicks<=0)return 0;
        float t=(14-wakeTicks+partialTick)/14F;
        return (float)Math.sin(Math.PI*t)*.18F*(1-t);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null){state=DreamState.NONE;wakeTicks=0;return;}
        elapsed++;if(wakeTicks>0)wakeTicks--;
        if(state!=DreamState.NONE&&state!=DreamState.NORMAL){
            if(mc.player!=null){mc.player.input.forwardImpulse=0;mc.player.input.leftImpulse=0;mc.player.input.jumping=false;}
            if(state==DreamState.FAKE_MENU&&!(mc.screen instanceof FakeDeathScreen))mc.setScreen(new FakeDeathScreen());
            else if(state!=DreamState.FAKE_MENU&&!(mc.screen instanceof DreamTransitionScreen))mc.setScreen(new DreamTransitionScreen());
        }
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){
        if(wakeTicks>0){
            float t=(14-wakeTicks+(float)e.getPartialTick())/14F;
            e.setPitch(e.getPitch()-(float)Math.sin(t*Math.PI*2)*2.5F*(1-t));
        }
    }
}
