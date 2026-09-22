package net.caravidro.wayaround.industrial.ship;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class ShipMenu extends AbstractContainerMenu {
    private final SailingShipEntity ship;
    private final DataSlot large = DataSlot.standalone();
    private final DataSlot anchored = DataSlot.standalone();
    public ShipMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public ShipMenu(int id, Inventory inventory, SailingShipEntity ship) {
        super(CoalShipContent.SHIP_MENU.get(), id);
        this.ship = ship;
        addDataSlot(large);
        addDataSlot(anchored);
        updateState();
    }
    private void updateState() {
        if (ship != null) {
            large.set(ship instanceof GreatShipEntity ? 1 : 0);
            anchored.set(ship.isAnchored() ? 1 : 0);
        }
    }
    public boolean isLarge() { return large.get() != 0; }
    public boolean isAnchored() { return anchored.get() != 0; }
    @Override public void broadcastChanges() { updateState(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return ship == null || ship.canUse(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean clickMenuButton(Player player, int action) {
        if (ship == null || !ship.canUse(player)) return false;
        if (action == 0) {
            ship.toggleAnchor(player);
            updateState();
            return true;
        }
        if (action == 1) {
            player.openMenu(ship);
            return true;
        }
        if (ship instanceof GreatShipEntity great) {
            if (action == 2) { great.openWorkbench(player); return true; }
            if (action == 3) { great.sleep(player); return true; }
            if (action >= 10 && action < 16) { great.board(player, action - 10); return true; }
        } else if (action == 10 && !player.isPassenger()) {
            player.closeContainer();
            player.startRiding(ship);
            return true;
        }
        return false;
    }
}
