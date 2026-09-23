package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FireboxBlockEntity extends BlockEntity implements HeatSource {
    private int coal;
    private int burnTicks;
    private int nextReceiver;

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.FIREBOX_ENTITY.get(), pos, state);
    }

    public boolean addCoal() {
        if (coal >= 64) return false;
        coal++;
        syncVisualState();
        setChanged();
        return true;
    }

    public boolean ignite() {
        if (burnTicks > 0 || coal <= 0) return false;
        coal--;
        burnTicks = ThermalUnits.COAL_BURN_TICKS;
        syncVisualState();
        setChanged();
        return true;
    }

    public int storedCoal() { return coal; }
    public boolean isBurning() { return burnTicks > 0; }

    @Override
    public int heatOutputPerTick() {
        return isBurning() ? ThermalUnits.FIREBOX_HEAT_PER_TICK : 0;
    }

    private int visualFuelLevel() {
        int effectiveFuel = coal + (burnTicks > 0 ? 1 : 0);
        if (effectiveFuel <= 0) return 0;
        if (effectiveFuel <= 2) return 1;
        if (effectiveFuel <= 8) return 2;
        return 3;
    }

    private void syncVisualState() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean lit = isBurning();
        int fuelLevel = visualFuelLevel();
        if (state.getValue(FireboxBlock.LIT) != lit
                || state.getValue(FireboxBlock.FUEL_LEVEL) != fuelLevel) {
            level.setBlock(worldPosition,
                state.setValue(FireboxBlock.LIT, lit)
                    .setValue(FireboxBlock.FUEL_LEVEL, fuelLevel),
                3);
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
        if (!(level instanceof ServerLevel server)) return;

        if (firebox.burnTicks > 0) {
            firebox.nextReceiver =
                ThermalTransfer.pushNetwork(server, pos, firebox, firebox.nextReceiver);

            long time = level.getGameTime() + pos.asLong();
            boolean open = state.getValue(FireboxBlock.OPEN);

            if (Math.floorMod(time, open ? 5 : 9) == 0) {
                server.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + (open ? 0.70 : 1.02),
                    pos.getZ() + 0.5,
                    open ? 2 : 1,
                    open ? 0.18 : 0.08, 0.04, open ? 0.18 : 0.08, 0.008);
            }

            if (open && Math.floorMod(time, 10) == 0) {
                server.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.48, pos.getZ() + 0.5,
                    2, 0.16, 0.05, 0.16, 0.004);
            }

            if (Math.floorMod(time, 65) == 0) {
                server.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, open ? 0.75F : 0.48F,
                    0.88F + server.random.nextFloat() * 0.18F);
            }

            firebox.burnTicks--;

            // Flint and steel starts the fire; a loaded firebox can then keep feeding itself.
            if (firebox.burnTicks == 0 && firebox.coal > 0) {
                firebox.coal--;
                firebox.burnTicks = ThermalUnits.COAL_BURN_TICKS;
            }

            firebox.syncVisualState();
            firebox.setChanged();
        } else {
            firebox.syncVisualState();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Coal", coal);
        tag.putInt("BurnTicks", burnTicks);
        tag.putInt("NextReceiver", nextReceiver);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coal = Math.max(0, Math.min(64, tag.getInt("Coal")));
        burnTicks = Math.max(0, Math.min(ThermalUnits.COAL_BURN_TICKS, tag.getInt("BurnTicks")));
        nextReceiver = tag.contains("NextReceiver")
            ? Math.max(0, tag.getInt("NextReceiver"))
            : Math.max(0, tag.getInt("NextSide"));
    }
}
