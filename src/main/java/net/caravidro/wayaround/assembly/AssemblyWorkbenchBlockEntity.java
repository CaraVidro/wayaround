package net.caravidro.wayaround.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AssemblyWorkbenchBlockEntity extends BlockEntity {
    private static final String STATE_KEY = "AssemblyState";
    private AssemblyState assembly = new AssemblyState();

    public AssemblyWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(AssemblyContent.ASSEMBLY_WORKBENCH_ENTITY.get(), pos, state);
    }

    public static boolean isSupportedInput(ItemStack stack) {
        return stack.is(AssemblyContent.STONE_FLAKE.get())
                || stack.is(Items.STICK)
                || stack.is(Items.STRING)
                || stack.is(AssemblyContent.PRIMITIVE_AXE.get());
    }

    public void insert(Player player, ItemStack stack, int orientation) {
        if (stack.is(AssemblyContent.PRIMITIVE_AXE.get())) {
            importTool(player, stack);
            return;
        }

        AssemblyPart.Kind kind = kindFor(stack);
        AssemblyPart.Material material = materialFor(stack);
        if (kind == null || material == null || level == null) return;

        AssemblyPart part = AssemblyStackData.readPart(stack);
        if (part == null || part.kind() != kind) {
            part = AssemblyPart.fresh(
                    kind,
                    material,
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    orientation,
                    level.getRandom()
            );
        }

        if (!AssemblyEngine.insert(assembly, part)) {
            player.displayClientMessage(Component.translatable("message.wayaround.assembly.rejected"), true);
            return;
        }

        consumeOne(player, stack);
        sync();

        player.displayClientMessage(Component.translatable(
                "message.wayaround.assembly.inserted",
                partName(part),
                part.orientation()
        ), true);
    }

    public void useHammer(Player player, boolean rotate) {
        if (assembly.isEmpty() || level == null) {
            player.displayClientMessage(Component.translatable("message.wayaround.assembly.empty"), true);
            return;
        }

        if (rotate) {
            AssemblyPart rotated = AssemblyEngine.rotateLast(assembly);
            if (rotated != null) {
                sync();
                player.displayClientMessage(Component.translatable(
                        "message.wayaround.assembly.rotated",
                        partName(rotated),
                        rotated.orientation()
                ), true);
            }
            return;
        }

        AssemblyPart hammered = AssemblyEngine.hammer(assembly, level.getRandom());
        if (hammered == null) return;

        player.displayClientMessage(Component.translatable(
                "message.wayaround.assembly.hammered",
                partName(hammered),
                percent(hammered.alignment()),
                percent(hammered.tension())
        ), true);

        if (AssemblyEngine.canFinalizePrimitiveAxe(assembly)) finishPrimitiveAxe(player);
        else sync();
    }

    public void removeLast(Player player) {
        AssemblyPart part = assembly.removeLast();
        if (part == null) {
            player.displayClientMessage(Component.translatable(
                    "message.wayaround.assembly.nothing_to_remove"
            ), true);
            return;
        }

        give(player, stackFor(part));
        sync();
        player.displayClientMessage(Component.translatable(
                "message.wayaround.assembly.removed",
                partName(part)
        ), true);
    }

    public void describe(Player player) {
        if (assembly.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wayaround.assembly.empty"), true);
            return;
        }

        player.displayClientMessage(Component.translatable(
                "message.wayaround.assembly.status",
                assembly.size(),
                percent(assembly.overallQuality()),
                percent(assembly.averageAlignment()),
                percent(assembly.averageTension())
        ), true);
    }

    public void dropParts(Level level, BlockPos pos) {
        while (!assembly.isEmpty()) {
            AssemblyPart part = assembly.removeLast();
            ItemStack stack = stackFor(part);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stack
                );
            }
        }
        setChanged();
    }

    private void importTool(Player player, ItemStack stack) {
        if (!assembly.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wayaround.assembly.rejected"), true);
            return;
        }

        AssemblyState imported = AssemblyStackData.readAssembly(stack);
        if (imported == null || imported.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wayaround.assembly.rejected"), true);
            return;
        }

        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            imported.applyToolWear((float) stack.getDamageValue() / (float) stack.getMaxDamage());
        }

        assembly = imported;
        consumeOne(player, stack);
        sync();
        player.displayClientMessage(Component.translatable("message.wayaround.assembly.imported"), true);
    }

    private void finishPrimitiveAxe(Player player) {
        AssemblyState finished = assembly.copy();
        ItemStack result = new ItemStack(AssemblyContent.PRIMITIVE_AXE.get());
        AssemblyStackData.writeAssembly(result, finished);

        int maxDamage = AssemblyEngine.primitiveAxeDurability(finished);
        result.set(DataComponents.MAX_DAMAGE, maxDamage);
        result.set(DataComponents.DAMAGE, 0);

        assembly.clear();
        give(player, result);
        sync();

        player.displayClientMessage(Component.translatable(
                "message.wayaround.assembly.completed",
                maxDamage,
                percent(finished.overallQuality())
        ), true);
    }

    private static AssemblyPart.Kind kindFor(ItemStack stack) {
        if (stack.is(AssemblyContent.STONE_FLAKE.get())) return AssemblyPart.Kind.HEAD;
        if (stack.is(Items.STICK)) return AssemblyPart.Kind.HANDLE;
        if (stack.is(Items.STRING)) return AssemblyPart.Kind.BINDING;
        return null;
    }

    private static AssemblyPart.Material materialFor(ItemStack stack) {
        if (stack.is(AssemblyContent.STONE_FLAKE.get())) return AssemblyPart.Material.STONE;
        if (stack.is(Items.STICK)) return AssemblyPart.Material.WOOD;
        if (stack.is(Items.STRING)) return AssemblyPart.Material.FIBER;
        return null;
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }

    private static ItemStack stackFor(AssemblyPart part) {
        Item item = BuiltInRegistries.ITEM.get(part.sourceItem());
        if (item == Items.AIR) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item);
        AssemblyStackData.writePart(stack, part);
        return stack;
    }

    private static Component partName(AssemblyPart part) {
        return Component.translatable(BuiltInRegistries.ITEM.get(part.sourceItem()).getDescriptionId());
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private static int percent(float value) {
        return Math.round(value * 100.0F);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(STATE_KEY, assembly.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        assembly = tag.contains(STATE_KEY, Tag.TAG_COMPOUND)
                ? AssemblyState.load(tag.getCompound(STATE_KEY))
                : new AssemblyState();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
