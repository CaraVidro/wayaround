package net.caravidro.wayaround.assembly;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class AssemblyStackData {
    private static final String ASSEMBLY_KEY = "WayAroundAssembly";
    private static final String PART_KEY = "WayAroundAssemblyPart";

    private AssemblyStackData() {}

    public static void writeAssembly(ItemStack stack, AssemblyState state) {
        CompoundTag custom = customTag(stack);
        custom.put(ASSEMBLY_KEY, state.save());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, custom);
    }

    public static AssemblyState readAssembly(ItemStack stack) {
        CompoundTag custom = customTag(stack);
        if (!custom.contains(ASSEMBLY_KEY, Tag.TAG_COMPOUND)) return null;
        return AssemblyState.load(custom.getCompound(ASSEMBLY_KEY));
    }

    public static void writePart(ItemStack stack, AssemblyPart part) {
        CompoundTag custom = customTag(stack);
        custom.put(PART_KEY, part.save());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, custom);
    }

    public static AssemblyPart readPart(ItemStack stack) {
        CompoundTag custom = customTag(stack);
        if (!custom.contains(PART_KEY, Tag.TAG_COMPOUND)) return null;
        return AssemblyPart.load(custom.getCompound(PART_KEY));
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
