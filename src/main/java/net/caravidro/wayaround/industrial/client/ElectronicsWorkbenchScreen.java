package net.caravidro.wayaround.industrial.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.electronics.CircuitBoardData;
import net.caravidro.wayaround.industrial.electronics.CircuitBoardData.ComponentType;
import net.caravidro.wayaround.industrial.electronics.ElectronicsContent;
import net.caravidro.wayaround.industrial.electronics.ElectronicsWorkbenchBlockEntity;
import net.caravidro.wayaround.industrial.electronics.ElectronicsWorkbenchMenu;
import net.caravidro.wayaround.network.CircuitWorkbenchActionC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ElectronicsWorkbenchScreen
        extends AbstractContainerScreen<ElectronicsWorkbenchMenu> {

    private static final int CATALOG_WIDTH = 124;
    private static final int PROPERTY_WIDTH = 150;
    private static final int TOP = 28;
    private static final int BOARD_CELL = 52;
    private static final int BOARD_WIDTH =
            CircuitBoardData.WIDTH * BOARD_CELL;
    private static final int BOARD_HEIGHT =
            CircuitBoardData.HEIGHT * BOARD_CELL;

    private static final double MIN_ZOOM = 0.55;
    private static final double MAX_ZOOM = 2.35;

    private ComponentType draggingType;
    private int selectedCell = -1;
    private int hoveredCell = -1;
    private int pendingConnection = -1;
    private boolean panning;
    private double lastPanX;
    private double lastPanY;
    private double zoom = 0.82;
    private double panX;
    private double panY;
    private boolean centered;

    public ElectronicsWorkbenchScreen(
            ElectronicsWorkbenchMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );

        imageWidth =
                586;

        imageHeight =
                330;
    }

    @Override
    protected void init() {
        super.init();

        centered =
                false;
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

        if (draggingType != null) {
            ItemStack ghost =
                    new ItemStack(
                            ElectronicsContent.itemFor(
                                    draggingType
                            )
                    );

            graphics.renderItem(
                    ghost,
                    mouseX - 8,
                    mouseY - 8
            );

            graphics.renderTooltip(
                    font,
                    ghost,
                    mouseX,
                    mouseY
            );
        }

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        if (!centered) {
            centerBoard();
            centered =
                    true;
        }

        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF171B1E
        );

        graphics.fill(
                leftPos + 3,
                topPos + 3,
                leftPos + imageWidth - 3,
                topPos + imageHeight - 3,
                0xFF252C30
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics_workbench"
                ),
                leftPos + 8,
                topPos + 9,
                0xFFE0E8DC,
                false
        );

        renderCatalog(
                graphics,
                mouseX,
                mouseY
        );

        renderBoardCanvas(
                graphics,
                mouseX,
                mouseY
        );

        renderProperties(
                graphics
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
    }

    private void renderCatalog(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x =
                leftPos + 5;

        int y =
                topPos + TOP;

        graphics.fill(
                x,
                y,
                x + CATALOG_WIDTH - 5,
                topPos + imageHeight - 5,
                0xFF1A2023
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.inventory_parts"
                ),
                x + 7,
                y + 8,
                0xFFA8C7B4,
                false
        );

        int rowY =
                y + 27;

        for (ComponentType type :
                visibleComponents()) {

            int count =
                    inventoryCount(
                            ElectronicsContent.itemFor(
                                    type
                            )
                    );

            boolean creative =
                    minecraft != null
                            && minecraft.player != null
                            && minecraft.player.getAbilities().instabuild;

            boolean hovered =
                    inside(
                            mouseX,
                            mouseY,
                            x + 6,
                            rowY,
                            CATALOG_WIDTH - 17,
                            25
                    );

            graphics.fill(
                    x + 6,
                    rowY,
                    x + CATALOG_WIDTH - 11,
                    rowY + 23,
                    hovered
                            ? 0xFF3B5147
                            : 0xFF29332E
            );

            graphics.renderItem(
                    new ItemStack(
                            ElectronicsContent.itemFor(
                                    type
                            )
                    ),
                    x + 9,
                    rowY + 3
            );

            graphics.drawString(
                    font,
                    componentName(
                            type
                    ),
                    x + 29,
                    rowY + 4,
                    0xFFE4E8E3,
                    false
            );

            graphics.drawString(
                    font,
                    creative
                            ? "∞"
                            : "x" + count,
                    x + 29,
                    rowY + 14,
                    0xFF94A49A,
                    false
            );

            rowY +=
                    27;
        }

        int copper =
                inventoryCount(
                        ElectronicsContent.COPPER_TRACE.get()
                );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.copper_available",
                        copper
                ),
                x + 7,
                topPos + imageHeight - 22,
                0xFFC98555,
                false
        );
    }

    private void renderBoardCanvas(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int x0 =
                canvasX();

        int y0 =
                canvasY();

        int x1 =
                x0 + canvasWidth();

        int y1 =
                y0 + canvasHeight();

        graphics.fill(
                x0,
                y0,
                x1,
                y1,
                0xFF0D1212
        );

        graphics.enableScissor(
                x0,
                y0,
                x1,
                y1
        );

        graphics.pose().pushPose();

        graphics.pose().translate(
                (float) (x0 + panX),
                (float) (y0 + panY),
                0.0F
        );

        graphics.pose().scale(
                (float) zoom,
                (float) zoom,
                1.0F
        );

        graphics.fill(
                0,
                0,
                BOARD_WIDTH,
                BOARD_HEIGHT,
                0xFF285B32
        );

        graphics.fill(
                4,
                4,
                BOARD_WIDTH - 4,
                BOARD_HEIGHT - 4,
                0xFF347441
        );

        CircuitBoardData board =
                board();

        hoveredCell =
                cellAt(
                        mouseX,
                        mouseY
                );

        for (CircuitBoardData.Trace trace :
                board.traceEdges()) {
            drawTrace(
                    graphics,
                    trace.a(),
                    trace.b(),
                    0xFFD17B38
            );
        }

        if (pendingConnection >= 0
                && hoveredCell >= 0
                && hoveredCell != pendingConnection
                && board.component(
                hoveredCell
        ) != ComponentType.EMPTY
                && !board.hasTrace(
                pendingConnection,
                hoveredCell
        )) {
            drawTrace(
                    graphics,
                    pendingConnection,
                    hoveredCell,
                    0x99FFD27A
            );
        }

        for (int y = 0;
             y < CircuitBoardData.HEIGHT;
             y++) {
            for (int x = 0;
                 x < CircuitBoardData.WIDTH;
                 x++) {

                int cell =
                        CircuitBoardData.cell(
                                x,
                                y
                        );

                renderCell(
                        graphics,
                        board,
                        cell,
                        mouseX,
                        mouseY
                );
            }
        }

        graphics.pose().popPose();

        graphics.disableScissor();

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.zoom_hint"
                ),
                x0 + 7,
                y1 - 12,
                0xFF718078,
                false
        );
    }

    private void renderCell(
            GuiGraphics graphics,
            CircuitBoardData board,
            int cell,
            int mouseX,
            int mouseY
    ) {
        int x =
                CircuitBoardData.cellX(
                        cell
                )
                        * BOARD_CELL;

        int y =
                CircuitBoardData.cellY(
                        cell
                )
                        * BOARD_CELL;

        graphics.fill(
                x + 2,
                y + 2,
                x + BOARD_CELL - 2,
                y + BOARD_CELL - 2,
                cell == selectedCell
                        ? 0x333FFFFF
                        : 0x11000000
        );

        ComponentType type =
                board.component(
                        cell
                );

        if (type == ComponentType.EMPTY) {
            graphics.fill(
                    x + BOARD_CELL / 2 - 2,
                    y + BOARD_CELL / 2 - 2,
                    x + BOARD_CELL / 2 + 2,
                    y + BOARD_CELL / 2 + 2,
                    0x5546A85B
            );

            return;
        }

        graphics.renderItem(
                new ItemStack(
                        ElectronicsContent.itemFor(
                                type
                        )
                ),
                x + BOARD_CELL / 2 - 8,
                y + BOARD_CELL / 2 - 10
        );

        int padX =
                x + BOARD_CELL - 12;

        int padY =
                y + BOARD_CELL - 12;

        graphics.fill(
                padX,
                padY,
                padX + 8,
                padY + 8,
                cell == pendingConnection
                        ? 0xFFFFFFA0
                        : 0xFFC77632
        );
    }

    private void drawTrace(
            GuiGraphics graphics,
            int a,
            int b,
            int color
    ) {
        double ax =
                CircuitBoardData.cellX(a)
                        * BOARD_CELL
                        + BOARD_CELL * 0.5;

        double ay =
                CircuitBoardData.cellY(a)
                        * BOARD_CELL
                        + BOARD_CELL * 0.5;

        double bx =
                CircuitBoardData.cellX(b)
                        * BOARD_CELL
                        + BOARD_CELL * 0.5;

        double by =
                CircuitBoardData.cellY(b)
                        * BOARD_CELL
                        + BOARD_CELL * 0.5;

        double dx =
                bx - ax;

        double dy =
                by - ay;

        double length =
                Math.sqrt(
                        dx * dx
                                + dy * dy
                );

        float angle =
                (float) Math.toDegrees(
                        Math.atan2(
                                dy,
                                dx
                        )
                );

        graphics.pose().pushPose();

        graphics.pose().translate(
                (float) ax,
                (float) ay,
                0.0F
        );

        graphics.pose().mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        graphics.fill(
                0,
                -2,
                (int) Math.ceil(
                        length
                ),
                2,
                color
        );

        graphics.pose().popPose();
    }

    private void renderProperties(
            GuiGraphics graphics
    ) {
        int x =
                leftPos
                        + imageWidth
                        - PROPERTY_WIDTH;

        int y =
                topPos + TOP;

        graphics.fill(
                x,
                y,
                leftPos + imageWidth - 5,
                topPos + imageHeight - 5,
                0xFF1A2023
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.properties"
                ),
                x + 8,
                y + 8,
                0xFFA8C7B4,
                false
        );

        CircuitBoardData board =
                board();

        int line =
                y + 31;

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.board_usage",
                        board.componentCount(),
                        CircuitBoardData.CELL_COUNT
                ),
                x + 8,
                line,
                0xFFD8DDD9,
                false
        );

        line +=
                14;

        graphics.drawString(
                font,
                Component.translatable(
                        "container.wayaround.electronics.trace_count",
                        board.traceCount()
                ),
                x + 8,
                line,
                0xFFD8DDD9,
                false
        );

        line +=
                14;

        graphics.drawString(
                font,
                Component.translatable(
                        board.hasSignalPath()
                                ? "container.wayaround.electronics.signal_closed"
                                : "container.wayaround.electronics.signal_open"
                ),
                x + 8,
                line,
                board.hasSignalPath()
                        ? 0xFF75D786
                        : 0xFF89918D,
                false
        );

        line +=
                24;

        if (selectedCell >= 0) {
            ComponentType type =
                    board.component(
                            selectedCell
                    );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.electronics.selected",
                            selectedCell + 1
                    ),
                    x + 8,
                    line,
                    0xFFE2D8B8,
                    false
            );

            line +=
                    14;

            graphics.drawString(
                    font,
                    type == ComponentType.EMPTY
                            ? Component.translatable(
                            "container.wayaround.electronics.empty_pad"
                    )
                            : Component.literal(
                            componentName(
                                    type
                            )
                    ),
                    x + 8,
                    line,
                    0xFFFFFFFF,
                    false
            );

            line +=
                    14;

            if (type != ComponentType.EMPTY) {
                graphics.drawString(
                        font,
                        Component.translatable(
                                "container.wayaround.electronics.component_draw",
                                type.energyCost()
                        ),
                        x + 8,
                        line,
                        0xFF9DA8A0,
                        false
                );
            }
        }

        if (pendingConnection >= 0) {
            line +=
                    24;

            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.electronics.connection_armed"
                    ),
                    x + 8,
                    line,
                    0xFFFFD27A,
                    false
            );

            line +=
                    13;

            graphics.drawString(
                    font,
                    Component.translatable(
                            "container.wayaround.electronics.connection_help"
                    ),
                    x + 8,
                    line,
                    0xFFA8A8A8,
                    false
            );

            CircuitBoardData current =
                    board();

            if (hoveredCell >= 0
                    && hoveredCell != pendingConnection
                    && current.component(
                    hoveredCell
            ) != ComponentType.EMPTY) {
                line +=
                        16;

                int cost =
                        CircuitBoardData.traceCopperCost(
                                pendingConnection,
                                hoveredCell
                        );

                graphics.drawString(
                        font,
                        Component.translatable(
                                "container.wayaround.electronics.connection_cost",
                                cost
                        ),
                        x + 8,
                        line,
                        inventoryCount(
                                ElectronicsContent.COPPER_TRACE.get()
                        ) >= cost
                                || creative()
                                ? 0xFFC98A52
                                : 0xFFE06060,
                        false
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && insideCanvas(
                mouseX,
                mouseY
        )) {
            panning =
                    true;

            lastPanX =
                    mouseX;

            lastPanY =
                    mouseY;

            return true;
        }

        ComponentType catalogue =
                componentAtCatalog(
                        mouseX,
                        mouseY
                );

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && catalogue != null) {
            draggingType =
                    catalogue;

            return true;
        }

        int cell =
                cellAt(
                        mouseX,
                        mouseY
                );

        if (cell >= 0) {
            selectedCell =
                    cell;

            CircuitBoardData board =
                    board();

            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    && board.component(
                    cell
            ) != ComponentType.EMPTY) {

                send(
                        CircuitWorkbenchActionC2SPayload.REMOVE,
                        0,
                        cell,
                        -1
                );

                if (pendingConnection == cell) {
                    pendingConnection =
                            -1;
                }

                return true;
            }

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    && board.component(
                    cell
            ) != ComponentType.EMPTY
                    && connectorAt(
                    cell,
                    mouseX,
                    mouseY
            )) {

                if (pendingConnection < 0) {
                    pendingConnection =
                            cell;

                } else if (pendingConnection == cell) {
                    pendingConnection =
                            -1;

                } else {
                    int cost =
                            CircuitBoardData.traceCopperCost(
                                    pendingConnection,
                                    cell
                            );

                    if (creative()
                            || inventoryCount(
                            ElectronicsContent.COPPER_TRACE.get()
                    ) >= cost) {
                        send(
                                CircuitWorkbenchActionC2SPayload.CONNECT,
                                0,
                                pendingConnection,
                                cell
                        );
                    }

                    pendingConnection =
                            -1;
                }

                return true;
            }

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (panning
                && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            panX +=
                    mouseX - lastPanX;

            panY +=
                    mouseY - lastPanY;

            lastPanX =
                    mouseX;

            lastPanY =
                    mouseY;

            return true;
        }

        return draggingType != null
                || super.mouseDragged(
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
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && panning) {
            panning =
                    false;

            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && draggingType != null) {
            ComponentType type =
                    draggingType;

            draggingType =
                    null;

            int cell =
                    cellAt(
                            mouseX,
                            mouseY
                    );

            if (cell >= 0
                    && board().component(
                    cell
            ) == ComponentType.EMPTY) {
                send(
                        CircuitWorkbenchActionC2SPayload.PLACE,
                        type.ordinal(),
                        cell,
                        -1
                );
            }

            return true;
        }

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
        if (!insideCanvas(
                mouseX,
                mouseY
        )
                || scrollY == 0.0) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        double localX =
                mouseX
                        - canvasX()
                        - panX;

        double localY =
                mouseY
                        - canvasY()
                        - panY;

        double boardX =
                localX
                        / zoom;

        double boardY =
                localY
                        / zoom;

        double old =
                zoom;

        zoom =
                Math.clamp(
                        zoom
                                * (
                                scrollY > 0
                                        ? 1.12
                                        : 1.0 / 1.12
                        ),
                        MIN_ZOOM,
                        MAX_ZOOM
                );

        if (Math.abs(
                zoom - old
        ) > 0.0001) {
            panX =
                    mouseX
                            - canvasX()
                            - boardX * zoom;

            panY =
                    mouseY
                            - canvasY()
                            - boardY * zoom;
        }

        return true;
    }

    private void centerBoard() {
        panX =
                (
                        canvasWidth()
                                - BOARD_WIDTH * zoom
                )
                        * 0.5;

        panY =
                (
                        canvasHeight()
                                - BOARD_HEIGHT * zoom
                )
                        * 0.5;
    }

    private CircuitBoardData board() {
        if (minecraft == null
                || minecraft.level == null) {
            return CircuitBoardData.empty();
        }

        if (minecraft.level.getBlockEntity(
                menu.workbenchPos()
        ) instanceof ElectronicsWorkbenchBlockEntity workbench) {
            return workbench.boardData();
        }

        return CircuitBoardData.empty();
    }

    private List<ComponentType> visibleComponents() {
        List<ComponentType> result =
                new ArrayList<>();

        for (ComponentType type :
                ComponentType.values()) {
            if (type == ComponentType.EMPTY) {
                continue;
            }

            if (creative()
                    || inventoryCount(
                    ElectronicsContent.itemFor(
                            type
                    )
            ) > 0) {
                result.add(
                        type
                );
            }
        }

        return result;
    }

    private ComponentType componentAtCatalog(
            double mouseX,
            double mouseY
    ) {
        int x =
                leftPos + 11;

        int y =
                topPos + TOP + 27;

        for (ComponentType type :
                visibleComponents()) {
            if (inside(
                    mouseX,
                    mouseY,
                    x,
                    y,
                    CATALOG_WIDTH - 17,
                    23
            )) {
                return type;
            }

            y +=
                    27;
        }

        return null;
    }

    private int cellAt(
            double mouseX,
            double mouseY
    ) {
        if (!insideCanvas(
                mouseX,
                mouseY
        )) {
            return -1;
        }

        double x =
                (
                        mouseX
                                - canvasX()
                                - panX
                )
                        / zoom;

        double y =
                (
                        mouseY
                                - canvasY()
                                - panY
                )
                        / zoom;

        int cellX =
                (int) Math.floor(
                        x / BOARD_CELL
                );

        int cellY =
                (int) Math.floor(
                        y / BOARD_CELL
                );

        return CircuitBoardData.cell(
                cellX,
                cellY
        );
    }

    private boolean connectorAt(
            int cell,
            double mouseX,
            double mouseY
    ) {
        double boardX =
                (
                        mouseX
                                - canvasX()
                                - panX
                )
                        / zoom;

        double boardY =
                (
                        mouseY
                                - canvasY()
                                - panY
                )
                        / zoom;

        int x =
                CircuitBoardData.cellX(
                        cell
                )
                        * BOARD_CELL
                        + BOARD_CELL
                        - 12;

        int y =
                CircuitBoardData.cellY(
                        cell
                )
                        * BOARD_CELL
                        + BOARD_CELL
                        - 12;

        return inside(
                boardX,
                boardY,
                x - 3,
                y - 3,
                14,
                14
        );
    }

    private int inventoryCount(
            Item item
    ) {
        if (minecraft == null
                || minecraft.player == null) {
            return 0;
        }

        int total =
                0;

        for (ItemStack stack :
                minecraft.player
                        .getInventory()
                        .items) {
            if (stack.is(
                    item
            )) {
                total +=
                        stack.getCount();
            }
        }

        return total;
    }

    private boolean creative() {
        return minecraft != null
                && minecraft.player != null
                && minecraft.player
                        .getAbilities()
                        .instabuild;
    }

    private void send(
            int action,
            int ordinal,
            int a,
            int b
    ) {
        PacketDistributor.sendToServer(
                new CircuitWorkbenchActionC2SPayload(
                        menu.workbenchPos()
                                .asLong(),
                        action,
                        ordinal,
                        a,
                        b
                )
        );
    }

    private String componentName(
            ComponentType type
    ) {
        ItemStack stack =
                new ItemStack(
                        ElectronicsContent.itemFor(
                                type
                        )
                );

        if (!stack.isEmpty()) {
            return stack.getHoverName()
                    .getString();
        }

        return type.name()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private int canvasX() {
        return leftPos
                + CATALOG_WIDTH;
    }

    private int canvasY() {
        return topPos
                + TOP;
    }

    private int canvasWidth() {
        return imageWidth
                - CATALOG_WIDTH
                - PROPERTY_WIDTH;
    }

    private int canvasHeight() {
        return imageHeight
                - TOP
                - 5;
    }

    private boolean insideCanvas(
            double x,
            double y
    ) {
        return inside(
                x,
                y,
                canvasX(),
                canvasY(),
                canvasWidth(),
                canvasHeight()
        );
    }

    private static boolean inside(
            double x,
            double y,
            double left,
            double top,
            double width,
            double height
    ) {
        return x >= left
                && y >= top
                && x < left + width
                && y < top + height;
    }
}
