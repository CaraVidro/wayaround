package net.caravidro.wayaround.industrial.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.caravidro.wayaround.industrial.engineering.EngineeringBlockProfile;
import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph;
import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.Node;
import net.caravidro.wayaround.industrial.engineering.EngineeringCalculationGraph.NodeType;
import net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class EngineeringWorkbenchScreen
        extends AbstractContainerScreen<EngineeringWorkbenchMenu> {

    private static final int CATALOG_WIDTH =
            126;

    private static final int PROPERTY_WIDTH =
            166;

    private static final int TOP =
            28;

    private static final int BOTTOM_TOOLBAR_HEIGHT =
            54;

    private static final int NODE_WIDTH =
            104;

    private static final int NODE_HEADER =
            15;

    private static final int NODE_INPUT_SPACING =
            10;

    private static final int CATALOG_ROW =
            21;

    private static final List<NodeType> PALETTE =
            List.of(
                    NodeType.CONSTANT,
                    NodeType.ADD,
                    NodeType.SUBTRACT,
                    NodeType.MULTIPLY,
                    NodeType.DIVIDE,
                    NodeType.PYTHAGORAS,
                    NodeType.RECTANGLE_AREA,
                    NodeType.BOX_VOLUME,
                    NodeType.CIRCLE_AREA,
                    NodeType.SLOPE_DEGREES,
                    NodeType.SAFETY_FACTOR,
                    NodeType.STRESS,
                    NodeType.BEAM_UDL_MOMENT,
                    NodeType.BEAM_CENTER_MOMENT
            );

    private final EngineeringCalculationGraph graph =
            new EngineeringCalculationGraph();

    private final List<EngineeringBlockProfile> catalog =
            new ArrayList<>();

    private final List<EngineeringBlockProfile> filtered =
            new ArrayList<>();

    private final Map<Integer, EngineeringBlockProfile> blockNodes =
            new HashMap<>();

    private final Map<Integer, String> blockProperties =
            new HashMap<>();

    private EditBox searchBox;
    private EditBox valueBox;

    private boolean recommendedOnly =
            true;

    private boolean visualMode =
            true;

    private int catalogScroll;
    private int propertyScroll;

    private int selectedNodeId =
            -1;

    private int pendingSourceId =
            -1;

    private int draggingNodeId =
            -1;

    private int dragOffsetX;
    private int dragOffsetY;

    private boolean syncingValueBox;

    public EngineeringWorkbenchScreen(
            EngineeringWorkbenchMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );

        imageWidth =
                580;

        imageHeight =
                318;
    }

    @Override
    protected void init() {
        super.init();

        buildCatalog();

        searchBox =
                new EditBox(
                        font,
                        leftPos + 8,
                        topPos + 31,
                        CATALOG_WIDTH - 16,
                        18,
                        Component.translatable(
                                "container.wayaround.engineering.search"
                        )
                );

        searchBox.setHint(
                Component.translatable(
                        "container.wayaround.engineering.search_hint"
                )
        );

        searchBox.setResponder(
                value -> {
                    catalogScroll =
                            0;

                    refreshFilter();
                }
        );

        addRenderableWidget(
                searchBox
        );

        valueBox =
                new EditBox(
                        font,
                        leftPos + imageWidth - PROPERTY_WIDTH + 10,
                        topPos + imageHeight - 27,
                        PROPERTY_WIDTH - 20,
                        18,
                        Component.translatable(
                                "container.wayaround.engineering.value"
                        )
                );

        valueBox.setVisible(
                false
        );

        valueBox.setResponder(
                value -> updateLiteralFromEditor(
                        value
                )
        );

        addRenderableWidget(
                valueBox
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "container.wayaround.engineering.recommended"
                                ),
                                button -> {
                                    recommendedOnly =
                                            !recommendedOnly;

                                    button.setMessage(
                                            Component.translatable(
                                                    recommendedOnly
                                                            ? "container.wayaround.engineering.recommended"
                                                            : "container.wayaround.engineering.all_blocks"
                                            )
                                    );

                                    catalogScroll =
                                            0;

                                    refreshFilter();
                                }
                        )
                        .bounds(
                                leftPos + 8,
                                topPos + 7,
                                112,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "container.wayaround.engineering.visual"
                                ),
                                button -> {
                                    visualMode =
                                            !visualMode;

                                    button.setMessage(
                                            Component.translatable(
                                                    visualMode
                                                            ? "container.wayaround.engineering.visual"
                                                            : "container.wayaround.engineering.graph_only"
                                            )
                                    );
                                }
                        )
                        .bounds(
                                leftPos + CATALOG_WIDTH + 9,
                                topPos + 7,
                                88,
                                20
                        )
                        .build()
        );

        /*
         * A new project is not a totally empty void. One meter starts as the
         * simplest possible geometric idea: a point carrying the value 1.
         */
        if (graph.nodes()
                .isEmpty()) {
            Node starter =
                    graph.add(
                            NodeType.CONSTANT,
                            28,
                            132,
                            "1 m reference",
                            1.0,
                            "m"
                    );

            selectNode(
                    starter.id()
            );
        }

        refreshFilter();
    }

    private void buildCatalog() {
        catalog.clear();

        for (Block block :
                BuiltInRegistries.BLOCK) {

            if (!EngineeringBlockProfile.catalogVisible(
                    block
            )) {
                continue;
            }

            catalog.add(
                    EngineeringBlockProfile.inspect(
                            block
                    )
            );
        }

        catalog.sort(
                Comparator
                        .comparingInt(
                                this::recommendedRank
                        )
                        .thenComparing(
                                profile -> displayName(
                                        profile.block()
                                ),
                                String.CASE_INSENSITIVE_ORDER
                        )
        );
    }

    private void refreshFilter() {
        filtered.clear();

        String query =
                searchBox == null
                        ? ""
                        : searchBox.getValue()
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );

        for (EngineeringBlockProfile profile :
                catalog) {

            if (recommendedOnly
                    && recommendedRank(
                    profile
            ) >= 1000) {
                continue;
            }

            String name =
                    displayName(
                            profile.block()
                    )
                            .toLowerCase(
                                    Locale.ROOT
                            );

            String id =
                    profile.id()
                            .toString()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (!query.isEmpty()
                    && !name.contains(
                    query
            )
                    && !id.contains(
                    query
            )) {
                continue;
            }

            filtered.add(
                    profile
            );
        }

        int maximum =
                Math.max(
                        0,
                        filtered.size()
                                - visibleCatalogRows()
                );

        catalogScroll =
                Math.clamp(
                        catalogScroll,
                        0,
                        maximum
                );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x =
                leftPos;

        int y =
                topPos;

        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + imageHeight,
                0xFF171A1E
        );

        graphics.fill(
                x + 3,
                y + 3,
                x + imageWidth - 3,
                y + imageHeight - 3,
                0xFF242A30
        );

        renderCatalog(
                graphics,
                mouseX,
                mouseY
        );

        renderCanvas(
                graphics,
                mouseX,
                mouseY
        );

        renderProperties(
                graphics,
                mouseX,
                mouseY
        );

        renderPalette(
                graphics,
                mouseX,
                mouseY
        );
    }

    private void renderCatalog(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x =
                leftPos;

        int y =
                topPos;

        graphics.fill(
                x + 4,
                y + TOP,
                x + CATALOG_WIDTH,
                y + imageHeight - 4,
                0xFF1B2026
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.catalog"
                ),
                x + 9,
                y + 53,
                0xFFBFD6E5,
                false
        );

        int startY =
                y + 66;

        int rows =
                visibleCatalogRows();

        for (int row =
                     0;
             row < rows;
             row++) {

            int index =
                    catalogScroll
                            + row;

            if (index >= filtered.size()) {
                break;
            }

            EngineeringBlockProfile profile =
                    filtered.get(
                            index
                    );

            int rowY =
                    startY
                            + row
                                    * CATALOG_ROW;

            boolean hovered =
                    inside(
                            mouseX,
                            mouseY,
                            x + 7,
                            rowY,
                            CATALOG_WIDTH - 14,
                            CATALOG_ROW - 2
                    );

            graphics.fill(
                    x + 7,
                    rowY,
                    x + CATALOG_WIDTH - 7,
                    rowY + CATALOG_ROW - 2,
                    hovered
                            ? 0xFF3A4B58
                            : 0xFF2A323A
            );

            ItemStack stack =
                    new ItemStack(
                            profile.block()
                                    .asItem()
                    );

            if (!stack.isEmpty()) {
                graphics.renderItem(
                        stack,
                        x + 9,
                        rowY + 1
                );
            }

            String name =
                    trim(
                            displayName(
                                    profile.block()
                            ),
                            14
                    );

            graphics.drawString(
                    font,
                    name,
                    x + 28,
                    rowY + 5,
                    hovered
                            ? 0xFFFFFFFF
                            : 0xFFD0D7DC,
                    false
            );
        }

        if (filtered.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.engineering.no_results"
                    ),
                    x + 10,
                    startY + 8,
                    0xFF8D9BA4,
                    false
            );
        }
    }

    private void renderCanvas(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x0 =
                canvasX();

        int y0 =
                canvasY();

        int x1 =
                canvasX()
                        + canvasWidth();

        int y1 =
                canvasY()
                        + canvasHeight();

        graphics.fill(
                x0,
                y0,
                x1,
                y1,
                0xFF101419
        );

        for (int x =
                     x0;
             x < x1;
             x += 12) {
            for (int y =
                         y0;
                 y < y1;
                 y += 12) {

                graphics.fill(
                        x,
                        y,
                        x + 1,
                        y + 1,
                        0xFF28313A
                );
            }
        }

        renderConnections(
                graphics
        );

        for (Node node :
                graph.nodes()) {
            renderNode(
                    graphics,
                    node,
                    mouseX,
                    mouseY
            );
        }

        if (visualMode) {
            renderVisualPreview(
                    graphics
            );
        }

        if (pendingSourceId >= 0) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.engineering.connect_hint"
                    ),
                    x0 + 8,
                    y1 - 12,
                    0xFFFFC45B,
                    false
            );
        }
    }

    private void renderConnections(
            GuiGraphics graphics
    ) {
        for (Node target :
                graph.nodes()) {

            for (int slot =
                         0;
                 slot < target.inputCount();
                 slot++) {

                Node source =
                        graph.node(
                                target.input(
                                        slot
                                )
                        );

                if (source == null) {
                    continue;
                }

                int sx =
                        nodeScreenX(
                                source
                        )
                                + NODE_WIDTH;

                int sy =
                        nodeScreenY(
                                source
                        )
                                + nodeHeight(
                                source
                        )
                                / 2;

                int tx =
                        nodeScreenX(
                                target
                        );

                int ty =
                        inputPortY(
                                target,
                                slot
                        );

                int middle =
                        (sx + tx)
                                / 2;

                drawLine(
                        graphics,
                        sx,
                        sy,
                        middle,
                        sy,
                        0xFF66C8FF
                );

                drawLine(
                        graphics,
                        middle,
                        sy,
                        middle,
                        ty,
                        0xFF66C8FF
                );

                drawLine(
                        graphics,
                        middle,
                        ty,
                        tx,
                        ty,
                        0xFF66C8FF
                );
            }
        }
    }

    private void renderNode(
            GuiGraphics graphics,
            Node node,
            int mouseX,
            int mouseY
    ) {
        int x =
                nodeScreenX(
                        node
                );

        int y =
                nodeScreenY(
                        node
                );

        int height =
                nodeHeight(
                        node
                );

        boolean selected =
                node.id()
                        == selectedNodeId;

        graphics.fill(
                x,
                y,
                x + NODE_WIDTH,
                y + height,
                selected
                        ? 0xFF40566A
                        : 0xFF29333D
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + NODE_WIDTH - 1,
                y + NODE_HEADER,
                selected
                        ? 0xFF52718A
                        : 0xFF374550
        );

        graphics.drawString(
                font,
                trim(
                        node.label(),
                        16
                ),
                x + 5,
                y + 4,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                font,
                "×",
                x + NODE_WIDTH - 10,
                y + 4,
                0xFFFF8080,
                false
        );

        String result =
                formatNumber(
                        node.result()
                )
                        + (
                        node.unit()
                                .isEmpty()
                                ? ""
                                : " "
                                        + node.unit()
                );

        graphics.drawString(
                font,
                trim(
                        result,
                        16
                ),
                x + 6,
                y + height - 11,
                Double.isFinite(
                        node.result()
                )
                        ? 0xFF8FE38F
                        : 0xFFFF7474,
                false
        );

        for (int slot =
                     0;
             slot < node.inputCount();
             slot++) {

            int portY =
                    inputPortY(
                            node,
                            slot
                    );

            graphics.fill(
                    x - 2,
                    portY - 2,
                    x + 3,
                    portY + 3,
                    node.input(
                            slot
                    ) >= 0
                            ? 0xFF66C8FF
                            : 0xFF78848E
            );

            graphics.drawString(
                    font,
                    inputLabel(
                            node.type(),
                            slot
                    ),
                    x + 6,
                    portY - 4,
                    0xFFB9C3CA,
                    false
            );
        }

        graphics.fill(
                x + NODE_WIDTH - 2,
                y + height / 2 - 2,
                x + NODE_WIDTH + 3,
                y + height / 2 + 3,
                pendingSourceId
                        == node.id()
                        ? 0xFFFFC45B
                        : 0xFF8FE38F
        );
    }

    private void renderProperties(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x =
                leftPos
                        + imageWidth
                        - PROPERTY_WIDTH;

        int y =
                topPos;

        graphics.fill(
                x,
                y + TOP,
                leftPos + imageWidth - 4,
                y + imageHeight - 4,
                0xFF1B2026
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.properties"
                ),
                x + 8,
                y + 35,
                0xFFBFD6E5,
                false
        );

        Node selected =
                graph.node(
                        selectedNodeId
                );

        if (selected == null) {
            valueBox.setVisible(
                    false
            );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.engineering.select_hint"
                    ),
                    x + 8,
                    y + 52,
                    0xFF8998A3,
                    false
            );

            return;
        }

        EngineeringBlockProfile profile =
                blockNodes.get(
                        selected.id()
                );

        if (profile != null) {
            valueBox.setVisible(
                    false
            );

            renderBlockProperties(
                    graphics,
                    selected,
                    profile,
                    x + 7,
                    y + 50,
                    mouseX,
                    mouseY
            );

        } else {
            renderCalculationProperties(
                    graphics,
                    selected,
                    x + 8,
                    y + 53
            );
        }
    }

    private void renderBlockProperties(
            GuiGraphics graphics,
            Node node,
            EngineeringBlockProfile profile,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        ItemStack stack =
                new ItemStack(
                        profile.block()
                                .asItem()
                );

        if (!stack.isEmpty()) {
            graphics.renderItem(
                    stack,
                    x,
                    y
            );
        }

        graphics.drawString(
                font,
                trim(
                        displayName(
                                profile.block()
                        ),
                        19
                ),
                x + 20,
                y + 4,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                font,
                profile.id()
                        .toString(),
                x,
                y + 21,
                0xFF71808C,
                false
        );

        int listY =
                y + 35;

        int available =
                imageHeight
                        - 96;

        int rows =
                Math.max(
                        1,
                        available / 22
                );

        List<EngineeringBlockProfile.Property> properties =
                profile.properties();

        propertyScroll =
                Math.clamp(
                        propertyScroll,
                        0,
                        Math.max(
                                0,
                                properties.size()
                                        - rows
                        )
                );

        for (int row =
                     0;
             row < rows;
             row++) {

            int index =
                    propertyScroll
                            + row;

            if (index >= properties.size()) {
                break;
            }

            EngineeringBlockProfile.Property property =
                    properties.get(
                            index
                    );

            int rowY =
                    listY
                            + row * 22;

            boolean active =
                    property.key()
                            .equals(
                                    blockProperties.get(
                                            node.id()
                                    )
                            );

            boolean hovered =
                    inside(
                            mouseX,
                            mouseY,
                            x,
                            rowY,
                            PROPERTY_WIDTH - 18,
                            20
                    );

            graphics.fill(
                    x,
                    rowY,
                    x + PROPERTY_WIDTH - 18,
                    rowY + 20,
                    active
                            ? 0xFF405F4A
                            : hovered
                                    ? 0xFF35414A
                                    : property.special()
                                            ? 0xFF302E3C
                                            : 0xFF262D33
            );

            graphics.drawString(
                    font,
                    trim(
                            property.label(),
                            20
                    ),
                    x + 4,
                    rowY + 3,
                    property.special()
                            ? 0xFFE2B8FF
                            : 0xFFCFD7DC,
                    false
            );

            String value =
                    property.display()
                            + (
                            property.unit()
                                    .isEmpty()
                                    ? ""
                                    : " "
                                            + property.unit()
                    );

            graphics.drawString(
                    font,
                    trim(
                            value,
                            20
                    ),
                    x + 4,
                    rowY + 11,
                    property.numeric()
                            ? 0xFF8FE38F
                            : 0xFF96A5AF,
                    false
            );
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.property_hint"
                ),
                x,
                topPos + imageHeight - 13,
                0xFF71808C,
                false
        );
    }

    private void renderCalculationProperties(
            GuiGraphics graphics,
            Node node,
            int x,
            int y
    ) {
        graphics.drawString(
                font,
                node.type()
                        .label(),
                x,
                y,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "Result: "
                                + formatNumber(
                                node.result()
                        )
                                + (
                                node.unit()
                                        .isEmpty()
                                        ? ""
                                        : " "
                                                + node.unit()
                        )
                ),
                x,
                y + 15,
                0xFF8FE38F,
                false
        );

        graphics.drawString(
                font,
                calculationExplanation(
                        node.type()
                ),
                x,
                y + 31,
                0xFF9EADB7,
                false
        );

        boolean editable =
                node.type()
                        == NodeType.CONSTANT;

        valueBox.setVisible(
                editable
        );

        if (editable
                && !valueBox.isFocused()) {
            String expected =
                    formatEditable(
                            node.literal()
                    );

            if (!expected.equals(
                    valueBox.getValue()
            )) {
                syncingValueBox =
                        true;

                valueBox.setValue(
                        expected
                );

                syncingValueBox =
                        false;
            }
        }

        if (node.inputCount() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.engineering.snap_hint"
                    ),
                    x,
                    y + 56,
                    0xFF6EBCE8,
                    false
            );
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.delete_hint"
                ),
                x,
                y + 74,
                0xFFB77E7E,
                false
        );
    }

    private void renderPalette(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x0 =
                canvasX();

        int y0 =
                topPos
                        + imageHeight
                        - BOTTOM_TOOLBAR_HEIGHT;

        int width =
                canvasWidth();

        graphics.fill(
                x0,
                y0,
                x0 + width,
                topPos + imageHeight - 4,
                0xFF1A2026
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.calculations"
                ),
                x0 + 6,
                y0 + 4,
                0xFFBFD6E5,
                false
        );

        int cellWidth =
                58;

        int rowHeight =
                18;

        for (int index =
                     0;
             index < PALETTE.size();
             index++) {

            int column =
                    index % 7;

            int row =
                    index / 7;

            int x =
                    x0 + 5
                            + column
                                    * cellWidth;

            int y =
                    y0 + 17
                            + row
                                    * rowHeight;

            boolean hovered =
                    inside(
                            mouseX,
                            mouseY,
                            x,
                            y,
                            cellWidth - 3,
                            16
                    );

            graphics.fill(
                    x,
                    y,
                    x + cellWidth - 3,
                    y + 16,
                    hovered
                            ? 0xFF475663
                            : 0xFF2C353D
            );

            graphics.drawString(
                    font,
                    paletteLabel(
                            PALETTE.get(
                                    index
                            )
                    ),
                    x + 4,
                    y + 4,
                    0xFFDCE4E9,
                    false
            );
        }
    }

    private void renderVisualPreview(
            GuiGraphics graphics
    ) {
        Node node =
                graph.node(
                        selectedNodeId
                );

        if (node == null) {
            return;
        }

        int x =
                canvasX() + 8;

        int y =
                canvasY() + 7;

        int width =
                canvasWidth() - 16;

        int height =
                76;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xD91B2025
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.engineering.visual_preview"
                ),
                x + 6,
                y + 5,
                0xFF9FD8FF,
                false
        );

        int cx =
                x + width / 2;

        int cy =
                y + 43;

        double a =
                inputResult(
                        node,
                        0
                );

        double b =
                inputResult(
                        node,
                        1
                );

        double c =
                inputResult(
                        node,
                        2
                );

        switch (node.type()) {
            case CONSTANT, BLOCK_PROPERTY ->
                    drawPoint(
                            graphics,
                            cx,
                            cy,
                            0xFFFFC857
                    );

            case ADD, SUBTRACT -> {
                int first =
                        visualLength(
                                a,
                                48
                        );

                int second =
                        visualLength(
                                b,
                                48
                        );

                int result =
                        visualLength(
                                node.result(),
                                92
                        );

                drawLine(
                        graphics,
                        cx - 50,
                        cy - 6,
                        cx - 50 + first,
                        cy - 6,
                        0xFF6FBFFF
                );

                drawLine(
                        graphics,
                        cx - 50 + first,
                        cy - 6,
                        cx - 50 + first + second,
                        cy - 6,
                        0xFFFFB85C
                );

                drawLine(
                        graphics,
                        cx - 50,
                        cy + 10,
                        cx - 50 + result,
                        cy + 10,
                        0xFF77E088
                );
            }

            case MULTIPLY, RECTANGLE_AREA -> {
                int w =
                        Math.max(
                                6,
                                visualLength(
                                        a,
                                        75
                                )
                        );

                int h =
                        Math.max(
                                6,
                                visualLength(
                                        b,
                                        35
                                )
                        );

                drawRectOutline(
                        graphics,
                        cx - w / 2,
                        cy - h / 2,
                        w,
                        h,
                        0xFF73C6FF
                );
            }

            case BOX_VOLUME -> {
                int w =
                        Math.max(
                                12,
                                visualLength(
                                        a,
                                        55
                                )
                        );

                int h =
                        Math.max(
                                10,
                                visualLength(
                                        b,
                                        28
                                )
                        );

                int d =
                        Math.max(
                                7,
                                visualLength(
                                        c,
                                        18
                                )
                        );

                drawBox(
                        graphics,
                        cx,
                        cy,
                        w,
                        h,
                        d,
                        0xFF73C6FF
                );
            }

            case CIRCLE_AREA -> {
                int radius =
                        Math.max(
                                5,
                                visualLength(
                                        a,
                                        25
                                )
                        );

                drawCircle(
                        graphics,
                        cx,
                        cy,
                        radius,
                        0xFFBE8CFF
                );
            }

            case PYTHAGORAS, SLOPE_DEGREES -> {
                int w =
                        Math.max(
                                8,
                                visualLength(
                                        b,
                                        70
                                )
                        );

                int h =
                        Math.max(
                                8,
                                visualLength(
                                        a,
                                        38
                                )
                        );

                int x0 =
                        cx - w / 2;

                int y0 =
                        cy + h / 2;

                drawLine(
                        graphics,
                        x0,
                        y0,
                        x0 + w,
                        y0,
                        0xFF6FBFFF
                );

                drawLine(
                        graphics,
                        x0,
                        y0,
                        x0,
                        y0 - h,
                        0xFFFFB85C
                );

                drawLine(
                        graphics,
                        x0,
                        y0 - h,
                        x0 + w,
                        y0,
                        0xFF77E088
                );
            }

            case SAFETY_FACTOR, STRESS, DIVIDE -> {
                int value =
                        visualLength(
                                node.result(),
                                92
                        );

                graphics.fill(
                        cx - 48,
                        cy - 5,
                        cx + 48,
                        cy + 5,
                        0xFF313941
                );

                graphics.fill(
                        cx - 48,
                        cy - 5,
                        cx - 48 + value,
                        cy + 5,
                        node.result() >= 1.0
                                ? 0xFF77E088
                                : 0xFFFF6D6D
                );
            }

            case BEAM_UDL_MOMENT, BEAM_CENTER_MOMENT -> {
                drawLine(
                        graphics,
                        cx - 58,
                        cy + 13,
                        cx + 58,
                        cy + 13,
                        0xFFB6C1C8
                );

                if (node.type()
                        == NodeType.BEAM_UDL_MOMENT) {

                    for (int offset =
                                 -48;
                         offset <= 48;
                         offset += 16) {

                        drawArrowDown(
                                graphics,
                                cx + offset,
                                cy - 18,
                                cy + 7,
                                0xFFFFB85C
                        );
                    }

                } else {
                    drawArrowDown(
                            graphics,
                            cx,
                            cy - 22,
                            cy + 7,
                            0xFFFFB85C
                    );
                }
            }
        }

        graphics.drawString(
                font,
                trim(
                        "= "
                                + formatNumber(
                                node.result()
                        )
                                + (
                                node.unit()
                                        .isEmpty()
                                        ? ""
                                        : " "
                                                + node.unit()
                        ),
                        22
                ),
                x + width - 108,
                y + 5,
                0xFF77E088,
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

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (super.mouseClicked(
                mouseX,
                mouseY,
                button
        )) {
            return true;
        }

        if (button != 0) {
            return false;
        }

        if (handleCatalogClick(
                mouseX,
                mouseY
        )) {
            return true;
        }

        if (handlePropertyClick(
                mouseX,
                mouseY
        )) {
            return true;
        }

        if (handlePaletteClick(
                mouseX,
                mouseY
        )) {
            return true;
        }

        for (Node node :
                reversedNodes()) {

            int x =
                    nodeScreenX(
                            node
                    );

            int y =
                    nodeScreenY(
                            node
                    );

            int h =
                    nodeHeight(
                            node
                    );

            if (inside(
                    mouseX,
                    mouseY,
                    x + NODE_WIDTH - 14,
                    y,
                    14,
                    NODE_HEADER
            )) {
                graph.remove(
                        node.id()
                );

                blockNodes.remove(
                        node.id()
                );

                blockProperties.remove(
                        node.id()
                );

                if (selectedNodeId
                        == node.id()) {
                    selectedNodeId =
                            -1;
                }

                if (pendingSourceId
                        == node.id()) {
                    pendingSourceId =
                            -1;
                }

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    x + NODE_WIDTH - 5,
                    y + h / 2 - 5,
                    10,
                    10
            )) {
                pendingSourceId =
                        pendingSourceId
                                == node.id()
                                ? -1
                                : node.id();

                selectNode(
                        node.id()
                );

                return true;
            }

            for (int slot =
                         0;
                 slot < node.inputCount();
                 slot++) {

                int py =
                        inputPortY(
                                node,
                                slot
                        );

                if (inside(
                        mouseX,
                        mouseY,
                        x - 6,
                        py - 6,
                        12,
                        12
                )) {
                    if (pendingSourceId >= 0) {
                        graph.connect(
                                pendingSourceId,
                                node.id(),
                                slot
                        );

                        pendingSourceId =
                                -1;

                    } else if (node.input(
                            slot
                    ) >= 0) {
                        graph.disconnect(
                                node.id(),
                                slot
                        );
                    }

                    selectNode(
                            node.id()
                    );

                    return true;
                }
            }

            if (inside(
                    mouseX,
                    mouseY,
                    x,
                    y,
                    NODE_WIDTH,
                    h
            )) {
                selectNode(
                        node.id()
                );

                draggingNodeId =
                        node.id();

                dragOffsetX =
                        (int) mouseX
                                - x;

                dragOffsetY =
                        (int) mouseY
                                - y;

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (draggingNodeId >= 0
                && button == 0) {

            Node node =
                    graph.node(
                            draggingNodeId
                    );

            if (node != null) {
                int localX =
                        (int) mouseX
                                - canvasX()
                                - dragOffsetX;

                int localY =
                        (int) mouseY
                                - canvasY()
                                - dragOffsetY;

                node.move(
                        Math.clamp(
                                localX,
                                2,
                                Math.max(
                                        2,
                                        canvasWidth()
                                                - NODE_WIDTH
                                                - 2
                                )
                        ),
                        Math.clamp(
                                localY,
                                2,
                                Math.max(
                                        2,
                                        canvasHeight()
                                                - nodeHeight(
                                                node
                                        )
                                                - 2
                                )
                        )
                );
            }

            return true;
        }

        return super.mouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        draggingNodeId =
                -1;

        return super.mouseReleased(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (inside(
                mouseX,
                mouseY,
                leftPos + 4,
                topPos + 50,
                CATALOG_WIDTH - 4,
                imageHeight - 56
        )) {
            catalogScroll =
                    Math.clamp(
                            catalogScroll
                                    - (int) Math.signum(
                                    scrollY
                            ),
                            0,
                            Math.max(
                                    0,
                                    filtered.size()
                                            - visibleCatalogRows()
                            )
                    );

            return true;
        }

        int propertyX =
                leftPos
                        + imageWidth
                        - PROPERTY_WIDTH;

        if (inside(
                mouseX,
                mouseY,
                propertyX,
                topPos + TOP,
                PROPERTY_WIDTH,
                imageHeight - TOP
        )) {
            Node selected =
                    graph.node(
                            selectedNodeId
                    );

            EngineeringBlockProfile profile =
                    selected == null
                            ? null
                            : blockNodes.get(
                            selected.id()
                    );

            if (profile != null) {
                propertyScroll =
                        Math.clamp(
                                propertyScroll
                                        - (int) Math.signum(
                                        scrollY
                                ),
                                0,
                                Math.max(
                                        0,
                                        profile.properties()
                                                .size()
                                                - 9
                                )
                        );

                return true;
            }
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (valueBox != null
                && valueBox.isFocused()) {
            return super.keyPressed(
                    keyCode,
                    scanCode,
                    modifiers
            );
        }

        if ((keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_BACKSPACE)
                && selectedNodeId >= 0) {

            graph.remove(
                    selectedNodeId
            );

            blockNodes.remove(
                    selectedNodeId
            );

            blockProperties.remove(
                    selectedNodeId
            );

            selectedNodeId =
                    -1;

            pendingSourceId =
                    -1;

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && pendingSourceId >= 0) {
            pendingSourceId =
                    -1;

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    private boolean handleCatalogClick(
            double mouseX,
            double mouseY
    ) {
        int startY =
                topPos + 66;

        if (!inside(
                mouseX,
                mouseY,
                leftPos + 7,
                startY,
                CATALOG_WIDTH - 14,
                visibleCatalogRows()
                        * CATALOG_ROW
        )) {
            return false;
        }

        int row =
                ((int) mouseY
                        - startY)
                        / CATALOG_ROW;

        int index =
                catalogScroll
                        + row;

        if (index < 0
                || index >= filtered.size()) {
            return false;
        }

        EngineeringBlockProfile profile =
                filtered.get(
                        index
                );

        addBlockNode(
                profile
        );

        return true;
    }

    private boolean handlePropertyClick(
            double mouseX,
            double mouseY
    ) {
        Node selected =
                graph.node(
                        selectedNodeId
                );

        EngineeringBlockProfile profile =
                selected == null
                        ? null
                        : blockNodes.get(
                        selected.id()
                );

        if (profile == null) {
            return false;
        }

        int x =
                leftPos
                        + imageWidth
                        - PROPERTY_WIDTH
                        + 7;

        int listY =
                topPos + 85;

        if (!inside(
                mouseX,
                mouseY,
                x,
                listY,
                PROPERTY_WIDTH - 18,
                imageHeight - 96
        )) {
            return false;
        }

        int row =
                ((int) mouseY
                        - listY)
                        / 22;

        int index =
                propertyScroll
                        + row;

        if (index < 0
                || index >= profile.properties()
                        .size()) {
            return false;
        }

        EngineeringBlockProfile.Property property =
                profile.properties()
                        .get(
                                index
                        );

        if (!property.numeric()) {
            return true;
        }

        if (hasShiftDown()) {
            Node pinned =
                    graph.add(
                            NodeType.BLOCK_PROPERTY,
                            Math.clamp(
                                    selected.x()
                                            + 118,
                                    2,
                                    canvasWidth()
                                            - NODE_WIDTH
                                            - 2
                            ),
                            Math.clamp(
                                    selected.y()
                                            + 18,
                                    2,
                                    canvasHeight()
                                            - 55
                            ),
                            trim(
                                    property.label(),
                                    18
                            ),
                            property.numericValue(),
                            property.unit()
                    );

            blockNodes.put(
                    pinned.id(),
                    profile
            );

            blockProperties.put(
                    pinned.id(),
                    property.key()
            );

            selectNode(
                    pinned.id()
            );

        } else {
            selected.label(
                    trim(
                            displayName(
                                    profile.block()
                            )
                                    + " · "
                                    + property.label(),
                            24
                    )
            );

            selected.literal(
                    property.numericValue()
            );

            selected.unit(
                    property.unit()
            );

            blockProperties.put(
                    selected.id(),
                    property.key()
            );

            graph.recalculate();
        }

        return true;
    }

    private boolean handlePaletteClick(
            double mouseX,
            double mouseY
    ) {
        int x0 =
                canvasX();

        int y0 =
                topPos
                        + imageHeight
                        - BOTTOM_TOOLBAR_HEIGHT
                        + 17;

        int cellWidth =
                58;

        int rowHeight =
                18;

        for (int index =
                     0;
             index < PALETTE.size();
             index++) {

            int column =
                    index % 7;

            int row =
                    index / 7;

            int x =
                    x0 + 5
                            + column
                                    * cellWidth;

            int y =
                    y0
                            + row
                                    * rowHeight;

            if (!inside(
                    mouseX,
                    mouseY,
                    x,
                    y,
                    cellWidth - 3,
                    16
            )) {
                continue;
            }

            NodeType type =
                    PALETTE.get(
                            index
                    );

            Node node =
                    graph.add(
                            type,
                            132
                                    + (
                                    index % 3
                            )
                                    * 16,
                            126
                                    + (
                                    index % 4
                            )
                                    * 12
                    );

            selectNode(
                    node.id()
            );

            return true;
        }

        return false;
    }

    private void addBlockNode(
            EngineeringBlockProfile profile
    ) {
        EngineeringBlockProfile.Property property =
                profile.property(
                        "mass"
                );

        double value =
                property == null
                        ? profile.massKg()
                        : property.numericValue();

        String unit =
                property == null
                        ? "kg"
                        : property.unit();

        String key =
                property == null
                        ? "mass"
                        : property.key();

        Node node =
                graph.add(
                        NodeType.BLOCK_PROPERTY,
                        18
                                + (
                                graph.nodes()
                                        .size()
                                        * 17
                        )
                                % Math.max(
                                20,
                                canvasWidth()
                                        - NODE_WIDTH
                                        - 28
                        ),
                        105
                                + (
                                graph.nodes()
                                        .size()
                                        * 13
                        )
                                % Math.max(
                                20,
                                canvasHeight()
                                        - 155
                        ),
                        trim(
                                displayName(
                                        profile.block()
                                )
                                        + " · Weight",
                                24
                        ),
                        value,
                        unit
                );

        blockNodes.put(
                node.id(),
                profile
        );

        blockProperties.put(
                node.id(),
                key
        );

        propertyScroll =
                0;

        selectNode(
                node.id()
        );
    }

    private void selectNode(
            int id
    ) {
        selectedNodeId =
                id;

        propertyScroll =
                0;

        Node node =
                graph.node(
                        id
                );

        if (node == null
                || node.type()
                        != NodeType.CONSTANT) {
            if (valueBox != null) {
                valueBox.setVisible(
                        false
                );
            }

            return;
        }

        if (valueBox != null) {
            syncingValueBox =
                    true;

            valueBox.setValue(
                    formatEditable(
                            node.literal()
                    )
            );

            syncingValueBox =
                    false;

            valueBox.setVisible(
                    true
            );
        }
    }

    private void updateLiteralFromEditor(
            String text
    ) {
        if (syncingValueBox) {
            return;
        }

        Node node =
                graph.node(
                        selectedNodeId
                );

        if (node == null
                || node.type()
                        != NodeType.CONSTANT) {
            return;
        }

        try {
            double value =
                    Double.parseDouble(
                            text.replace(
                                    ',',
                                    '.'
                            )
                    );

            node.literal(
                    value
            );

            graph.recalculate();

        } catch (NumberFormatException ignored) {
        }
    }

    private int canvasX() {
        return leftPos
                + CATALOG_WIDTH
                + 4;
    }

    private int canvasY() {
        return topPos
                + TOP;
    }

    private int canvasWidth() {
        return imageWidth
                - CATALOG_WIDTH
                - PROPERTY_WIDTH
                - 8;
    }

    private int canvasHeight() {
        return imageHeight
                - TOP
                - BOTTOM_TOOLBAR_HEIGHT
                - 4;
    }

    private int visibleCatalogRows() {
        return Math.max(
                1,
                (
                        imageHeight
                                - 72
                )
                        / CATALOG_ROW
        );
    }

    private int nodeScreenX(
            Node node
    ) {
        return canvasX()
                + node.x();
    }

    private int nodeScreenY(
            Node node
    ) {
        return canvasY()
                + node.y();
    }

    private int nodeHeight(
            Node node
    ) {
        return Math.max(
                40,
                30
                        + node.inputCount()
                                * NODE_INPUT_SPACING
        );
    }

    private int inputPortY(
            Node node,
            int slot
    ) {
        return nodeScreenY(
                node
        )
                + NODE_HEADER
                + 6
                + slot
                        * NODE_INPUT_SPACING;
    }

    private List<Node> reversedNodes() {
        ArrayList<Node> nodes =
                new ArrayList<>(
                        graph.nodes()
                );

        java.util.Collections.reverse(
                nodes
        );

        return nodes;
    }

    private double inputResult(
            Node node,
            int slot
    ) {
        if (slot >= node.inputCount()) {
            return 0.0;
        }

        Node source =
                graph.node(
                        node.input(
                                slot
                        )
                );

        return source == null
                ? 0.0
                : source.result();
    }

    private int recommendedRank(
            EngineeringBlockProfile profile
    ) {
        String path =
                profile.id()
                        .getPath();

        return switch (path) {
            case "oak_planks" -> 0;
            case "stone_bricks" -> 1;
            case "smooth_stone" -> 2;
            case "bricks" -> 3;
            case "iron_block" -> 4;
            case "copper_block" -> 5;
            case "glass" -> 6;
            case "white_concrete" -> 7;
            case "scaffolding" -> 8;
            case "oak_log" -> 9;
            case "deepslate_bricks" -> 10;
            case "obsidian" -> 11;
            default -> profile.id()
                    .getNamespace()
                    .equals(
                            "wayaround"
                    )
                    ? 100
                    : 1000;
        };
    }

    private static String displayName(
            Block block
    ) {
        return Component.translatable(
                block.getDescriptionId()
        ).getString();
    }

    private static String paletteLabel(
            NodeType type
    ) {
        return switch (type) {
            case CONSTANT -> "Number";
            case ADD -> "+ Add";
            case SUBTRACT -> "- Sub";
            case MULTIPLY -> "× Mul";
            case DIVIDE -> "÷ Div";
            case PYTHAGORAS -> "△ Dist";
            case RECTANGLE_AREA -> "□ Area";
            case BOX_VOLUME -> "▣ Vol";
            case CIRCLE_AREA -> "○ Area";
            case SLOPE_DEGREES -> "∠ Slope";
            case SAFETY_FACTOR -> "SF";
            case STRESS -> "F/A";
            case BEAM_UDL_MOMENT -> "qL²/8";
            case BEAM_CENTER_MOMENT -> "PL/4";
            case BLOCK_PROPERTY -> "Block";
        };
    }

    private static String inputLabel(
            NodeType type,
            int slot
    ) {
        return switch (type) {
            case RECTANGLE_AREA ->
                    slot == 0
                            ? "W"
                            : "H";

            case BOX_VOLUME ->
                    switch (slot) {
                        case 0 -> "W";
                        case 1 -> "H";
                        default -> "D";
                    };

            case CIRCLE_AREA ->
                    "r";

            case SLOPE_DEGREES ->
                    slot == 0
                            ? "rise"
                            : "run";

            case SAFETY_FACTOR ->
                    slot == 0
                            ? "capacity"
                            : "load";

            case STRESS ->
                    slot == 0
                            ? "force"
                            : "area";

            case BEAM_UDL_MOMENT ->
                    slot == 0
                            ? "q"
                            : "L";

            case BEAM_CENTER_MOMENT ->
                    slot == 0
                            ? "P"
                            : "L";

            default ->
                    "in"
                            + (
                            slot + 1
                    );
        };
    }

    private static Component calculationExplanation(
            NodeType type
    ) {
        return Component.literal(
                switch (type) {
                    case CONSTANT ->
                            "Editable design value.";
                    case ADD ->
                            "Adds two design quantities.";
                    case SUBTRACT ->
                            "Difference between quantities.";
                    case MULTIPLY ->
                            "Product / scale relation.";
                    case DIVIDE ->
                            "Ratio between quantities.";
                    case PYTHAGORAS ->
                            "sqrt(a² + b²).";
                    case RECTANGLE_AREA ->
                            "Width × height.";
                    case BOX_VOLUME ->
                            "Width × height × depth.";
                    case CIRCLE_AREA ->
                            "πr².";
                    case SLOPE_DEGREES ->
                            "atan(rise/run).";
                    case SAFETY_FACTOR ->
                            "Capacity ÷ applied load.";
                    case STRESS ->
                            "Force ÷ loaded area.";
                    case BEAM_UDL_MOMENT ->
                            "Max simple beam moment.";
                    case BEAM_CENTER_MOMENT ->
                            "Center point-load moment.";
                    case BLOCK_PROPERTY ->
                            "Output from a selected block.";
                }
        );
    }

    private static int visualLength(
            double value,
            int maximum
    ) {
        if (!Double.isFinite(
                value
        )) {
            return 0;
        }

        double magnitude =
                Math.abs(
                        value
                );

        if (magnitude < 0.000001) {
            return 0;
        }

        double scaled =
                7.0
                        + Math.log10(
                        1.0 + magnitude
                )
                                * 18.0;

        return (int) Math.clamp(
                Math.round(
                        scaled
                ),
                2,
                maximum
        );
    }

    private static String formatNumber(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "—";
        }

        double absolute =
                Math.abs(
                        value
                );

        if (absolute >= 1000000.0
                || (
                absolute > 0.0
                        && absolute < 0.001
        )) {
            return String.format(
                    Locale.ROOT,
                    "%.2e",
                    value
            );
        }

        if (absolute >= 1000.0) {
            return String.format(
                    Locale.ROOT,
                    "%.0f",
                    value
            );
        }

        if (absolute >= 10.0) {
            return String.format(
                    Locale.ROOT,
                    "%.2f",
                    value
            );
        }

        return String.format(
                Locale.ROOT,
                "%.3f",
                value
        );
    }

    private static String formatEditable(
            double value
    ) {
        if (Math.abs(
                value
                        - Math.rint(
                        value
                )
        ) < 0.000001) {
            return Long.toString(
                    Math.round(
                            value
                    )
            );
        }

        return String.format(
                Locale.ROOT,
                "%.4f",
                value
        );
    }

    private static String trim(
            String value,
            int maximum
    ) {
        if (value == null
                || value.length()
                        <= maximum) {
            return value == null
                    ? ""
                    : value;
        }

        return value.substring(
                0,
                Math.max(
                        0,
                        maximum - 1
                )
        )
                + "…";
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private static void drawPoint(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(
                x - 2,
                y - 2,
                x + 3,
                y + 3,
                color
        );
    }

    private static void drawLine(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int color
    ) {
        int dx =
                Math.abs(
                        x1 - x0
                );

        int sx =
                x0 < x1
                        ? 1
                        : -1;

        int dy =
                -Math.abs(
                        y1 - y0
                );

        int sy =
                y0 < y1
                        ? 1
                        : -1;

        int error =
                dx + dy;

        while (true) {
            graphics.fill(
                    x0,
                    y0,
                    x0 + 1,
                    y0 + 1,
                    color
            );

            if (x0 == x1
                    && y0 == y1) {
                break;
            }

            int twice =
                    2 * error;

            if (twice >= dy) {
                error +=
                        dy;

                x0 +=
                        sx;
            }

            if (twice <= dx) {
                error +=
                        dx;

                y0 +=
                        sy;
            }
        }
    }

    private static void drawRectOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        drawLine(
                graphics,
                x,
                y,
                x + width,
                y,
                color
        );

        drawLine(
                graphics,
                x + width,
                y,
                x + width,
                y + height,
                color
        );

        drawLine(
                graphics,
                x + width,
                y + height,
                x,
                y + height,
                color
        );

        drawLine(
                graphics,
                x,
                y + height,
                x,
                y,
                color
        );
    }

    private static void drawBox(
            GuiGraphics graphics,
            int cx,
            int cy,
            int width,
            int height,
            int depth,
            int color
    ) {
        int x =
                cx - width / 2;

        int y =
                cy - height / 2;

        drawRectOutline(
                graphics,
                x,
                y,
                width,
                height,
                color
        );

        drawRectOutline(
                graphics,
                x + depth,
                y - depth,
                width,
                height,
                color
        );

        drawLine(
                graphics,
                x,
                y,
                x + depth,
                y - depth,
                color
        );

        drawLine(
                graphics,
                x + width,
                y,
                x + width + depth,
                y - depth,
                color
        );

        drawLine(
                graphics,
                x,
                y + height,
                x + depth,
                y + height - depth,
                color
        );

        drawLine(
                graphics,
                x + width,
                y + height,
                x + width + depth,
                y + height - depth,
                color
        );
    }

    private static void drawCircle(
            GuiGraphics graphics,
            int cx,
            int cy,
            int radius,
            int color
    ) {
        int previousX =
                cx + radius;

        int previousY =
                cy;

        for (int step =
                     1;
             step <= 32;
             step++) {

            double angle =
                    Math.PI
                            * 2.0
                            * step
                            / 32.0;

            int x =
                    cx
                            + (int) Math.round(
                            Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    cy
                            + (int) Math.round(
                            Math.sin(
                                    angle
                            )
                                    * radius
                    );

            drawLine(
                    graphics,
                    previousX,
                    previousY,
                    x,
                    y,
                    color
            );

            previousX =
                    x;

            previousY =
                    y;
        }
    }

    private static void drawArrowDown(
            GuiGraphics graphics,
            int x,
            int y0,
            int y1,
            int color
    ) {
        drawLine(
                graphics,
                x,
                y0,
                x,
                y1,
                color
        );

        drawLine(
                graphics,
                x,
                y1,
                x - 3,
                y1 - 4,
                color
        );

        drawLine(
                graphics,
                x,
                y1,
                x + 3,
                y1 - 4,
                color
        );
    }
}
