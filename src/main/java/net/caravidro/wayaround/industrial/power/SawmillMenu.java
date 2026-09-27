package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.IndustrialContent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class SawmillMenu
        extends AbstractContainerMenu {

    @Nullable
    private final SawmillBlockEntity machine;

    private final ContainerData data;

    public SawmillMenu(
            int id,
            Inventory inventory
    ) {
        this(
                id,
                inventory,
                null,
                new SimpleContainerData(
                        3
                )
        );
    }

    public SawmillMenu(
            int id,
            Inventory inventory,
            SawmillBlockEntity machine
    ) {
        this(
                id,
                inventory,
                machine,
                machine.menuData()
        );
    }

    private SawmillMenu(
            int id,
            Inventory inventory,
            @Nullable SawmillBlockEntity machine,
            ContainerData data
    ) {
        super(
                IndustrialContent.SAWMILL_MENU.get(),
                id
        );

        this.machine =
                machine;

        this.data =
                data;

        addDataSlots(
                data
        );
    }

    public int selectedRecipe() {
        return data.get(
                0
        );
    }

    public int progressPercent() {
        return Math.clamp(
                data.get(1)
                        / 10,
                0,
                100
        );
    }

    public boolean manualCranking() {
        return data.get(
                2
        ) > 0;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int id
    ) {
        if (id < 0
                || id >= SawmillBlockEntity.SawmillRecipe.values().length) {
            return false;
        }

        if (machine != null) {
            machine.selectRecipe(
                    id
            );
        }

        return true;
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        if (machine == null
                || machine.getLevel() == null) {
            return true;
        }

        return player.distanceToSqr(
                machine.getBlockPos()
                        .getX()
                        + 0.5,
                machine.getBlockPos()
                        .getY()
                        + 0.5,
                machine.getBlockPos()
                        .getZ()
                        + 0.5
        )
                <= 64.0;
    }
}
