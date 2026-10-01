package net.caravidro.wayaround.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WayAroundUpdateLogScreen extends Screen {

    private final Screen parent;

    public WayAroundUpdateLogScreen(Screen parent) {
        super(
                Component.translatable(
                        "screen.wayaround.update_log.title",
                        WayAroundReleaseInfo.NEXT_VERSION,
                        WayAroundReleaseInfo.NEXT_TITLE
                )
        );

        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth =
                Math.min(
                        150,
                        Math.max(
                                80,
                                width - 24
                        )
                );

        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.done"),
                                button -> onClose()
                        )
                        .bounds(
                                width / 2 - buttonWidth / 2,
                                Math.max(
                                        8,
                                        height - 28
                                ),
                                buttonWidth,
                                20
                        )
                        .build()
        );
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

        boolean compact =
                height < 220;

        int titleY =
                compact
                        ? 8
                        : 18;

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                titleY,
                0xFFF0E2C0
        );

        if (!compact
                && doneTop - 17 > titleY + 18) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.wayaround.update_log.subtitle"
                    ),
                    width / 2,
                    titleY + 15,
                    0xFF9C8058
            );
        }

        int horizontalMargin =
                Math.max(
                        8,
                        Math.min(
                                18,
                                width / 20
                        )
                );

        int boxLeft =
                Math.max(
                        horizontalMargin,
                        width / 2 - 220
                );

        int boxRight =
                Math.min(
                        width - horizontalMargin,
                        width / 2 + 220
                );

        int top =
                compact
                        ? 26
                        : 50;

        int doneTop =
                Math.max(
                        8,
                        height - 28
                );

        int releaseSpace =
                !compact
                        ? 18
                        : 0;

        int bodyBottom =
                doneTop
                        - 8
                        - releaseSpace;

        boolean bodyVisible =
                bodyBottom
                        >= top
                                + font.lineHeight
                                + 8;

        if (bodyVisible) {
            graphics.fill(
                    boxLeft,
                    top,
                    boxRight,
                    bodyBottom,
                    0xA0181818
            );
        }

        int y = top + 10;
        int textWidth =
                Math.max(
                        40,
                        boxRight
                                - boxLeft
                                - 20
                );

        if (bodyVisible) {
        for (Component line : WayAroundReleaseInfo.updateLogLines()) {
            Component bullet =
                    Component.literal(
                            "• "
                    )
                            .append(
                                    line
                            );

            for (var row : font.split(
                    bullet,
                    textWidth
            )) {
                if (y > bodyBottom - 12) {
                    break;
                }

                graphics.drawString(
                        font,
                        row,
                        boxLeft + 10,
                        y,
                        0xFFD6D6D6,
                        false
                );

                y += 11;
            }

            y += 5;

            if (y > bodyBottom - 12) {
                break;
            }
        }

        }

        if (!compact) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.wayaround.update_log.release_note"
                    ),
                    width / 2,
                    doneTop - 17,
                    0xFF777777
            );
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance()
                .setScreen(
                        parent
                );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
