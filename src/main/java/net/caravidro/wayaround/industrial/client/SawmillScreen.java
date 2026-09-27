package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.industrial.power.SawmillMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SawmillScreen
        extends AbstractContainerScreen<SawmillMenu> {

    public SawmillScreen(
            SawmillMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );

        imageWidth =
                212;

        imageHeight =
                136;
    }

    @Override
    protected void init() {
        super.init();

        addRecipeButton(
                0,
                "10 Tábuas",
                22
        );

        addRecipeButton(
                1,
                "4 Pás da Roda",
                48
        );

        addRecipeButton(
                2,
                "20 Pregos de Madeira",
                74
        );
    }

    private void addRecipeButton(
            int id,
            String label,
            int yOffset
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        label
                                ),
                                button -> {
                                    if (minecraft != null
                                            && minecraft.gameMode != null) {
                                        minecraft.gameMode
                                                .handleInventoryButtonClick(
                                                        menu.containerId,
                                                        id
                                                );
                                    }
                                }
                        )
                        .bounds(
                                leftPos + 18,
                                topPos + yOffset,
                                176,
                                20
                        )
                        .build()
        );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF2E241C
        );

        graphics.fill(
                leftPos + 4,
                topPos + 4,
                leftPos + imageWidth - 4,
                topPos + imageHeight - 4,
                0xFFC7A46D
        );

        int selectedY =
                22
                        + menu.selectedRecipe()
                                * 26;

        graphics.fill(
                leftPos + 12,
                topPos + selectedY - 3,
                leftPos + 200,
                topPos + selectedY + 23,
                0x66FFF0A0
        );

        graphics.drawString(
                font,
                "Produto do próximo tronco",
                leftPos + 18,
                topPos + 8,
                0xFF2B2118,
                false
        );

        graphics.drawString(
                font,
                "Corte: "
                        + menu.progressPercent()
                        + "%",
                leftPos + 18,
                topPos + 104,
                0xFF2B2118,
                false
        );

        graphics.drawString(
                font,
                menu.manualCranking()
                        ? "Manivela: girando"
                        : "Manivela: parada",
                leftPos + 108,
                topPos + 104,
                menu.manualCranking()
                        ? 0xFF346B30
                        : 0xFF6A4932,
                false
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }
}
