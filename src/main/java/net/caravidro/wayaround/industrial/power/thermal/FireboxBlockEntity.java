package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FireboxBlockEntity extends BlockEntity implements HeatSource {
    private int coal;
    private int burnTicks;
    private int nextSide;

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.FIREBOX_ENTITY.get(), pos, state);
    }

    public boolean addCoal() {
        if (coal >= 64) return false;
        coal++;
        setChanged();
        return true;
    }

    public int storedCoal() { return coal; }
    public boolean isBurning() { return burnTicks > 0; }

    @Override
    public int heatOutputPerTick() {
        return isBurning() ? ThermalUnits.FIREBOX_HEAT_PER_TICK : 0;
    }

    public Component status() {
        return Component.translatable("message.wayaround.firebox.status", coal,
            Component.translatable(isBurning() ? "message.wayaround.firebox.burning" : "message.wayaround.firebox.idle"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
        if (!(level instanceof ServerLevel server)) return;
        boolean changed = false;

        if (firebox.burnTicks <= 0 && firebox.coal > 0) {
            firebox.coal--;
            firebox.burnTicks = ThermalUnits.COAL_BURN_TICKS;
            changed = true;
        }

        if (firebox.burnTicks > 0) {
            firebox.nextSide = ThermalTransfer.pushAdjacent(server, pos, firebox, firebox.nextSide);
            firebox.burnTicks--;
            changed = true;
        }

        boolean lit = firebox.burnTicks > 0;
        if (state.getValue(FireboxBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(FireboxBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) firebox.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Coal", coal);
        tag.putInt("BurnTicks", burnTicks);
        tag.putInt("NextSide", nextSide);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coal = Math.max(0, Math.min(64, tag.getInt("Coal")));
        burnTicks = Math.max(0, Math.min(ThermalUnits.COAL_BURN_TICKS, tag.getInt("BurnTicks")));
        nextSide = Math.floorMod(tag.getInt("NextSide"), 6);
    }
}
