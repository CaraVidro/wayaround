package net.caravidro.wayaround.spectrum;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Single inventory/access bridge for Spectrums.
 * If ownership later becomes persistent world state, callers do not need to
 * be rewritten: only this bridge changes.
 */
public final class SpectrumAccess {
    private SpectrumAccess() {
    }

    public static boolean isSpectrum(ItemStack stack, SpectrumType type) {
        return !stack.isEmpty()
                && stack.getItem() instanceof SpectrumItem spectrum
                && spectrum.spectrumType() == type;
    }

    public static boolean has(Player player, SpectrumType type) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isSpectrum(player.getInventory().getItem(slot), type)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack removeFirst(Player player, SpectrumType type) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isSpectrum(stack, type)) {
                continue;
            }

            ItemStack removed = stack.copyWithCount(1);
            stack.shrink(1);
            player.getInventory().setChanged();
            return removed;
        }

        return ItemStack.EMPTY;
    }
}
