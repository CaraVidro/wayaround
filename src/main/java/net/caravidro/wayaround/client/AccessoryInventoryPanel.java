package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.accessory.AccessoryWear;
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
 * Accessory sockets around the vanilla player preview.
 *
 * V1.1.1 expands the original four cosmetic sockets into body-aware slots so
 * complete kits can coexist: hat + goggles + coat + gloves + trousers + boots
 * + cape + utility piece.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryInventoryPanel {

    private static final int SLOT_SIZE =
            18;

    // Eight sockets occupy a separate strip above the vanilla inventory.
    // They never intercept crafting, armor, recipe-book or storage slots.
    private static final int[] SLOT_X = {8,28,48,68,88,108,128,148};
    private static final int[] SLOT_Y = {-24,-24,-24,-24,-24,-24,-24,-24};

    private static final String[] EMPTY_MARKS = {
            "H",
            "O",
            "M",
            "T",
            "L",
            "P",
            "C",
            "+"
    };

    private AccessoryInventoryPanel() {
    }

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

        AccessorySlot[] slots =
                AccessorySlot.values();

        for (int ordinal = 0;
             ordinal < slots.length;
             ordinal++) {
            drawSlot(
                    graphics,
                    minecraft,
                    state,
                    slots[ordinal],
                    ordinal,
                    left + SLOT_X[ordinal],
                    top + SLOT_Y[ordinal],
                    event.getMouseX(),
                    event.getMouseY()
            );
        }
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Minecraft minecraft,
            AccessoryClientState.State state,
            AccessorySlot slot,
            int ordinal,
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

        AccessoryKind kind =
                state == null
                        ? null
                        : state.kind(
                                slot
                        );

        ItemStack stack =
                kind == null
                        ? ItemStack.EMPTY
                        : OddityContent.accessoryStack(
                                kind
                        );

        if (!stack.isEmpty()
                && state != null) {
            AccessoryWear.setWear(
                    stack,
                    kind,
                    state.wear(
                            slot
                    )
            );

            AccessoryWear.setGlassState(
                    stack,
                    state.glass(
                            slot
                    )
            );

            graphics.renderItem(
                    stack,
                    x + 1,
                    y + 1
            );
        } else {
            graphics.drawString(
                    minecraft.font,
                    EMPTY_MARKS[ordinal],
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

            graphics.renderTooltip(
                    minecraft.font,
                    Component.translatable(
                            slot.translationKey()
                    ),
                    (int) mouseX,
                    (int) mouseY
            );
        }
    }

    @SubscribeEvent
    public static void click(
            ScreenEvent.MouseButtonPressed.Pre event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.ACCESSORIES) || !(event.getScreen()
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

        AccessorySlot[] slots =
                AccessorySlot.values();

        for (int ordinal = 0;
             ordinal < slots.length;
             ordinal++) {
            int x =
                    left + SLOT_X[ordinal];

            int y =
                    top + SLOT_Y[ordinal];

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
                            (byte) ordinal,
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
}
