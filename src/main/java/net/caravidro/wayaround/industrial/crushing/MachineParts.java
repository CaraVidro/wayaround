package net.caravidro.wayaround.industrial.crushing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.caravidro.wayaround.industrial.assembly.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;

/** Actual installed stacks, shared by crushers and the existing mill. Not an Assembly replacement. */
public final class MachineParts {
    private final String family;
    private final ItemStack[] parts = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    public MachineParts(String family) { this.family = family; }
    public ItemStack stack(MachinePartSpec.Role role) { return parts[role.ordinal()]; }
    public boolean has(MachinePartSpec.Role role) { return !stack(role).isEmpty(); }
    public boolean complete() { for (var role : MachinePartSpec.Role.values()) if (!has(role)) return false; return true; }
    public boolean operable() {
        return complete() && condition(MachinePartSpec.Role.DRIVE) > .12F
            && condition(MachinePartSpec.Role.BEARING) > .12F && condition(MachinePartSpec.Role.TOOL) > .12F;
    }
    public boolean accepts(ItemStack held) {
        return held.getItem() instanceof MachinePartItem item && item.spec().fits(family);
    }
    public boolean install(Player player, ItemStack held) {
        if (!accepts(held)) return false;
        var item = (MachinePartItem) held.getItem();
        int slot = item.spec().role().ordinal();
        if (!parts[slot].isEmpty()) return false;
        parts[slot] = held.copyWithCount(1);
        var profile = AssemblyItemData.profileOrCreate(parts[slot], kind(item.spec().role()),
            item.spec().heavy() ? AssemblyPartProfile.Material.IRON : material(item.spec()), 0, player.getRandom());
        AssemblyItemData.writePart(parts[slot], profile);
        if (!player.getAbilities().instabuild) held.shrink(1);
        return true;
    }
    public ItemStack remove(MachinePartSpec.Role role) {
        ItemStack result = stack(role); parts[role.ordinal()] = ItemStack.EMPTY; return result;
    }
    public ItemStack removeLast() {
        for (int i = parts.length - 1; i >= 0; i--) if (!parts[i].isEmpty()) {
            ItemStack result = parts[i]; parts[i] = ItemStack.EMPTY; return result;
        }
        return ItemStack.EMPTY;
    }
    public MachinePartSpec spec(MachinePartSpec.Role role) {
        return stack(role).getItem() instanceof MachinePartItem item ? item.spec() : null;
    }
    public float driveCost() {
        float value = 1; for (var role : MachinePartSpec.Role.values()) {
            var spec = spec(role); if (spec != null) value *= spec.driveCost();
        } return value;
    }
    public float speed() {
        if (!complete()) return 0;
        float value = 1; for (var role : MachinePartSpec.Role.values()) {
            var profile = AssemblyItemData.readPart(stack(role));
            value *= spec(role).speed() * (profile == null ? 1 : .65F + profile.assemblyScore() * .35F);
        } return value;
    }
    public float condition(MachinePartSpec.Role role) {
        var spec = spec(role); if (spec == null) return 0;
        var profile = AssemblyItemData.readPart(stack(role));
        return spec.strength() * (profile == null ? 1 : (1 - profile.wear()) * (1 - profile.fatigue() * .4F));
    }
    public int feedMultiplier() { var feed = spec(MachinePartSpec.Role.FEED); return feed == null ? 0 : feed.feedMultiplier(); }
    public void wear(float work, float load) {
        for (var role : MachinePartSpec.Role.values()) {
            ItemStack stack = stack(role); if (stack.isEmpty()) continue;
            var profile = AssemblyItemData.readPart(stack); if (profile == null) continue;
            float multiplier = role == MachinePartSpec.Role.TOOL ? 1.2F : role == MachinePartSpec.Role.FEED ? .25F : .7F;
            profile.applyWear(work * multiplier * spec(role).abrasion() * Math.max(.25F, load));
            AssemblyItemData.writePart(stack, profile);
        }
    }
    public Collection<AssemblyPartNode> nodes(boolean supported) {
        var result = new ArrayList<AssemblyPartNode>();
        for (var role : MachinePartSpec.Role.values()) {
            ItemStack stack = stack(role); if (stack.isEmpty()) continue;
            var profile = AssemblyItemData.readPart(stack);
            if (profile == null) profile = AssemblyPartProfile.legacy(kind(role), material(spec(role)),
                BuiltInRegistries.ITEM.getKey(stack.getItem()), 0, 0);
            result.add(new AssemblyPartNode(role.name().toLowerCase(java.util.Locale.ROOT), role.name(), profile, supported, 1));
        } return result;
    }
    public Collection<AssemblyConnection> connections() {
        var result = new ArrayList<AssemblyConnection>();
        for (var role : MachinePartSpec.Role.values()) if (has(role)) result.add(new AssemblyConnection(
            "frame", role.name().toLowerCase(java.util.Locale.ROOT), role == MachinePartSpec.Role.BEARING
                ? AssemblyConnection.Type.BEARING : role == MachinePartSpec.Role.DRIVE
                ? AssemblyConnection.Type.SHAFT : AssemblyConnection.Type.FASTENED, condition(role), 0));
        if (has(MachinePartSpec.Role.DRIVE) && has(MachinePartSpec.Role.TOOL)) result.add(new AssemblyConnection(
            "drive", "tool", AssemblyConnection.Type.SHAFT, Math.min(condition(MachinePartSpec.Role.DRIVE), condition(MachinePartSpec.Role.TOOL)), 0));
        return result;
    }
    public void drop(Level level, BlockPos pos) { for (var role : MachinePartSpec.Role.values()) Block.popResource(level, pos, remove(role)); }
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag inventory = new CompoundTag();
        for (var role : MachinePartSpec.Role.values()) if (has(role)) inventory.put(role.name(), stack(role).save(registries));
        tag.put("InstalledMachineParts", inventory);
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag inventory = tag.getCompound("InstalledMachineParts");
        for (var role : MachinePartSpec.Role.values()) {
            ItemStack parsed = ItemStack.parseOptional(registries, inventory.getCompound(role.name()));
            parts[role.ordinal()] = accepts(parsed) && ((MachinePartItem) parsed.getItem()).spec().role() == role
                ? parsed.copyWithCount(1) : ItemStack.EMPTY;
        }
    }
    public void setLegacy(MachinePartSpec.Role role, ItemStack stack) {
        parts[role.ordinal()] = stack.copyWithCount(1);
        var spec = spec(role);
        AssemblyItemData.writePart(parts[role.ordinal()], AssemblyPartProfile.legacy(kind(role), material(spec),
            BuiltInRegistries.ITEM.getKey(stack.getItem()), 0, 0));
    }
    private static AssemblyPartProfile.Kind kind(MachinePartSpec.Role role) {
        return switch (role) { case DRIVE -> AssemblyPartProfile.Kind.SHAFT; case BEARING -> AssemblyPartProfile.Kind.GENERAL;
            case TOOL -> AssemblyPartProfile.Kind.HEAD; case FEED -> AssemblyPartProfile.Kind.FRAME; };
    }
    private static AssemblyPartProfile.Material material(MachinePartSpec spec) {
        if (spec.id().contains("copper")) return AssemblyPartProfile.Material.COPPER;
        if (spec.id().contains("stone")) return AssemblyPartProfile.Material.STONE;
        return AssemblyPartProfile.Material.IRON;
    }
}
