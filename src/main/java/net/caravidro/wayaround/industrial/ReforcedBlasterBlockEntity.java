package net.caravidro.wayaround.industrial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ReforcedBlasterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final MachineEnergy energy = new MachineEnergy();
    private final RecipeManager.CachedCheck<SingleRecipeInput, BlastingRecipe> blasting = RecipeManager.createCheck(RecipeType.BLASTING);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smelting = RecipeManager.createCheck(RecipeType.SMELTING);
    private final Set<ResourceLocation> usedRecipes = new HashSet<>();
    private ResourceLocation processingRecipe;
    private int progress;
    private int total = 80;
    private float experience;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) { case 0 -> energy.getEnergyStored(); case 1 -> BlasterCycle.CAPACITY; case 2 -> progress; case 3 -> total; default -> 0; };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public ReforcedBlasterBlockEntity(BlockPos pos, BlockState state) { super(IndustrialContent.BLASTER_ENTITY.get(), pos, state); }
    public IEnergyStorage energy() { return energy; }
    @Override public int getContainerSize() { return 2; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; progress = 0; processingRecipe = null; }
    @Override protected Component getDefaultName() { return Component.translatable("block.wayaround.reforced_blaster"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ReforcedBlasterMenu(id, inventory, this, data); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{1} : new int[]{0}; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && side != Direction.DOWN; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && !ItemStack.isSameItemSameComponents(items.get(0), stack)) {
            progress = 0;
            processingRecipe = null;
        }
        super.setItem(slot, stack);
    }

    public static boolean canSmelt(Level level, ItemStack stack) {
        if (stack.isEmpty()) return false;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, input, level).isPresent()
                || level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).isPresent();
    }

    private RecipeHolder<? extends AbstractCookingRecipe> findRecipe(ServerLevel level, SingleRecipeInput input) {
        var blast = blasting.getRecipeFor(input, level);
        return blast.isPresent() ? blast.get() : smelting.getRecipeFor(input, level).orElse(null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReforcedBlasterBlockEntity machine) {
        if (!(level instanceof ServerLevel server)) return;
        SingleRecipeInput input = new SingleRecipeInput(machine.items.get(0));
        RecipeHolder<? extends AbstractCookingRecipe> recipe = input.isEmpty() ? null : machine.findRecipe(server, input);
        boolean running = false;
        if (recipe == null) {
            if (machine.progress != 0 || machine.processingRecipe != null) {
                machine.progress = 0;
                machine.processingRecipe = null;
                machine.setChanged();
            }
        } else {
            if (!recipe.id().equals(machine.processingRecipe)) {
                machine.progress = 0;
                machine.processingRecipe = recipe.id();
                machine.setChanged();
            }
            machine.total = Math.min(32_000, BlasterCycle.duration(recipe.value().getCookingTime()));
            ItemStack result = recipe.value().assemble(input, level.registryAccess());
            ItemStack output = machine.items.get(1);
            boolean accepts = !result.isEmpty() && result.getCount() <= result.getMaxStackSize()
                    && (output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                    && output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), machine.getMaxStackSize())));
            BlasterCycle.Step step = BlasterCycle.step(machine.progress, machine.total, machine.energy.getEnergyStored(), accepts);
            if (step.energyUsed() > 0) {
                running = true;
                machine.energy.consume(step.energyUsed());
                machine.progress = step.progress();
                if (step.finished()) {
                    if (output.isEmpty()) machine.items.set(1, result.copy());
                    else output.grow(result.getCount());
                    machine.items.get(0).shrink(1);
                    machine.experience += recipe.value().getExperience();
                    machine.usedRecipes.add(recipe.id());
                }
                machine.setChanged();
            }
        }
        if (state.getValue(ReforcedBlasterBlock.LIT) != running) {
            level.setBlock(pos, state.setValue(ReforcedBlasterBlock.LIT, running), 3);
        }
    }

    public void popExperience(ServerLevel level, Vec3 position, ServerPlayer player) {
        int xp = (int) Math.floor(experience);
        if (level.random.nextFloat() < experience - xp) xp++;
        if (xp > 0) ExperienceOrb.award(level, position, xp);
        if (player != null) {
            var recipes = new ArrayList<RecipeHolder<?>>();
            for (ResourceLocation id : usedRecipes) level.getRecipeManager().byKey(id).ifPresent(recipes::add);
            player.awardRecipes(recipes);
        }
        experience = 0;
        usedRecipes.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("Total", total);
        tag.putFloat("Experience", experience);
        if (processingRecipe != null) tag.putString("ProcessingRecipe", processingRecipe.toString());
        ListTag used = new ListTag();
        for (ResourceLocation id : usedRecipes) used.add(StringTag.valueOf(id.toString()));
        tag.put("UsedRecipes", used);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        energy.restore(tag.getInt("Energy"));
        total = Math.clamp(tag.getInt("Total"), 1, 32_000);
        progress = Math.clamp(tag.getInt("Progress"), 0, total - 1);
        experience = Math.max(0, tag.getFloat("Experience"));
        processingRecipe = ResourceLocation.tryParse(tag.getString("ProcessingRecipe"));
        usedRecipes.clear();
        for (Tag value : tag.getList("UsedRecipes", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) usedRecipes.add(id);
        }
    }

    private final class MachineEnergy extends EnergyStorage {
        private MachineEnergy() { super(BlasterCycle.CAPACITY, 4096, 0); }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
        private void consume(int amount) { energy -= amount; }
        private void restore(int amount) { energy = Math.clamp(amount, 0, capacity); }
    }

}
