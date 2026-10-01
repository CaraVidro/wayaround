package net.caravidro.wayaround.industrial.engineering;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EngineeringWorkbenchBlockEntity
        extends BlockEntity
        implements MenuProvider {

    private final ContainerData menuData =
            new ContainerData() {
                @Override
                public int get(
                        int index
                ) {
                    return switch (index) {
                        case 0 -> worldPosition.getX();
                        case 1 -> worldPosition.getY();
                        case 2 -> worldPosition.getZ();
                        default -> 0;
                    };
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                }

                @Override
                public int getCount() {
                    return 3;
                }
            };

    public EngineeringWorkbenchBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.ENGINEERING_WORKBENCH_ENTITY.get(),
                pos,
                state
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.wayaround.engineering_workbench"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player
    ) {
        return new EngineeringWorkbenchMenu(
                id,
                inventory,
                this,
                menuData
        );
    }
}
