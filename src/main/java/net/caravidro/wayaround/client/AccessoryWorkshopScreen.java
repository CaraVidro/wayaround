package net.caravidro.wayaround.client;

import net.caravidro.wayaround.accessory.AccessoryCustomizationData;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.network.AccessoryWorkshopApplyC2SPayload;
import net.caravidro.wayaround.network.AccessoryWorkshopOpenS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AccessoryWorkshopScreen
        extends Screen {

    private final AccessoryWorkshopOpenS2CPayload source;
    private final AccessoryKind kind;

    private int material;
    private int size;
    private int extras;
    private int woolColor;

    private Button materialButton;
    private Button sizeButton;
    private Button gearsButton;
    private Button clockButton;
    private Button woolButton;

    public AccessoryWorkshopScreen(
            AccessoryWorkshopOpenS2CPayload source
    ) {
        super(
                Component.translatable(
                        "accessory.workshop.title"
                )
        );

        this.source =
                source;

        kind =
                AccessoryKind.byPath(
                        source.kind()
                );

        material =
                AccessoryCustomizationData.clampMaterial(
                        source.material()
                );

        size =
                AccessoryCustomizationData.clampSize(
                        source.size()
                );

        extras =
                source.extras();

        woolColor =
                AccessoryCustomizationData.clampWoolColor(
                        source.woolColor()
                );
    }

    @Override
    protected void init() {
        int center =
                width / 2;

        int left =
                center - 102;

        int y =
                height / 2 - 72;

        materialButton =
                addRenderableWidget(
                        Button.builder(
                                        materialLabel(),
                                        button -> {
                                            material =
                                                    (material + 1)
                                                            % AccessoryCustomizationData.MATERIAL_COUNT;
                                            refresh();
                                        }
                                )
                                .bounds(
                                        left,
                                        y,
                                        204,
                                        20
                                )
                                .build()
                );

        sizeButton =
                addRenderableWidget(
                        Button.builder(
                                        sizeLabel(),
                                        button -> {
                                            size =
                                                    (size + 1)
                                                            % AccessoryCustomizationData.SIZE_STEPS;
                                            refresh();
                                        }
                                )
                                .bounds(
                                        left,
                                        y + 24,
                                        204,
                                        20
                                )
                                .build()
                );

        if (kind == AccessoryKind.ENGINEER_CAP) {
            gearsButton =
                    addRenderableWidget(
                            Button.builder(
                                            gearsLabel(),
                                            button -> {
                                                extras ^=
                                                        AccessoryCustomizationData.EXTRA_GEARS;
                                                refresh();
                                            }
                                    )
                                    .bounds(
                                            left,
                                            y + 48,
                                            99,
                                            20
                                    )
                                    .build()
                    );

            clockButton =
                    addRenderableWidget(
                            Button.builder(
                                            clockLabel(),
                                            button -> {
                                                extras ^=
                                                        AccessoryCustomizationData.EXTRA_CLOCK;
                                                refresh();
                                            }
                                    )
                                    .bounds(
                                            left + 105,
                                            y + 48,
                                            99,
                                            20
                                    )
                                    .build()
                    );
        }

        if (kind == AccessoryKind.SOMBRERO) {
            woolButton =
                    addRenderableWidget(
                            Button.builder(
                                            woolLabel(),
                                            button -> {
                                                woolColor =
                                                        (woolColor + 1)
                                                                % AccessoryCustomizationData.WOOL_COLOR_COUNT;
                                                refresh();
                                            }
                                    )
                                    .bounds(
                                            left,
                                            y + 48,
                                            204,
                                            20
                                    )
                                    .build()
                    );
        }

        int bottom =
                height / 2 + 66;

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "accessory.workshop.apply"
                                ),
                                button -> apply()
                        )
                        .bounds(
                                left,
                                bottom,
                                99,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "gui.cancel"
                                ),
                                button -> onClose()
                        )
                        .bounds(
                                left + 105,
                                bottom,
                                99,
                                20
                        )
                        .build()
        );
    }

    private void refresh() {
        if (materialButton != null) {
            materialButton.setMessage(
                    materialLabel()
            );
        }

        if (sizeButton != null) {
            sizeButton.setMessage(
                    sizeLabel()
            );
        }

        if (gearsButton != null) {
            gearsButton.setMessage(
                    gearsLabel()
            );
        }

        if (clockButton != null) {
            clockButton.setMessage(
                    clockLabel()
            );
        }

        if (woolButton != null) {
            woolButton.setMessage(
                    woolLabel()
            );
        }
    }

    private Component materialLabel() {
        return Component.translatable(
                "accessory.workshop.material",
                Component.translatable(
                        AccessoryCustomizationData.materialNameKey(
                                material
                        )
                )
        );
    }

    private Component sizeLabel() {
        String key =
                kind == AccessoryKind.ENGINEER_CAP
                        ? "accessory.workshop.height"
                        : kind == AccessoryKind.SOMBRERO
                        ? "accessory.workshop.brim"
                        : "accessory.workshop.size";

        return Component.translatable(
                key,
                size + 1
        );
    }

    private Component gearsLabel() {
        return Component.translatable(
                "accessory.workshop.gears",
                Component.translatable(
                        (extras
                                & AccessoryCustomizationData.EXTRA_GEARS)
                                != 0
                                ? "options.on"
                                : "options.off"
                )
        );
    }

    private Component clockLabel() {
        return Component.translatable(
                "accessory.workshop.clock",
                Component.translatable(
                        (extras
                                & AccessoryCustomizationData.EXTRA_CLOCK)
                                != 0
                                ? "options.on"
                                : "options.off"
                )
        );
    }

    private Component woolLabel() {
        return Component.translatable(
                "accessory.workshop.wool",
                Component.translatable(
                        AccessoryCustomizationData.woolNameKey(
                                woolColor
                        )
                )
        );
    }

    private void apply() {
        PacketDistributor.sendToServer(
                new AccessoryWorkshopApplyC2SPayload(
                        source.blockPos(),
                        source.hand(),
                        source.kind(),
                        material,
                        size,
                        extras,
                        woolColor
                )
        );

        onClose();
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

        int center =
                width / 2;

        int top =
                height / 2 - 108;

        graphics.fill(
                center - 118,
                top,
                center + 118,
                height / 2 + 98,
                0xD8181511
        );

        graphics.fill(
                center - 118,
                top,
                center - 114,
                height / 2 + 98,
                0xFFC17A32
        );

        graphics.drawCenteredString(
                font,
                title,
                center,
                top + 12,
                0xFFF4D7A1
        );

        Component template =
                Component.translatable(
                        "accessory.workshop.template",
                        kind == null
                                ? Component.literal(
                                source.kind()
                        )
                                : Component.translatable(
                                kind.translationKey()
                        )
                );

        graphics.drawCenteredString(
                font,
                template,
                center,
                top + 31,
                0xFFD1C1A3
        );

        ItemStack stack =
                ItemStack.EMPTY;

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null) {
            InteractionHand hand =
                    source.hand() == 1
                            ? InteractionHand.OFF_HAND
                            : InteractionHand.MAIN_HAND;

            stack =
                    minecraft.player.getItemInHand(
                            hand
                    );
        }

        if (!stack.isEmpty()) {
            graphics.renderItem(
                    stack,
                    center - 8,
                    top + 45
            );
        }

        if (kind == AccessoryKind.ENGINEER_CAP) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "accessory.workshop.top_hat_hint"
                    ),
                    center,
                    height / 2 + 37,
                    0xFF9C8B73
            );
        } else if (kind == AccessoryKind.SOMBRERO) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "accessory.workshop.sombrero_hint"
                    ),
                    center,
                    height / 2 + 37,
                    0xFF9C8B73
            );
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
