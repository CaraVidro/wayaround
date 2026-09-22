package net.caravidro.wayaround.dream.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DreamTransitionScreen extends Screen {
    public DreamTransitionScreen(){super(Component.empty());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        int alpha=Math.clamp((int)(DreamClientState.fade()*255),0,255);
        graphics.fill(0,0,width,height,alpha<<24);
    }
}
