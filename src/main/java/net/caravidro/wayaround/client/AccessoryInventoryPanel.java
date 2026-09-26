package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.network.AccessoryActionC2SPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Four accessory slots embedded around the vanilla player preview.
 *
 * Vanilla offhand sits at inventory-relative (77,62). The accessory column
 * deliberately uses y=8/26/44/80, leaving the offhand slot itself untouched
 * in the middle of the stack.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryInventoryPanel {

    private AccessoryInventoryPanel() {}

    private static final int SLOT_SIZE =
            18;

    private static final int SLOT_X =
            77;

    // AccessorySlot ordinal order: HEAD, HANDS, TORSO, FEET.
    private static final int[] SLOT_Y = {
            8,
            44,
            26,
            80
    };

    private static final String[] EMPTY_MARKS = {
            "H",
            "M",
            "T",
            "F"
    };

    @SubscribeEvent
    public static void render(
            ScreenEvent.Render.Post event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ACCESSORIES
        )
                || !(event.getScreen()
                instanceof InventoryScreen screen)) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        AccessoryClientState.State state =
                AccessoryClientState.get(
                        minecraft.player.getUUID()
                );

        int left =
                screen.getGuiLeft();

        int top =
                screen.getGuiTop();

        GuiGraphics graphics =
                event.getGuiGraphics();

        for (int slot = 0;
             slot < 4;
             slot++) {
            int x =
                    left
                            + SLOT_X;

            int y =
                    top
                            + SLOT_Y[slot];

            drawSlot(
                    graphics,
                    minecraft,
                    state,
                    slot,
                    x,
                    y,
                    event.getMouseX(),
                    event.getMouseY()
            );
        }
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Minecraft minecraft,
            AccessoryClientState.State state,
            int slot,
            int x,
            int y,
            double mouseX,
            double mouseY
    ) {
        boolean hovered =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y
                );

        /*
         * Three-layer border mirrors vanilla's recessed inventory language
         * without adding another detached panel.
         */
        graphics.fill(
                x,
                y,
                x + SLOT_SIZE,
                y + SLOT_SIZE,
                0xFF8B8B8B
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + SLOT_SIZE - 1,
                y + SLOT_SIZE - 1,
                0xFF373737
        );

        graphics.fill(
                x + 2,
                y + 2,
                x + SLOT_SIZE - 2,
                y + SLOT_SIZE - 2,
                hovered
                        ? 0xFF25252D
                        : 0xFF15151A
        );

        String path =
                state == null
                        ? ""
                        : state.forSlot(
                                slot
                        );

        ItemStack stack =
                stackFor(
                        path
                );

        if (!stack.isEmpty()) {
            graphics.renderItem(
                    stack,
                    x + 1,
                    y + 1
            );
        } else {
            graphics.drawString(
                    minecraft.font,
                    EMPTY_MARKS[slot],
                    x + 6,
                    y + 5,
                    hovered
                            ? 0xFFA996C9
                            : 0xFF686873,
                    false
            );
        }

        if (hovered) {
            graphics.fill(
                    x + 1,
                    y + 1,
                    x + SLOT_SIZE - 1,
                    y + SLOT_SIZE - 1,
                    0x22FFFFFF
            );

            Component label =
                    switch (slot) {
                        case 0 ->
                                Component.literal(
                                        "Accessory: Head"
                                );
                        case 1 ->
                                Component.literal(
                                        "Accessory: Hands"
                                );
                        case 2 ->
                                Component.literal(
                                        "Accessory: Torso"
                                );
                        default ->
                                Component.literal(
                                        "Accessory: Feet"
                                );
                    };

            graphics.renderTooltip(
                    minecraft.font,
                    label,
                    (int) mouseX,
                    (int) mouseY
            );
        }
    }

    @SubscribeEvent
    public static void click(
            ScreenEvent.MouseButtonPressed.Pre event
    ) {
        if (!(event.getScreen()
                instanceof InventoryScreen screen)) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        int left =
                screen.getGuiLeft();

        int top =
                screen.getGuiTop();

        for (int slot = 0;
             slot < 4;
             slot++) {
            int x =
                    left
                            + SLOT_X;

            int y =
                    top
                            + SLOT_Y[slot];

            if (!inside(
                    event.getMouseX(),
                    event.getMouseY(),
                    x,
                    y
            )) {
                continue;
            }

            byte action =
                    event.getButton() == 1
                            ? AccessoryActionC2SPayload.TOGGLE
                            : AccessoryActionC2SPayload.EQUIP_OR_UNEQUIP;

            PacketDistributor.sendToServer(
                    new AccessoryActionC2SPayload(
                            (byte) slot,
                            action
                    )
            );

            event.setCanceled(
                    true
            );

            return;
        }
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y
    ) {
        return mouseX >= x
                && mouseX < x + SLOT_SIZE
                && mouseY >= y
                && mouseY < y + SLOT_SIZE;
    }

    private static ItemStack stackFor(
            String path
    ) {
        if (path == null
                || path.isBlank()) {
            return ItemStack.EMPTY;
        }

        return switch (path) {
            case "spectral_glasses" ->
                    OddityContent.SPECTRAL_GLASSES.get()
                            .getDefaultInstance();
            case "work_gloves" ->
                    OddityContent.WORK_GLOVES.get()
                            .getDefaultInstance();
            case "engineer_cape" ->
                    OddityContent.ENGINEER_CAPE.get()
                            .getDefaultInstance();
            case "wind_boots" ->
                    OddityContent.WIND_BOOTS.get()
                            .getDefaultInstance();
            default ->
                    ItemStack.EMPTY;
        };
    }
}
