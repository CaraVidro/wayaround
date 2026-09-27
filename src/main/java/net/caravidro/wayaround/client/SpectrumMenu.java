package net.caravidro.wayaround.client;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.JujutsuCastC2SPayload;
import net.caravidro.wayaround.network.SpectrumInputPayload;
import net.caravidro.wayaround.spectrum.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class SpectrumMenu {
    public static final KeyMapping TOGGLE=key("Spectrum: abrir / fechar",GLFW.GLFW_KEY_T);
    public static final KeyMapping NEXT=key("Spectrum: próximo spectrum",GLFW.GLFW_KEY_Y);
    public static final KeyMapping PAGE=key("Spectrum: próxima página",GLFW.GLFW_KEY_U);
    private static KeyMapping key(String name,int key){return new KeyMapping(name,KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,InputConstants.Type.KEYSYM,key,"Way Around - Spectrums");}
    private static boolean open;
    private static SpectrumType selected=SpectrumType.TUKUNA;
    private static int page;
    private static final Map<Integer,Integer> PRESSED=new HashMap<>();
    public static boolean isOpen(){return open;}
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(TOGGLE);e.register(NEXT);e.register(PAGE);}
    private static List<SpectrumType> unlocked(){var p=Minecraft.getInstance().player;return p==null?List.of():Arrays.stream(SpectrumType.values()).filter(t->SpectrumAccess.has(p,t)).toList();}
    private static List<SpectrumAction> actions(){return SpectrumAction.forSpectrum(selected);}
    private static boolean jujutsuMode(){return unlocked().isEmpty();}
    private static void cancel(){
        if(Minecraft.getInstance().getConnection()!=null) PacketDistributor.sendToServer(new SpectrumInputPayload(0,SpectrumInputPayload.CANCEL));
        PRESSED.clear();
    }
    private static void menuState(boolean active){
        if(Minecraft.getInstance().getConnection()!=null && selected!=null)
            PacketDistributor.sendToServer(new SpectrumInputPayload(selected.ordinal(),
                    active?SpectrumInputPayload.MENU_OPEN:SpectrumInputPayload.MENU_CLOSE));
    }
    @SubscribeEvent public static void key(InputEvent.Key e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null)return;
        if(TOGGLE.consumeClick()){
            boolean wasJujutsu=jujutsuMode();
            if(open&&!wasJujutsu)menuState(false);
            cancel(); open=!open;
            var unlocked=unlocked();
            if(!unlocked.isEmpty()&&!unlocked.contains(selected))selected=unlocked.getFirst();
            page=0;
            if(open&&!unlocked.isEmpty())menuState(true);
            // Shift+T is the shared combat/ability panel for Spectrums and awakened Jujutsu.
            while(mc.options.keyChat.consumeClick()){}
            return;
        }
        if(!open)return;
        if(NEXT.consumeClick()){
            if(jujutsuMode())return;
            menuState(false);cancel();var types=unlocked();if(!types.isEmpty())selected=types.get((types.indexOf(selected)+1)%types.size());page=0;menuState(true);return;
        }
        if(PAGE.consumeClick()){
            if(jujutsuMode())return;
            cancel();page=(page+1)%Math.max(1,(actions().size()+9)/10);return;
        }
        int slot;
        if(e.getKey()>=GLFW.GLFW_KEY_1 && e.getKey()<=GLFW.GLFW_KEY_9) slot=e.getKey()-GLFW.GLFW_KEY_1;
        else if(e.getKey()==GLFW.GLFW_KEY_0) slot=9;
        else return;
        // 1-9 are also vanilla hotbar keys; suppress those while the Spectrum menu is open.
        if(slot<9){
            mc.options.keyHotbarSlots[slot].setDown(false);
            while(mc.options.keyHotbarSlots[slot].consumeClick()){}
        }
        if(e.getAction()==GLFW.GLFW_PRESS){
            if(jujutsuMode()){
                if(slot==0){
                    PacketDistributor.sendToServer(new JujutsuCastC2SPayload((byte)0));
                }
                return;
            }
            int index=page*10+slot;
            if(index>=actions().size())return;
            int id=actions().get(index).id;PRESSED.put(e.getKey(),id);
            PacketDistributor.sendToServer(new SpectrumInputPayload(id,SpectrumInputPayload.PRESS));
        } else if(e.getAction()==GLFW.GLFW_RELEASE){
            if(jujutsuMode())return;
            Integer id=PRESSED.remove(e.getKey());
            if(id!=null)PacketDistributor.sendToServer(new SpectrumInputPayload(id,SpectrumInputPayload.RELEASE));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){open=false;PRESSED.clear();return;}
        if(open&&mc.screen!=null){
            if(!jujutsuMode())menuState(false);
            cancel();open=false;return;
        }
        if(open&&!jujutsuMode()&&!SpectrumAccess.has(mc.player,selected)){menuState(false);cancel();open=false;}
    }
    @SubscribeEvent public static void gui(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(!open||mc.player==null||mc.options.hideGui)return;
        var g=e.getGuiGraphics();
        boolean jujutsu=jujutsuMode();
        var all=jujutsu?List.<SpectrumAction>of():actions();
        int count=jujutsu?1:Math.min(10,Math.max(0,all.size()-page*10));
        int w=Math.min(184,mc.getWindow().getGuiScaledWidth()-16),h=31+count*14;
        int x=mc.getWindow().getGuiScaledWidth()-w-6,y=Math.max(6,(mc.getWindow().getGuiScaledHeight()-h)/2);
        g.fill(x-1,y-1,x+w+1,y+h+1,0xAA666666);g.fill(x,y,x+w,y+h,0xC914141A);
        g.drawString(mc.font,jujutsu?"JUJUTSU · TÉCNICA":"SPECTRUM · "+selected.path().toUpperCase(Locale.ROOT),x+5,y+5,0xFFF3CE,false);
        if(jujutsu){
            int row=y+17;
            g.fill(x+3,row,x+w-3,row+12,0x983B3B42);
            g.drawString(mc.font,"1 Usar técnica",x+6,row+2,0xDDDDDD,false);
        }
        for(int i=0;i<(jujutsu?0:count);i++){
            SpectrumAction action=all.get(page*10+i);int row=y+17+i*14;
            boolean held=PRESSED.containsValue(action.id);
            g.fill(x+3,row,x+w-3,row+12,held?0xB88A2929:0x983B3B42);
            String key=i==9?"0":Integer.toString(i+1);
            String label=action.label;
            while(mc.font.width(label)>w-28 && label.length()>4) label=label.substring(0,label.length()-2)+"…";
            g.drawString(mc.font,key+" "+label,x+6,row+2,held?0xFFFFFF:0xDDDDDD,false);
        }
        if(!jujutsu)g.drawString(mc.font,"P "+(page+1)+"/"+Math.max(1,(all.size()+9)/10)+" · "+PAGE.getTranslatedKeyMessage().getString(),x+5,y+h-10,0xAFAFB8,false);
        else g.drawString(mc.font,"Shift+T · habilidade do Orb",x+5,y+h-10,0xAFAFB8,false);
    }
}
