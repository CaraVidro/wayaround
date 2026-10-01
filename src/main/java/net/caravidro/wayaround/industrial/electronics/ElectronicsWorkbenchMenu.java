package net.caravidro.wayaround.industrial.electronics;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class ElectronicsWorkbenchMenu
        extends AbstractContainerMenu {

    @Nullable
    private final ElectronicsWorkbenchBlockEntity workbench;

    private final ContainerData data;

    public ElectronicsWorkbenchMenu(
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

    public ElectronicsWorkbenchMenu(
            int id,
            Inventory inventory,
            @Nullable ElectronicsWorkbenchBlockEntity workbench,
            ContainerData data
    ) {
        super(
                ElectronicsContent.ELECTRONICS_WORKBENCH_MENU.get(),
                id
        );

        this.workbench =
                workbench;

        this.data =
                data;

        addDataSlots(
                data
        );
    }

    public BlockPos workbenchPos() {
        return new BlockPos(
                data.get(0),
                data.get(1),
                data.get(2)
        );
    }

    @Nullable
    public ElectronicsWorkbenchBlockEntity workbench() {
        return workbench;
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
        BlockPos pos =
                workbench == null
                        ? workbenchPos()
                        : workbench.getBlockPos();

        return player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= 64.0;
    }
}
