package net.caravidro.wayaround.spectral;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class SpectralTraitData {

    private SpectralTraitData() {}

    private static final String TRAIT =
            "WayAroundSpectralTrait";

    private static final String DISCOVERED =
            "WayAroundSpectralTraitDiscovered";

    private static final String ARMOR_ASCENDED =
            "WayAroundSpectralArmorAscended";

    public static int ensure(
            ItemStack stack,
            RandomSource random
    ) {
        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                ).copyTag();

        if (!tag.contains(
                TRAIT
        )) {
            int chosen =
                    random.nextInt(
                            4
                    );

            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    data ->
                            data.putInt(
                                    TRAIT,
                                    chosen
                            )
            );

            return chosen;
        }

        return tag.getInt(
                TRAIT
        );
    }

    public static int trait(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                ).copyTag();

        return tag.contains(
                TRAIT
        )
                ? tag.getInt(
                TRAIT
        )
                : -1;
    }

    public static boolean discovered(
            ItemStack stack
    ) {
        return stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag()
                .getBoolean(
                        DISCOVERED
                );
    }

    public static void discover(
            ItemStack stack
    ) {
        if (discovered(
                stack
        )) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                data ->
                        data.putBoolean(
                                DISCOVERED,
                                true
                        )
        );
    }

    public static boolean armorAscended(
            ItemStack stack
    ) {
        return stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag()
                .getBoolean(
                        ARMOR_ASCENDED
                );
    }

    public static void ascendArmor(
            ItemStack stack
    ) {
        if (armorAscended(
                stack
        )) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                data ->
                        data.putBoolean(
                                ARMOR_ASCENDED,
                                true
                        )
        );
    }

    public static void appendHiddenHint(
            ItemStack stack,
            List<Component> tooltip
    ) {
        int trait =
                trait(
                        stack
                );

        if (trait < 0) {
            tooltip.add(
                    Component.literal(
                            "It has not decided what you are yet."
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
            return;
        }

        boolean known =
                discovered(
                        stack
                );

        String hint =
                switch (trait) {
                    case 0 -> known
                            ? "It strengthens where light fails."
                            : "The dark notices what you carry.";
                    case 1 -> known
                            ? "North gives it momentum."
                            : "Keep one direction longer than feels useful.";
                    case 2 -> known
                            ? "It hardens while you stand still."
                            : "Stillness is not surrender.";
                    default -> known
                            ? "Storms wake what sleeps inside it."
                            : "Bad weather is sometimes an invitation.";
                };

        tooltip.add(
                Component.literal(
                        hint
                ).withStyle(
                        known
                                ? ChatFormatting.AQUA
                                : ChatFormatting.DARK_PURPLE
                )
        );
    }
}
