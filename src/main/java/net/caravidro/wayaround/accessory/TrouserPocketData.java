package net.caravidro.wayaround.accessory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Stores one real ItemStack inside an unequipped pair of engineer trousers.
 *
 * Equipped trousers move the same stack into player persistent data; when the
 * trousers are removed the pocket is written back into the accessory item.
 */
public final class TrouserPocketData {

    private static final String POCKET =
            "WayAroundTrouserPocket";

    private TrouserPocketData() {
    }

    public static ItemStack read(
            ItemStack trousers,
            HolderLookup.Provider registries
    ) {
        if (trousers.isEmpty()) {
            return ItemStack.EMPTY;
        }

        CustomData data =
                trousers.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                );

        CompoundTag root =
                data.copyTag();

        if (!root.contains(
                POCKET
        )) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                ItemStack.parseOptional(
                        registries,
                        root.getCompound(
                                POCKET
                        )
                );

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack single =
                stack.copy();

        single.setCount(
                1
        );

        return single;
    }

    public static void write(
            ItemStack trousers,
            ItemStack pocket,
            HolderLookup.Provider registries
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                trousers,
                root -> {
                    if (pocket == null
                            || pocket.isEmpty()) {
                        root.remove(
                                POCKET
                        );
                        return;
                    }

                    ItemStack single =
                            pocket.copy();

                    single.setCount(
                            1
                    );

                    root.put(
                            POCKET,
                            single.save(
                                    registries
                            )
                    );
                }
        );
    }
}
