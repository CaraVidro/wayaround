package net.caravidro.wayaround.client;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.caravidro.wayaround.WayAround;
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
    private static void cancel(){
        if(Minecraft.getInstance().getConnection()!=null) PacketDistributor.sendToServer(new SpectrumInputPayload(0,SpectrumInputPayload.CANCEL));
        PRESSED.clear();
    }
    @SubscribeEvent public static void key(InputEvent.Key e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null)return;
        if(TOGGLE.consumeClick()){
            cancel(); open=!open;
            var unlocked=unlocked();
            if(unlocked.isEmpty()){open=false;mc.player.displayClientMessage(Component.literal("Nenhum Spectrum desbloqueado."),true);}
            else if(!unlocked.contains(selected))selected=unlocked.getFirst();
            page=0;
            // Shift+T must not also open vanilla chat; remapped toggle keys still work.
            while(mc.options.keyChat.consumeClick()){}
            return;
        }
        if(!open)return;
        if(NEXT.consumeClick()){
            cancel();var types=unlocked();if(!types.isEmpty())selected=types.get((types.indexOf(selected)+1)%types.size());page=0;return;
        }
        if(PAGE.consumeClick()){cancel();page=(page+1)%Math.max(1,(actions().size()+8)/9);return;}
        int slot=e.getKey()-GLFW.GLFW_KEY_1;
        if(slot<0||slot>8)return;
        // InputEvent fires after KeyMapping updates and before Minecraft consumes hotbar clicks.
        mc.options.keyHotbar[slot].setDown(false);
        while(mc.options.keyHotbar[slot].consumeClick()){}
        if(e.getAction()==GLFW.GLFW_PRESS){
            int index=page*9+slot;
            if(index>=actions().size())return;
            int id=actions().get(index).id;PRESSED.put(e.getKey(),id);
            PacketDistributor.sendToServer(new SpectrumInputPayload(id,SpectrumInputPayload.PRESS));
        } else if(e.getAction()==GLFW.GLFW_RELEASE){
            Integer id=PRESSED.remove(e.getKey());
            if(id!=null)PacketDistributor.sendToServer(new SpectrumInputPayload(id,SpectrumInputPayload.RELEASE));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){open=false;PRESSED.clear();return;}
        if(open&&(mc.screen!=null||!SpectrumAccess.has(mc.player,selected))){cancel();open=false;}
    }
    @SubscribeEvent public static void gui(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(!open||mc.player==null||mc.options.hideGui)return;
        var g=e.getGuiGraphics();var all=actions();int count=Math.min(9,all.size()-page*9);
        int w=Math.min(226,mc.getWindow().getGuiScaledWidth()-16),h=44+count*20;
        int x=mc.getWindow().getGuiScaledWidth()-w-8,y=Math.max(8,(mc.getWindow().getGuiScaledHeight()-h)/2);
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF777777);g.fill(x,y,x+w,y+h,0xD914141A);
        g.drawString(mc.font,"SPECTRUM · "+selected.path().toUpperCase(Locale.ROOT),x+7,y+7,0xFFF3CE,false);
        for(int i=0;i<count;i++){
            SpectrumAction action=all.get(page*9+i);int row=y+22+i*20;
            boolean held=PRESSED.containsValue(action.id);
            g.fill(x+5,row,x+w-5,row+18,held?0xC08A2929:0xB03B3B42);
            g.drawString(mc.font,(i+1)+"  "+action.label,x+9,row+5,held?0xFFFFFF:0xDDDDDD,false);
        }
        g.drawString(mc.font,"Página "+(page+1)+"/"+((all.size()+8)/9)+" · "+PAGE.getTranslatedKeyMessage().getString(),x+7,y+h-13,0xB7B7C2,false);
        if(selected==SpectrumType.TUKUNA)g.drawString(mc.font,"1 segurar: rajada · 1 → 2: fogo",x+3,y+h+6,0xFFAD66,false);
    }
}
