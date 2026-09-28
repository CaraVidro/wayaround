package net.caravidro.wayaround.accessory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AccessoryWear {

    private static final String GLASS_STATE =
            "WayAroundAccessoryGlassState";

    private AccessoryWear() {
    }

    public static int stage(
            AccessoryKind kind,
            int wear
    ) {
        if (kind == null) {
            return 0;
        }

        float ratio =
                Mth.clamp(
                        wear
                                / (float) Math.max(
                                1,
                                kind.maxWear() - 1
                        ),
                        0.0F,
                        1.0F
                );

        if (ratio >= 0.72F) {
            return 2;
        }

        if (ratio >= 0.34F) {
            return 1;
        }

        return 0;
    }

    public static int stage(
            ItemStack stack,
            AccessoryKind kind
    ) {
        return stage(
                kind,
                stack.getDamageValue()
        );
    }

    public static int glassState(
            ItemStack stack
    ) {
        CustomData data =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                );

        CompoundTag tag =
                data.copyTag();

        return Mth.clamp(
                tag.getInt(
                        GLASS_STATE
                ),
                0,
                2
        );
    }

    public static void setGlassState(
            ItemStack stack,
            int state
    ) {
        int clamped =
                Mth.clamp(
                        state,
                        0,
                        2
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putInt(
                        GLASS_STATE,
                        clamped
                )
        );
    }

    public static void setWear(
            ItemStack stack,
            AccessoryKind kind,
            int wear
    ) {
        if (kind == null) {
            return;
        }

        stack.setDamageValue(
                Mth.clamp(
                        wear,
                        0,
                        Math.max(
                                0,
                                kind.maxWear() - 1
                        )
                )
        );
    }
}
