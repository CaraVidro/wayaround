package net.caravidro.wayaround.industrial;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReforcedBlasterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public ReforcedBlasterMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainerData(4));
    }

    public ReforcedBlasterMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(IndustrialContent.BLASTER_MENU.get(), id);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, 0, 44, 35));
        addSlot(new Slot(container, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                if (container instanceof ReforcedBlasterBlockEntity machine
                        && player instanceof ServerPlayer serverPlayer
                        && player.level() instanceof ServerLevel level) {
                    machine.popExperience(level, player.position(), serverPlayer);
                }
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ReforcedBlasterBlockEntity.canSmelt(player.level(), stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 2, 29, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    public int energy() { return data.get(0); }
    public int capacity() { return data.get(1); }
    public int progressPixels(int width) {
        return data.get(3) <= 0 ? 0 : Math.clamp((long) data.get(2) * width / data.get(3), 0, width);
    }
}
