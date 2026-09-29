package net.caravidro.wayaround.industrial.ship.client;

import java.util.ArrayList;
import java.util.List;

import net.caravidro.wayaround.industrial.ship.ShipMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ShipScreen
        extends AbstractContainerScreen<ShipMenu> {

    private Button anchor;
    private final List<Button> largeButtons =
            new ArrayList<>();

    public ShipScreen(
            ShipMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );

        imageWidth =
                280;

        imageHeight =
                314;

        inventoryLabelY =
                -1000;
    }

    @Override
    protected void init() {
        super.init();

        largeButtons.clear();

        anchor =
                button(
                        0,
                        "anchor",
                        12,
                        30,
                        122
                );

        button(
                1,
                "cargo",
                146,
                30,
                122
        );

        largeButtons.add(
                button(
                        2,
                        "craft",
                        12,
                        58,
                        122
                )
        );

        largeButtons.add(
                button(
                        3,
                        "sleep",
                        146,
                        58,
                        122
                )
        );

        largeButtons.add(
                button(
                        4,
                        "trim_left",
                        12,
                        98,
                        122
                )
        );

        largeButtons.add(
                button(
                        5,
                        "trim_right",
                        146,
                        98,
                        122
                )
        );

        largeButtons.add(
                button(
                        6,
                        "sails",
                        12,
                        126,
                        122
                )
        );

        largeButtons.add(
                button(
                        7,
                        "pump",
                        146,
                        126,
                        122
                )
        );

        for (int seat = 0;
             seat < 6;
             seat++) {

            Button button =
                    button(
                            10 + seat,
                            seat == 0
                                    ? "helm"
                                    : "seat"
                                            + seat,
                            12
                                    + (
                                    seat % 2
                            ) * 134,
                            194
                                    + (
                                    seat / 2
                            ) * 28,
                            122
                    );

            if (seat > 0) {
                largeButtons.add(
                        button
                );
            }
        }
    }

    private Button button(
            int action,
            String label,
            int x,
            int y,
            int width
    ) {
        return addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "menu.wayaround.ship."
                                                + label
                                ),
                                button -> {
                                    if (minecraft != null
                                            && minecraft.gameMode
                                            != null) {
                                        minecraft.gameMode
                                                .handleInventoryButtonClick(
                                                        menu.containerId,
                                                        action
                                                );
                                    }
                                }
                        )
                        .bounds(
                                leftPos + x,
                                topPos + y,
                                width,
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
                0xFF241D17
        );

        graphics.fill(
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                0xFFD6C8A9
        );

        graphics.fill(
                leftPos + 8,
                topPos + 88,
                leftPos + imageWidth - 8,
                topPos + 164,
                0x66543B26
        );

        anchor.setMessage(
                Component.translatable(
                        "menu.wayaround.ship."
                                + (
                                menu.isAnchored()
                                        ? "raise_anchor"
                                        : "anchor"
                        )
                )
        );

        for (Button button :
                largeButtons) {
            button.active =
                    menu.isLarge();

            button.visible =
                    menu.isLarge();
        }

        if (menu.isLarge()) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "menu.wayaround.ship.navigation"
                    ),
                    leftPos + 12,
                    topPos + 78,
                    0x403020,
                    false
            );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "menu.wayaround.ship.hull_status",
                            menu.hullPercent(),
                            menu.floodingPercent()
                    ),
                    leftPos + 12,
                    topPos + 154,
                    hullColor(),
                    false
            );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "menu.wayaround.ship.sail_status",
                            menu.sailsRaised()
                                    ? Component.translatable(
                                    "menu.wayaround.ship.sails_up"
                            )
                                    : Component.translatable(
                                    "menu.wayaround.ship.sails_down"
                            ),
                            menu.sailTrim(),
                            menu.windEfficiency()
                    ),
                    leftPos + 12,
                    topPos + 166,
                    0x403020,
                    false
            );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "menu.wayaround.ship.course_status",
                            menu.heading(),
                            menu.seaSeverity()
                    ),
                    leftPos + 12,
                    topPos + 178,
                    0x403020,
                    false
            );
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "menu.wayaround.ship."
                                + (
                                menu.isAnchored()
                                        ? "moored"
                                        : "adrift"
                        )
                ),
                leftPos + 12,
                topPos + 292,
                0x403020,
                false
        );
    }

    private int hullColor() {
        if (menu.floodingPercent() >= 70
                || menu.hullPercent() <= 30) {
            return 0xA52620;
        }

        if (menu.floodingPercent() >= 35
                || menu.hullPercent() <= 60) {
            return 0x8C5D15;
        }

        return 0x355D2F;
    }
}
