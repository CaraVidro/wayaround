package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.network.AccessoryActionC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryInventoryPanel {

    private AccessoryInventoryPanel() {}

    private static final int SLOT =
            22;

    private static final int PANEL_WIDTH =
            28;

    @SubscribeEvent
    public static void render(
            ScreenEvent.Render.Post event
    ) {
        if (!(event.getScreen()
                instanceof InventoryScreen)) {
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

        int[] origin =
                origin(
                        minecraft
                );

        int x =
                origin[0];

        int y =
                origin[1];

        GuiGraphics graphics =
                event.getGuiGraphics();

        graphics.fill(
                x - 2,
                y - 16,
                x + PANEL_WIDTH,
                y + SLOT * 4 + 3,
                0xD0181818
        );

        graphics.fill(
                x + 3,
                y - 13,
                x + PANEL_WIDTH - 3,
                y - 2,
                0xFF342A44
        );

        graphics.drawString(
                minecraft.font,
                "A",
                x + 10,
                y - 12,
                0xFFE7D7FF,
                false
        );

        String[] letters =
                {"H", "M", "T", "F"};

        for (int index = 0;
             index < 4;
             index++) {
            int slotY =
                    y
                            + index
                                    * SLOT;

            graphics.fill(
                    x,
                    slotY,
                    x + 20,
                    slotY + 20,
                    0xFF2D2D34
            );

            graphics.fill(
                    x + 1,
                    slotY + 1,
                    x + 19,
                    slotY + 19,
                    0xFF111116
            );

            String path =
                    state == null
                            ? ""
                            : state.forSlot(
                                    index
                            );

            ItemStack stack =
                    stackFor(
                            path
                    );

            if (!stack.isEmpty()) {
                graphics.renderItem(
                        stack,
                        x + 2,
                        slotY + 2
                );
            } else {
                graphics.drawString(
                        minecraft.font,
                        letters[index],
                        x + 7,
                        slotY + 6,
                        0xFF777783,
                        false
                );
            }

            if (event.getMouseX()
                    >= x
                    && event.getMouseX()
                    < x + 20
                    && event.getMouseY()
                    >= slotY
                    && event.getMouseY()
                    < slotY + 20) {

                graphics.fill(
                        x,
                        slotY,
                        x + 20,
                        slotY + 20,
                        0x28FFFFFF
                );
            }
        }
    }

    @SubscribeEvent
    public static void click(
            ScreenEvent.MouseButtonPressed.Pre event
    ) {
        if (!(event.getScreen()
                instanceof InventoryScreen)) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        int[] origin =
                origin(
                        minecraft
                );

        int x =
                origin[0];

        int y =
                origin[1];

        for (int index = 0;
             index < 4;
             index++) {
            int slotY =
                    y
                            + index
                                    * SLOT;

            if (event.getMouseX()
                    < x
                    || event.getMouseX()
                    >= x + 20
                    || event.getMouseY()
                    < slotY
                    || event.getMouseY()
                    >= slotY + 20) {
                continue;
            }

            byte action =
                    event.getButton() == 1
                            ? AccessoryActionC2SPayload.TOGGLE
                            : AccessoryActionC2SPayload.EQUIP_OR_UNEQUIP;

            PacketDistributor.sendToServer(
                    new AccessoryActionC2SPayload(
                            (byte) index,
                            action
                    )
            );

            event.setCanceled(
                    true
            );

            return;
        }
    }

    private static int[] origin(
            Minecraft minecraft
    ) {
        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        int inventoryLeft =
                (
                        width - 176
                )
                        / 2;

        int inventoryTop =
                (
                        height - 166
                )
                        / 2;

        return new int[]{
                inventoryLeft - 29,
                inventoryTop + 12
        };
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
