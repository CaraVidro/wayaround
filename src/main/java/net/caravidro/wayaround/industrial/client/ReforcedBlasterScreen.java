package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.industrial.ReforcedBlasterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ReforcedBlasterScreen extends AbstractContainerScreen<ReforcedBlasterMenu> {
    public ReforcedBlasterScreen(ReforcedBlasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6C6C6);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF555555);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
        }
        graphics.fill(x + 73, y + 39, x + 103, y + 47, 0xFF555555);
        graphics.fill(x + 73, y + 39, x + 73 + menu.progressPixels(30), y + 47, 0xFFFFB040);
        graphics.drawString(font, menu.energy() + " / " + menu.capacity() + " FE", x + 8, y + 61, 0x404040, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
