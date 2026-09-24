package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.network.LabelRecordingC2SPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TapeLabelScreen
        extends Screen {

    private final String recordingId;
    private final String defaultTitle;
    private final boolean vhs;

    private EditBox titleBox;
    private Button coordsButton;
    private Button dateButton;

    private boolean showCoordinates;
    private boolean showDateTime;

    public TapeLabelScreen(
            String recordingId,
            String defaultTitle,
            boolean vhs
    ) {
        super(
                Component.translatable(
                        "screen.wayaround.media.label_title"
                )
        );

        this.recordingId =
                recordingId;

        this.defaultTitle =
                defaultTitle;

        this.vhs =
                vhs;
    }

    @Override
    protected void init() {
        super.init();

        int center =
                this.width / 2;

        titleBox =
                new EditBox(
                        this.font,
                        center - 110,
                        this.height / 2 - 42,
                        220,
                        20,
                        Component.translatable(
                                "screen.wayaround.media.label_name"
                        )
                );

        titleBox.setMaxLength(32);
        titleBox.setValue("");
        titleBox.setHint(
                Component.literal(
                        defaultTitle
                )
        );

        this.addRenderableWidget(
                titleBox
        );

        coordsButton =
                this.addRenderableWidget(
                        Button.builder(
                                        coordsLabel(),
                                        button -> {
                                            showCoordinates =
                                                    !showCoordinates;

                                            button.setMessage(
                                                    coordsLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        center - 110,
                                        this.height / 2 - 12,
                                        220,
                                        20
                                )
                                .build()
                );

        dateButton =
                this.addRenderableWidget(
                        Button.builder(
                                        dateLabel(),
                                        button -> {
                                            showDateTime =
                                                    !showDateTime;

                                            button.setMessage(
                                                    dateLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        center - 110,
                                        this.height / 2 + 14,
                                        220,
                                        20
                                )
                                .build()
                );

        this.addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "screen.wayaround.media.done"
                                ),
                                button -> finish()
                        )
                        .bounds(
                                center - 70,
                                this.height / 2 + 50,
                                140,
                                20
                        )
                        .build()
        );

        this.setInitialFocus(
                titleBox
        );
    }

    private Component coordsLabel() {
        return Component.translatable(
                "screen.wayaround.media.show_coords",
                Component.translatable(
                        showCoordinates
                                ? "screen.wayaround.media.yes"
                                : "screen.wayaround.media.no"
                )
        );
    }

    private Component dateLabel() {
        return Component.translatable(
                "screen.wayaround.media.show_datetime",
                Component.translatable(
                        showDateTime
                                ? "screen.wayaround.media.yes"
                                : "screen.wayaround.media.no"
                )
        );
    }

    private void finish() {
        PacketDistributor.sendToServer(
                new LabelRecordingC2SPayload(
                        recordingId,
                        titleBox == null
                                ? ""
                                : titleBox.getValue(),
                        showCoordinates,
                        showDateTime
                )
        );

        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                34,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        vhs
                                ? "VHS"
                                : "FILM ROLL"
                ),
                this.width / 2,
                48,
                0x909090
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }
}
