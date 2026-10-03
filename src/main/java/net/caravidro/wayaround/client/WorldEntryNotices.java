package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.voice.client.VoiceSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One introduction per connection, shown before optional voice settings. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class WorldEntryNotices {
    private static boolean pending;
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn e) { pending=true; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { pending=false; }
    public static boolean blockVoiceCapture() { return pending || Minecraft.getInstance().screen instanceof NoticeScreen; }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();
        if(pending && mc.level!=null && mc.player!=null && mc.screen==null) { pending=false;mc.setScreen(new NoticeScreen()); }
    }
    public static final class NoticeScreen extends Screen {
        private int scroll,contentHeight;
        public NoticeScreen() { super(Component.translatable("screen.wayaround.entry.title")); }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("screen.wayaround.entry.continue"),b->onClose()).bounds(width/2-100,height-30,200,20).build());
        }
        @Override public boolean mouseScrolled(double x,double y,double dx,double dy) { scroll=net.minecraft.util.Mth.clamp(scroll-(int)(dy*18),0,Math.max(0,contentHeight-(height-92)));return true; }
        @Override public void onClose() { minecraft.setScreen(new VoiceSettingsScreen(null)); }
        @Override public void render(GuiGraphics g,int x,int y,float partial) {
            super.render(g,x,y,partial);int textWidth=Math.min(420,width-36),top=52-scroll;
            g.drawCenteredString(font,title,width/2,22,0xFFFFFF);g.enableScissor(12,48,width-12,height-40);
            for(int i=1;i<=4;i++) {
                for(var line:font.split(Component.translatable("screen.wayaround.entry."+i),textWidth)) { g.drawString(font,line,(width-textWidth)/2,top,0xE0E0E0,false);top+=font.lineHeight+2; }
                top+=9;
            }
            contentHeight=top+scroll-52;g.disableScissor();
        }
    }
}
