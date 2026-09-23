package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AssemblyItemData {

    public static final int MAX_COMPONENT_WEAR = 10_000;

    public static final TagKey<Item> NAILS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "nails"
                    )
            );

    private static final String WEAR_KEY =
            "WayAroundWear";

    private AssemblyItemData() {
    }

    public static int wear(ItemStack stack) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return 0;
        }

        return Mth.clamp(
                data.copyTag()
                        .getInt(
                                WEAR_KEY
                        ),
                0,
                MAX_COMPONENT_WEAR
        );
    }

    public static ItemStack withWear(
            ItemStack source,
            int wear
    ) {
        ItemStack stack =
                source.copyWithCount(
                        1
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag ->
                        tag.putInt(
                                WEAR_KEY,
                                Mth.clamp(
                                        wear,
                                        0,
                                        MAX_COMPONENT_WEAR
                                )
                        )
        );

        return stack;
    }

    public static boolean isNail(
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(NAILS)) {
            return true;
        }

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                );

        if (id == null) {
            return false;
        }

        String path =
                id.getPath();

        return path.equals("nail")
                || path.endsWith("_nail")
                || path.startsWith("nail_");
    }

    public static int nailDurability(
            ItemStack stack
    ) {
        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                );

        String path =
                id == null
                        ? ""
                        : id.getPath();

        if (path.contains("netherite")) {
            return 60000;
        }

        if (path.contains("diamond")) {
            return 40000;
        }

        if (path.contains("steel")) {
            return 18000;
        }

        if (path.contains("iron")) {
            return 12000;
        }

        if (path.contains("copper")) {
            return 7000;
        }

        if (path.contains("gold")) {
            return 4000;
        }

        if (path.contains("wood")
                || path.contains("oak")) {
            return 2400;
        }

        return 9000;
    }

    public static int nailWear(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return 0;
        }

        return Math.max(
                0,
                data.copyTag()
                        .getInt(
                                "WayAroundNailWear"
                        )
        );
    }

    public static ItemStack withNailWear(
            ItemStack source,
            int wear
    ) {
        ItemStack stack =
                source.copyWithCount(
                        1
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag ->
                        tag.putInt(
                                "WayAroundNailWear",
                                Math.max(
                                        0,
                                        wear
                                )
                        )
        );

        return stack;
    }

    public static CompoundTag customData(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        return data == null
                ? new CompoundTag()
                : data.copyTag();
    }
}
