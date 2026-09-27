package net.caravidro.wayaround.media.broadcast;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persistent broadcast attachment data stored directly on a Camera item. */
public final class BroadcastCameraData {

    private static final String INTEGRATED =
            "WayAroundBroadcastIntegratedAntenna";

    private static final String FREQUENCY =
            "WayAroundBroadcastFrequencyKHz";

    private BroadcastCameraData() {}

    public static boolean hasIntegratedAntenna(
            ItemStack stack
    ) {
        CustomData custom =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        return custom != null
                && custom.copyTag()
                        .getBoolean(
                                INTEGRATED
                        );
    }

    public static int frequencyKHz(
            ItemStack stack
    ) {
        CustomData custom =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (custom == null) {
            return BroadcastFrequency.DEFAULT_KHZ;
        }

        CompoundTag tag =
                custom.copyTag();

        return BroadcastFrequency.clamp(
                tag.contains(
                        FREQUENCY
                )
                        ? tag.getInt(
                                FREQUENCY
                        )
                        : BroadcastFrequency.DEFAULT_KHZ
        );
    }

    public static boolean installAntenna(
            ItemStack stack
    ) {
        if (hasIntegratedAntenna(
                stack
        )) {
            return false;
        }

        CompoundTag tag =
                customTag(
                        stack
                );

        tag.putBoolean(
                INTEGRATED,
                true
        );

        if (!tag.contains(
                FREQUENCY
        )) {
            tag.putInt(
                    FREQUENCY,
                    BroadcastFrequency.DEFAULT_KHZ
            );
        }

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(
                        tag
                )
        );

        return true;
    }

    public static int tune(
            ItemStack stack,
            int direction
    ) {
        CompoundTag tag =
                customTag(
                        stack
                );

        int next =
                BroadcastFrequency.step(
                        frequencyKHz(
                                stack
                        ),
                        direction
                );

        tag.putInt(
                FREQUENCY,
                next
        );

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(
                        tag
                )
        );

        return next;
    }

    public static void write(
            ItemStack stack,
            boolean integrated,
            int frequencyKHz
    ) {
        CompoundTag tag =
                customTag(
                        stack
                );

        tag.putBoolean(
                INTEGRATED,
                integrated
        );

        tag.putInt(
                FREQUENCY,
                BroadcastFrequency.clamp(
                        frequencyKHz
                )
        );

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(
                        tag
                )
        );
    }

    private static CompoundTag customTag(
            ItemStack stack
    ) {
        CustomData custom =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        return custom == null
                ? new CompoundTag()
                : custom.copyTag();
    }
}
