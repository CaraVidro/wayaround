package net.caravidro.wayaround.industrial.ship.client;

import java.util.ArrayList;
import java.util.List;
import net.caravidro.wayaround.industrial.ship.ShipMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ShipScreen extends AbstractContainerScreen<ShipMenu> {
    private Button anchor;
    private final List<Button> largeButtons = new ArrayList<>();
    public ShipScreen(ShipMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
        inventoryLabelY = -1000;
    }
    @Override protected void init() {
        super.init();
        largeButtons.clear();
        anchor = button(0, "anchor", 12, 30);
        button(1, "cargo", 132, 30);
        largeButtons.add(button(2, "craft", 12, 58));
        largeButtons.add(button(3, "sleep", 132, 58));
        for (int seat = 0; seat < 6; seat++) {
            Button button = button(10 + seat, seat == 0 ? "helm" : "seat" + seat,
                    12 + (seat % 2) * 120, 98 + (seat / 2) * 28);
            if (seat > 0) largeButtons.add(button);
        }
    }
    private Button button(int action, String label, int x, int y) {
        return addRenderableWidget(Button.builder(Component.translatable("menu.wayaround.ship." + label), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            }
        }).bounds(leftPos + x, topPos + y, 112, 20).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF33291F);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFD2C4A8);
        anchor.setMessage(Component.translatable("menu.wayaround.ship." + (menu.isAnchored() ? "raise_anchor" : "anchor")));
        largeButtons.forEach(button -> button.active = menu.isLarge());
        graphics.drawString(font, Component.translatable("menu.wayaround.ship." + (menu.isAnchored() ? "moored" : "adrift")),
                leftPos + 12, topPos + 193, 0x403020, false);
    }
}
