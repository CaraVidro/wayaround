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
        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.done"),
                                button -> onClose()
                        )
                        .bounds(
                                width / 2 - 75,
                                height - 32,
                                150,
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
        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                24,
                0xFFF0E2C0
        );

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.wayaround.update_log.subtitle"
                ),
                width / 2,
                39,
                0xFF9C8058
        );

        int boxLeft =
                Math.max(
                        18,
                        width / 2 - 190
                );

        int boxRight =
                Math.min(
                        width - 18,
                        width / 2 + 190
                );

        int top = 58;

        graphics.fill(
                boxLeft,
                top,
                boxRight,
                height - 46,
                0xA0181818
        );

        int y = top + 14;

        for (Component line : WayAroundReleaseInfo.updateLogLines()) {
            graphics.drawString(
                    font,
                    line,
                    boxLeft + 14,
                    y,
                    0xFFD6D6D6,
                    false
            );

            y += 16;
        }

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.wayaround.update_log.release_note"
                ),
                width / 2,
                height - 51,
                0xFF777777
        );
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
