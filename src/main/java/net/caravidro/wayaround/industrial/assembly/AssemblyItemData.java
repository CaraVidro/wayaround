package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
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

    private static final String PART_PROFILE_KEY =
            "WayAroundAssemblyPart";

    private static final String PRIMITIVE_ASSEMBLY_KEY =
            "WayAroundPrimitiveAssembly";

    private static final String PROCESS_KEY =
            "WayAroundManufacturingProcess";

    private static final String MATERIAL_MEMORY_KEY =
            "WayAroundMaterialMemory";

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

        AssemblyPartProfile profile =
                readPart(source);

        if (profile != null) {
            profile.setWearFraction(
                    Mth.clamp(
                            wear / (float) MAX_COMPONENT_WEAR,
                            0.0F,
                            1.0F
                    )
            );
            writePart(
                    stack,
                    profile
            );
        }

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

    public static void writePart(
            ItemStack stack,
            AssemblyPartProfile profile
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.put(
                        PART_PROFILE_KEY,
                        profile.save()
                )
        );
    }

    public static AssemblyPartProfile readPart(
            ItemStack stack
    ) {
        CompoundTag tag =
                customData(stack);

        if (!tag.contains(
                PART_PROFILE_KEY,
                net.minecraft.nbt.Tag.TAG_COMPOUND
        )) {
            return null;
        }

        return AssemblyPartProfile.load(
                tag.getCompound(
                        PART_PROFILE_KEY
                )
        );
    }

    public static AssemblyPartProfile profileOrCreate(
            ItemStack stack,
            AssemblyPartProfile.Kind kind,
            AssemblyPartProfile.Material material,
            int orientation,
            net.minecraft.util.RandomSource random
    ) {
        AssemblyPartProfile existing =
                readPart(stack);

        if (existing != null) {
            return existing.copy();
        }

        return AssemblyPartProfile.fresh(
                kind,
                material,
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                ),
                orientation,
                random
        );
    }

    public static AssemblyPartProfile.Material inferMaterial(
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

        if (path.contains("diamond")) {
            return AssemblyPartProfile.Material.DIAMOND;
        }

        if (path.contains("steel")
                || path.contains("netherite")) {
            return AssemblyPartProfile.Material.STEEL;
        }

        if (path.contains("iron")) {
            return AssemblyPartProfile.Material.IRON;
        }

        if (path.contains("bronze")) {
            return AssemblyPartProfile.Material.BRONZE;
        }

        if (path.contains("copper")) {
            return AssemblyPartProfile.Material.COPPER;
        }

        if (path.contains("string")
                || path.contains("rope")
                || path.contains("fiber")
                || path.contains("wool")) {
            return AssemblyPartProfile.Material.FIBER;
        }

        if (path.contains("plank")
                || path.contains("wood")
                || path.contains("log")
                || path.contains("stem")
                || path.contains("pulley")
                || path.contains("sawmill")) {
            return AssemblyPartProfile.Material.WOOD;
        }

        if (path.contains("stone")
                || path.contains("flint")
                || path.contains("deepslate")) {
            return AssemblyPartProfile.Material.STONE;
        }

        return AssemblyPartProfile.Material.IRON;
    }

    public static AssemblyPartProfile ensurePart(
            ItemStack stack,
            AssemblyPartProfile.Kind kind,
            int orientation,
            net.minecraft.util.RandomSource random
    ) {
        AssemblyPartProfile existing =
                readPart(
                        stack
                );

        if (existing != null) {
            return existing;
        }

        AssemblyPartProfile created =
                AssemblyPartProfile.fresh(
                        kind,
                        inferMaterial(
                                stack
                        ),
                        BuiltInRegistries.ITEM.getKey(
                                stack.getItem()
                        ),
                        orientation,
                        random
                );

        writePart(
                stack,
                created
        );

        return created;
    }

    public static void writeProcessStamp(
            ItemStack stack,
            String process,
            float quality,
            float machineCondition,
            long gameTime
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    CompoundTag processTag =
                            new CompoundTag();

                    processTag.putString(
                            "Process",
                            process
                    );

                    processTag.putFloat(
                            "Quality",
                            Mth.clamp(
                                    quality,
                                    0.0F,
                                    1.0F
                            )
                    );

                    processTag.putFloat(
                            "MachineCondition",
                            Mth.clamp(
                                    machineCondition,
                                    0.0F,
                                    1.0F
                            )
                    );

                    processTag.putLong(
                            "GameTime",
                            gameTime
                    );

                    tag.put(
                            PROCESS_KEY,
                            processTag
                    );
                }
        );
    }

    public static CompoundTag readProcessStamp(
            ItemStack stack
    ) {
        CompoundTag tag =
                customData(
                        stack
                );

        return tag.contains(
                PROCESS_KEY,
                net.minecraft.nbt.Tag.TAG_COMPOUND
        )
                ? tag.getCompound(
                        PROCESS_KEY
                )
                : new CompoundTag();
    }

    public static MaterialMemory readMaterialMemory(
            ItemStack stack
    ) {
        CompoundTag tag =
                customData(
                        stack
                );

        if (!tag.contains(
                MATERIAL_MEMORY_KEY,
                net.minecraft.nbt.Tag.TAG_COMPOUND
        )) {
            return null;
        }

        return MaterialMemory.load(
                tag.getCompound(
                        MATERIAL_MEMORY_KEY
                )
        );
    }

    public static MaterialMemory materialMemoryOrCreate(
            ItemStack stack,
            long gameTime
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        if (memory == null) {
            memory =
                    MaterialMemory.fresh(
                            gameTime
                    );

            writeMaterialMemory(
                    stack,
                    memory
            );
        }

        return memory;
    }

    public static void writeMaterialMemory(
            ItemStack stack,
            MaterialMemory memory
    ) {
        if (stack == null
                || stack.isEmpty()
                || memory == null) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.put(
                        MATERIAL_MEMORY_KEY,
                        memory.save()
                )
        );
    }

    public static void observeMaterialUse(
            ItemStack stack,
            AssemblyPartProfile.Material material,
            long gameTime,
            float loadRatio,
            float vibration,
            float heat
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return;
        }

        MaterialMemory memory =
                materialMemoryOrCreate(
                        stack,
                        gameTime
                );

        memory.observeMechanicalUse(
                material,
                loadRatio,
                vibration,
                heat
        );

        writeMaterialMemory(
                stack,
                memory
        );
    }

    public static void exposeMaterialWet(
            ItemStack stack,
            AssemblyPartProfile.Material material,
            long gameTime,
            float wetness,
            boolean salty
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return;
        }

        MaterialMemory memory =
                materialMemoryOrCreate(
                        stack,
                        gameTime
                );

        memory.exposeWet(
                material,
                wetness,
                salty
        );

        writeMaterialMemory(
                stack,
                memory
        );
    }

    public static void serviceMaterialMemory(
            ItemStack stack,
            long gameTime,
            float effectiveness
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        if (memory == null) {
            return;
        }

        AssemblyPartProfile profile =
                readPart(
                        stack
                );

        AssemblyPartProfile.Material material =
                profile == null
                        ? inferMaterial(
                                stack
                        )
                        : profile.material();

        memory.service(
                material,
                gameTime,
                effectiveness
        );

        writeMaterialMemory(
                stack,
                memory
        );
    }

    public static float materialCondition(
            ItemStack stack
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        return memory == null
                ? 1.0F
                : memory.conditionFactor();
    }

    public static float materialConductivity(
            ItemStack stack
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        return memory == null
                ? 1.0F
                : memory.conductivityFactor();
    }

    public static float materialMechanicalIntegrity(
            ItemStack stack
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        return memory == null
                ? 1.0F
                : memory.mechanicalIntegrityFactor();
    }

    public static float materialFatigueDamage(
            ItemStack stack
    ) {
        MaterialMemory memory =
                readMaterialMemory(
                        stack
                );

        return memory == null
                ? 0.0F
                : memory.fatigueDamage();
    }

    public static void writeAssembly(
            ItemStack stack,
            PrimitiveAssemblyState state
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.put(
                        PRIMITIVE_ASSEMBLY_KEY,
                        state.save()
                )
        );
    }

    public static PrimitiveAssemblyState readAssembly(
            ItemStack stack
    ) {
        CompoundTag tag =
                customData(stack);

        if (!tag.contains(
                PRIMITIVE_ASSEMBLY_KEY,
                net.minecraft.nbt.Tag.TAG_COMPOUND
        )) {
            return null;
        }

        return PrimitiveAssemblyState.load(
                tag.getCompound(
                        PRIMITIVE_ASSEMBLY_KEY
                )
        );
    }

    public static ItemStack withProfile(
            ItemStack source,
            AssemblyPartProfile profile
    ) {
        ItemStack stack =
                source.copyWithCount(1);

        writePart(
                stack,
                profile
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
