package net.caravidro.wayaround.client.worldconfig;

import java.util.ArrayList;
import java.util.List;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureCategory;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeatureSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public final class WorldFeatureSelectionScreen
        extends Screen {

    private final CreateWorldScreen parent;

    private final WorldFeatureSettings settings =
            WorldFeatureSettings.allEnabled();

    private WorldFeatureCategory category =
            WorldFeatureCategory.POWERS;

    private final List<Button> featureButtons =
            new ArrayList<>();

    public WorldFeatureSelectionScreen(
            CreateWorldScreen parent
    ) {
        super(
                Component.literal(
                        "Way Around - World Modifications"
                )
        );

        this.parent =
                parent;
    }

    @Override
    protected void init() {
        rebuildControls();
    }

    private void rebuildControls() {
        clearWidgets();
        featureButtons.clear();

        int margin =
                10;

        int tabs =
                WorldFeatureCategory.values()
                        .length;

        int tabGap =
                3;

        int tabWidth =
                Math.max(
                        48,
                        Math.min(
                                104,
                                (
                                        width
                                                - margin * 2
                                                - tabGap
                                                * (
                                                tabs - 1
                                        )
                                )
                                        / tabs
                        )
                );

        int totalTabs =
                tabWidth
                        * tabs
                        + tabGap
                        * (
                        tabs - 1
                );

        int tabX =
                (
                        width
                                - totalTabs
                )
                        / 2;

        int tabY =
                50;

        int index =
                0;

        for (WorldFeatureCategory value :
                WorldFeatureCategory.values()) {
            final WorldFeatureCategory selected =
                    value;

            Button button =
                    Button.builder(
                            Component.literal(
                                    shortCategory(
                                            value
                                    )
                            ).withStyle(
                                    value == category
                                            ? ChatFormatting.AQUA
                                            : ChatFormatting.WHITE
                            ),
                            ignored -> {
                                category =
                                        selected;
                                rebuildControls();
                            }
                    ).bounds(
                            tabX
                                    + index
                                    * (
                                    tabWidth
                                            + tabGap
                            ),
                            tabY,
                            tabWidth,
                            20
                    ).build();

            button.setTooltip(
                    Tooltip.create(
                            Component.literal(
                                    value.title()
                            )
                    )
            );

            addRenderableWidget(
                    button
            );

            index++;
        }

        int listWidth =
                Math.min(
                        430,
                        Math.max(
                                230,
                                width - 60
                        )
                );

        int x =
                (
                        width - listWidth
                )
                        / 2;

        int y =
                91;

        int featureCount =
                0;

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (feature.category()
                    == category) {
                featureCount++;
            }
        }

        /*
         * Categories with many switches use two columns. This keeps the
         * mandatory setup usable even at Minecraft's small GUI heights.
         */
        int columns =
                featureCount > 4
                        ? 2
                        : 1;

        int columnGap =
                6;

        int buttonWidth =
                columns == 1
                        ? listWidth
                        : (
                        listWidth
                                - columnGap
                ) / 2;

        int entry =
                0;

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (feature.category()
                    != category) {
                continue;
            }

            final WorldFeature target =
                    feature;

            int column =
                    entry
                            % columns;

            int row =
                    entry
                            / columns;

            Button button =
                    Button.builder(
                            featureLabel(
                                    feature
                            ),
                            ignored -> {
                                if (target
                                        == WorldFeature.WATER_DYNAMICS) {
                                    settings.cycleWaveMode();
                                    rebuildControls();
                                    return;
                                }

                                settings.toggle(
                                        target
                                );

                                refreshFeatureMessages();
                            }
                    ).bounds(
                            x
                                    + column
                                    * (
                                    buttonWidth
                                            + columnGap
                            ),
                            y + row * 24,
                            buttonWidth,
                            20
                    ).build();

            button.setTooltip(
                    Tooltip.create(
                            feature
                                    == WorldFeature.WATER_DYNAMICS
                                    ? Component.literal(
                                    feature.description()
                                            + "\nMode: "
                                            + settings.waveMode()
                                            .title()
                                            + "\n"
                                            + settings.waveMode()
                                            .description()
                            )
                                    : Component.literal(
                                    feature.description()
                            ).append(
                                    dependencyText(
                                            feature
                                    )
                            )
                    )
            );

            featureButtons.add(
                    button
            );

            addRenderableWidget(
                    button
            );

            entry++;
        }

        int bottomY =
                height - 28;

        int smallWidth =
                Math.min(
                        104,
                        Math.max(
                                76,
                                (
                                        width - 250
                                ) / 3
                        )
                );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "Enable all"
                        ),
                        ignored -> {
                            settings.setAll(
                                    true
                            );

                            refreshFeatureMessages();
                        }
                ).bounds(
                        10,
                        bottomY,
                        smallWidth,
                        20
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "Disable all"
                        ),
                        ignored -> {
                            settings.setAll(
                                    false
                            );

                            refreshFeatureMessages();
                        }
                ).bounds(
                        14 + smallWidth,
                        bottomY,
                        smallWidth,
                        20
                ).build()
        );

        int createWidth =
                Math.min(
                        180,
                        Math.max(
                                120,
                                width / 4
                        )
                );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "CREATE WORLD"
                        ).withStyle(
                                ChatFormatting.GREEN,
                                ChatFormatting.BOLD
                        ),
                        ignored ->
                                WorldFeatureCreationFlow.createWorld(
                                        parent,
                                        settings
                                )
                ).bounds(
                        width
                                - createWidth
                                - 10,
                        bottomY,
                        createWidth,
                        20
                ).build()
        );
    }

    private void refreshFeatureMessages() {
        int index =
                0;

        for (WorldFeature feature :
                WorldFeature.values()) {
            if (feature.category()
                    != category) {
                continue;
            }

            if (index
                    >= featureButtons.size()) {
                break;
            }

            featureButtons.get(
                    index
            ).setMessage(
                    featureLabel(
                            feature
                    )
            );

            index++;
        }
    }

    private Component featureLabel(
            WorldFeature feature
    ) {
        if (feature
                == WorldFeature.WATER_DYNAMICS) {
            WaveMode mode =
                    settings.waveMode();

            ChatFormatting color =
                    switch (mode) {
                        case OFF ->
                                ChatFormatting.DARK_GRAY;

                        case STYLIZED ->
                                ChatFormatting.GOLD;

                        case REALISTIC ->
                                ChatFormatting.AQUA;
                    };

            return Component.literal(
                    "[ "
                            + mode.title()
                            .toUpperCase(
                                    java.util.Locale.ROOT
                            )
                            + " ] "
                            + feature.title()
            ).withStyle(
                    color
            );
        }

        boolean raw =
                settings.rawEnabled(
                        feature
                );

        boolean effective =
                settings.enabled(
                        feature
                );

        ChatFormatting color =
                !raw
                        ? ChatFormatting.DARK_GRAY
                        : effective
                        ? ChatFormatting.GREEN
                        : ChatFormatting.YELLOW;

        String state =
                !raw
                        ? "[ OFF ] "
                        : effective
                        ? "[  ON  ] "
                        : "[WAIT] ";

        return Component.literal(
                state
                        + feature.title()
        ).withStyle(
                color
        );
    }

    private Component dependencyText(
            WorldFeature feature
    ) {
        return switch (feature) {
            case DOMAINS,
                 TUKUNA_SYSTEM,
                 IMMORTAL_WHEEL,
                 THERMAL_SYSTEM ->
                    Component.literal(
                            "\nRequires: Spectrums"
                    ).withStyle(
                            ChatFormatting.YELLOW
                    );

            case ASSEMBLY,
                 POWER_NETWORKS,
                 SHIPS ->
                    Component.literal(
                            "\nRequires: Industrial Machines"
                    ).withStyle(
                            ChatFormatting.YELLOW
                    );

            default ->
                    Component.empty();
        };
    }

    private static String shortCategory(
            WorldFeatureCategory category
    ) {
        return switch (category) {
            case POWERS ->
                    "Powers";
            case WORLD ->
                    "World";
            case INDUSTRY ->
                    "Industry";
            case ITEMS ->
                    "Items";
            case SYSTEMS ->
                    "Systems";
        };
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
                Component.literal(
                        "WAY AROUND"
                ).withStyle(
                        ChatFormatting.LIGHT_PURPLE,
                        ChatFormatting.BOLD
                ),
                width / 2,
                14,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                "Choose what is allowed to exist in this world. Saved per-world.",
                width / 2,
                29,
                0xFFAAAAAA
        );

        graphics.drawCenteredString(
                font,
                Component.literal(
                        category.title()
                ).withStyle(
                        ChatFormatting.AQUA
                ),
                width / 2,
                77,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                settings.enabledCount()
                        + " / "
                        + WorldFeature.values().length
                        + " systems enabled",
                width / 2,
                height - 43,
                0xFFB7B7B7
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(
                parent
        );
    }
}
