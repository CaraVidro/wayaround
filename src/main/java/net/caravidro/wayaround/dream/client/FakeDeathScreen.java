package net.caravidro.wayaround.dream.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Inert prop GUI. No button changes worlds, respawns, disconnects or opens another screen. */
public final class FakeDeathScreen extends Screen {
    public FakeDeathScreen(){super(Component.translatable("deathScreen.title"));}
    @Override protected void init(){
        addRenderableWidget(Button.builder(Component.translatable("deathScreen.respawn"),button->{})
                .bounds(width/2-100,height/4+72,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"),button->{})
                .bounds(width/2-100,height/4+96,200,20).build());
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public boolean mouseClicked(double x,double y,int button){return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){return true;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        graphics.fillGradient(0,0,width,height,0xA0601010,0xD0100000);
        graphics.pose().pushPose();graphics.pose().scale(2,2,2);
        graphics.drawCenteredString(font,title,width/4,30,0xFFFFFF);graphics.pose().popPose();
        for(var widget:renderables)widget.render(graphics,mouseX,mouseY,partialTick);
    }
}
