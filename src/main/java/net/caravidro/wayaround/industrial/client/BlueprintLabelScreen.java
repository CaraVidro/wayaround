package net.caravidro.wayaround.industrial.client;

import net.caravidro.wayaround.network.BlueprintLabelC2SPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BlueprintLabelScreen
        extends Screen {

    private final String blueprintId;
    private final String defaultTitle;

    private EditBox titleBox;
    private Button coordsButton;
    private Button dateButton;

    private boolean showCoordinates;
    private boolean showDateTime;

    public BlueprintLabelScreen(
            String blueprintId,
            String defaultTitle,
            boolean showCoordinates,
            boolean showDateTime
    ) {
        super(
                Component.translatable(
                        "screen.wayaround.blueprint.label_title"
                )
        );

        this.blueprintId =
                blueprintId;

        this.defaultTitle =
                defaultTitle;

        this.showCoordinates =
                showCoordinates;

        this.showDateTime =
                showDateTime;
    }

    @Override
    protected void init() {
        super.init();

        int center =
                width / 2;

        titleBox =
                new EditBox(
                        font,
                        center - 110,
                        height / 2 - 42,
                        220,
                        20,
                        Component.translatable(
                                "screen.wayaround.blueprint.name"
                        )
                );

        titleBox.setMaxLength(
                40
        );

        titleBox.setValue(
                defaultTitle == null
                        ? ""
                        : defaultTitle
        );

        addRenderableWidget(
                titleBox
        );

        coordsButton =
                addRenderableWidget(
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
                                        height / 2 - 12,
                                        220,
                                        20
                                )
                                .build()
                );

        dateButton =
                addRenderableWidget(
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
                                        height / 2 + 14,
                                        220,
                                        20
                                )
                                .build()
                );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "screen.wayaround.blueprint.done"
                                ),
                                button -> finish()
                        )
                        .bounds(
                                center - 70,
                                height / 2 + 50,
                                140,
                                20
                        )
                        .build()
        );

        setInitialFocus(
                titleBox
        );
    }

    private Component coordsLabel() {
        return Component.translatable(
                "screen.wayaround.blueprint.show_coords",
                Component.translatable(
                        showCoordinates
                                ? "screen.wayaround.blueprint.yes"
                                : "screen.wayaround.blueprint.no"
                )
        );
    }

    private Component dateLabel() {
        return Component.translatable(
                "screen.wayaround.blueprint.show_datetime",
                Component.translatable(
                        showDateTime
                                ? "screen.wayaround.blueprint.yes"
                                : "screen.wayaround.blueprint.no"
                )
        );
    }

    private void finish() {
        PacketDistributor.sendToServer(
                new BlueprintLabelC2SPayload(
                        blueprintId,
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
        if (minecraft != null) {
            minecraft.setScreen(
                    null
            );
        }
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

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                34,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.wayaround.blueprint.subtitle"
                ),
                width / 2,
                48,
                0x79BFEA
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }
}
